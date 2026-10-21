---
sidebar_position: 6
---

# jUPnP on Android

jUPnP Core provides a UPnP stack for Android applications.
Typically you'd write control point applications, as most Android systems today are small hand-held devices.
You can however also write UPnP server applications on Android, all features of jUPnP Core are supported.

Android platform level 15 (4.0) is required for jUPnP 2.x/3.x, use jUPnP 1.x to support older Android versions.

:::note

**jUPnP on the Android emulator**

At the time of writing, receiving UDP Multicast datagrams was not supported by the Android emulator.
The emulator will send (multicast) UDP datagrams, however.
You will be able to send a multicast UPnP search and receive UDP unicast responses, therefore discover existing running devices.
You will not discover devices which have been turned on after your search, and you will not receive any message when a device is switched off.
Other control points on your network will not discover your local Android device/services at all.
All of this can be confusing when testing your application, so unless you really understand what works and what doesn't, you might want to use a real device instead.

:::

## Configuring the application service

You could instantiate the jUPnP `UpnpService` as usual in your Android application's main activity.
On the other hand, if several activities in your application require access to the UPnP stack, a better design would utilize a background `android.app.Service`.
Any activity that wants to access the UPnP stack can then bind and unbind from this service as needed.

The interface of such a service component is available in jUPnP as `org.jupnp.android.AndroidUpnpService`:

```java
public interface AndroidUpnpService {

    /**
     * @return The actual main instance and interface of the UPnP service.
     */
    UpnpService get();

    /**
     * @return The configuration of the UPnP service.
     */
    UpnpServiceConfiguration getConfiguration();

    /**
     * @return The registry of the UPnP service.
     */
    Registry getRegistry();

    /**
     * @return The client API of the UPnP service.
     */
    ControlPoint getControlPoint();
}
```

An activity typically accesses the `Registry` of known UPnP devices or searches for and controls UPnP devices with the `ControlPoint`.

You have to configure the built-in implementation of this service component in your `AndroidManifest.xml`, along with various required permissions:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="org.jupnp.demo.android.browser">

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>

    <uses-sdk
            android:targetSdkVersion="22"
            android:minSdkVersion="15"/>

    <application
            android:icon="@drawable/appicon"
            android:label="@string/appName"
            android:allowBackup="false">

        <activity android:name=".BrowserActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <service android:name="org.jupnp.android.AndroidUpnpServiceImpl"/>

        <!-- Or a custom service configuration, also use this class in bindService()!
        <service android:name=".BrowserUpnpService"/>
        -->

    </application>

</manifest>
```

If a WiFi interface is present, jUPnP requires access to the interface.
jUPnP will automatically detect when network interfaces are switched on and off and handle this situation gracefully: Any client operation will result in a "no response from server" state when no network is available.
Your client code has to handle such a situation anyway.

jUPnP uses a custom configuration on Android, the `AndroidUpnpServiceConfiguration`, utilizing the Jetty transport and the `Recovering*` XML parsers and processors.
See the Javadoc of the class for more information.

*Jetty 9 libraries are required to use jUPnP on Android!*

For example, these dependencies are usually required in a Maven POM for jUPnP to work on Android:

```xml
<dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-server</artifactId>
    <version>${jetty.version}</version>
</dependency>
<dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-servlet</artifactId>
    <version>${jetty.version}</version>
</dependency>
<dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-client</artifactId>
    <version>${jetty.version}</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-jdk14</artifactId>
    <version>${slf4j.version}</version>
</dependency>
```

The service component starts and stops the UPnP system when the service component is created and destroyed.
This depends on how you access the service component from within your activities.

## Accessing the service from an activity

The lifecycle of service components in Android is well defined.
The first activity which binds to a service will start the service if it is not already running.
When no activity is bound to the service any more, the operating system will destroy the service.

Let's write a simple UPnP browsing activity.
It shows all devices on your network in a list and it has a menu option which triggers a search action.
The activity connects to the UPnP service and then listens to any device additions or removals in the `Registry`, so the displayed list of devices is kept up-to-date:

```java
public class BrowserActivity extends ListActivity {

    private ArrayAdapter<DeviceDisplay> listAdapter;

    private BrowseRegistryListener registryListener = new BrowseRegistryListener();

    private AndroidUpnpService upnpService;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            // Clear the list
            listAdapter.clear();

            // Get ready for future device advertisements
            upnpService.getRegistry().addListener(registryListener);

            // Now add all devices to the list we already know about
            for (Device device : upnpService.getRegistry().getDevices()) {
                registryListener.deviceAdded(device);
            }

            // Search asynchronously for all devices, they will respond soon
            upnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);

        // This will start the UPnP service if it wasn't already started
        getApplicationContext().bindService(
            new Intent(this, AndroidUpnpServiceImpl.class),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        // This will stop the UPnP service if nobody else is bound to it
        getApplicationContext().unbindService(serviceConnection);
    }
    // ...
}
```

We utilize the default layout provided by the Android runtime and the `ListActivity` superclass.
Note that this activity can be your applications main activity, or further up in the stack of a task.
The `listAdapter` is the glue between the device additions and removals on the jUPnP `Registry` and the list of items shown in the user interface.

:::note

**Debug logging on Android**

jUPnP uses SLF4J logging.
Use either DEBUG or TRACE logging level.

:::

The `upnpService` variable is `null` when no backend service is bound to this activity.
Binding and unbinding occurs in the `onCreate()` and `onDestroy()` callbacks, so the activity is bound to the service as long as it is alive.

Binding and unbinding the service is handled with the `ServiceConnection`: On connect, first a listener is added to the `Registry` of the UPnP service.
This listener will process additions and removals of devices as they are discovered on your network, and update the items shown in the user interface list.
The `BrowseRegistryListener` is removed when the activity is destroyed.

Then any already discovered devices are added manually to the user interface, passing them through the listener.
(There might be none if the UPnP service was just started and no device has so far announced its presence.)
Finally, you start asynchronous discovery by sending a search message to all UPnP devices, so they will announce themselves.
This search message is NOT required every time you connect to the service.
It is only necessary once, to populate the registry with all known devices when your (main) activity and application starts.

This is the `BrowseRegistryListener`, its only job is to update the displayed list items:

```java
protected class BrowseRegistryListener extends DefaultRegistryListener {

    /* Discovery performance optimization for very slow Android devices! */
    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
        deviceAdded(device);
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, 
            final Exception ex) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(
                    BrowserActivity.this,
                    "Discovery failed of '" + device.getDisplayString() + "': "
                        + (ex != null ? ex.toString() : "Couldn't retrieve device/service descriptors"),
                    Toast.LENGTH_LONG
                ).show();
            }
        });
        deviceRemoved(device);
    }
    /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        deviceAdded(device);
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        deviceRemoved(device);
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice device) {
        deviceAdded(device);
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice device) {
        deviceRemoved(device);
    }

    public void deviceAdded(final Device device) {
        runOnUiThread(new Runnable() {
            public void run() {
                DeviceDisplay d = new DeviceDisplay(device);
                int position = listAdapter.getPosition(d);
                if (position >= 0) {
                    // Device already in the list, re-set new value at same position
                    listAdapter.remove(d);
                    listAdapter.insert(d, position);
                } else {
                    listAdapter.add(d);
                }
            }
        });
    }

    public void deviceRemoved(final Device device) {
        runOnUiThread(new Runnable() {
            public void run() {
                listAdapter.remove(new DeviceDisplay(device));
            }
        });
    }
}
```

For performance reasons, when a new device has been discovered, we don't wait until a fully hydrated (all services retrieved and validated) device metadata model is available.
We react as quickly as possible and don't wait until the `remoteDeviceAdded()` method will be called.
We display any device even while discovery is still running.
You'd usually not care about this on a desktop computer, however, Android handheld devices are slow and UPnP uses several bloated XML descriptors to exchange metadata about devices and services.
Sometimes it can take several seconds before a device and its services are fully available.
The `remoteDeviceDiscoveryStarted()` and `remoteDeviceDiscoveryFailed()` methods are called as soon as possible in the discovery process.
On modern fast Android handsets, and unless you have to deal with dozens of UPnP devices on a LAN, you don't need this optimization.

By the way, devices are equal (`a.equals(b)`) if they have the same UDN, they might not be identical (`a==b`).

The `Registry` will call the listener methods in a separate thread.
You have to update the displayed list data in the thread of the user interface.

The following methods on the activity add a menu with a search action, so a user can refresh the list manually:

```java
public class BrowserActivity extends ListActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.searchLAN).setIcon(android.R.drawable.ic_menu_search);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                if (upnpService == null) {
                    break;
                }
                Toast.makeText(this, R.string.searchingLAN, Toast.LENGTH_SHORT).show();
                upnpService.getRegistry().removeAllRemoteDevices();
                upnpService.getControlPoint().search();
                break;
        }
        return false;
    }
    // ...
}
```

Finally, the `DeviceDisplay` class is a very simple JavaBean that only provides a `toString()` method for rendering the list.
You can display any information about UPnP devices by changing this method:

```java
protected class DeviceDisplay {

    Device device;

    public DeviceDisplay(Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceDisplay that = (DeviceDisplay) o;
        return device.equals(that.device);
    }

    @Override
    public int hashCode() {
        return device.hashCode();
    }

    @Override
    public String toString() {
        String name =
            getDevice().getDetails() != null && getDevice().getDetails().getFriendlyName() != null
                ? getDevice().getDetails().getFriendlyName()
                : getDevice().getDisplayString();
        // Display a little star while the device is being loaded (see performance optimization earlier)
        return device.isFullyHydrated() ? name : name + " *";
    }
}
```

We have to override the equality operations as well, so we can remove and add devices from the list manually with the `DeviceDisplay` instance as a convenient handle.

So far we have implemented a UPnP control point, next we create a UPnP device with services.

## Creating a UPnP device

The following activity provides a UPnP service, the well known *SwitchPower:1* with a *BinaryLight:1* device:

```java
public class LightActivity extends Activity implements PropertyChangeListener {

    private AndroidUpnpService upnpService;

    private UDN udn = new UDN(UUID.randomUUID()); // TODO: Not stable!

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            LocalService<SwitchPower> switchPowerService = getSwitchPowerService();

            // Register the device when this activity binds to the service for the first time
            if (switchPowerService == null) {
                try {
                    LocalDevice binaryLightDevice = createDevice();

                    Toast.makeText(LightActivity.this, R.string.registeringDevice,
                        Toast.LENGTH_SHORT).show();
                    upnpService.getRegistry().addDevice(binaryLightDevice);

                    switchPowerService = getSwitchPowerService();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, "Creating BinaryLight device failed", ex);
                    Toast.makeText(LightActivity.this, R.string.createDeviceFailed,
                        Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Obtain the state of the power switch and update the UI
            setLightbulb(switchPowerService.getManager().getImplementation().getStatus());

            // Start monitoring the power switch
            switchPowerService.getManager().getImplementation().getPropertyChangeSupport()
                    .addPropertyChangeListener(LightActivity.this);

        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.light);

        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop monitoring the power switch
        LocalService<SwitchPower> switchPowerService = getSwitchPowerService();
        if (switchPowerService != null) {
            switchPowerService.getManager().getImplementation().getPropertyChangeSupport()
                    .removePropertyChangeListener(this);
        }

        getApplicationContext().unbindService(serviceConnection);
    }

    protected LocalService<SwitchPower> getSwitchPowerService() {
        if (upnpService == null) {
            return null;
        }

        LocalDevice binaryLightDevice;
        if ((binaryLightDevice = upnpService.getRegistry().getLocalDevice(udn, true)) == null) {
            return null;
        }

        return (LocalService<SwitchPower>) binaryLightDevice.findService(
            new UDAServiceType("SwitchPower", 1));
    }
    // ...
}
```

When the UPnP service is bound, for the first time, we check if the device has already been created by querying the `Registry`.
If not, we create the device and add it to the `Registry`.

:::note

**Generating a stable UDN on Android**

The UDN of a UPnP device is supposed to be stable: It should not change between restarts of the device.
Unfortunately, the jUPnP helper method `UDN.uniqueSystemIdentifier()` doesn't work on Android, see its Javadoc.
Generating a new UUID every time your activity starts might be OK for testing, in production you should generate a UUID once when your application starts for the first time and store the UUID value in your application's preferences.

:::

The activity is also a JavaBean `PropertyChangeListener`, registered with `SwitchPower` service.
Note that this is JavaBean eventing, it has nothing to do with UPnP GENA eventing!
We monitor the state of the service and switch the UI accordingly, turning the light on and off:

```java
public class LightActivity extends Activity implements PropertyChangeListener {

    public void propertyChange(PropertyChangeEvent event) {
        // This is regular JavaBean eventing, not UPnP eventing!
        if (event.getPropertyName().equals("status")) {
            log.info("Turning light: " + event.getNewValue());
            setLightbulb((Boolean) event.getNewValue());
        }
    }

    protected void setLightbulb(final boolean on) {
        runOnUiThread(new Runnable() {
            public void run() {
                ImageView imageView = (ImageView) findViewById(R.id.light_imageview);
                imageView.setImageResource(on ? R.drawable.light_on : R.drawable.light_off);
                // You can NOT externalize this color into /res/values/colors.xml. Go on, try it!
                imageView.setBackgroundColor(on ? Color.parseColor("#9EC942") : Color.WHITE);
            }
        });
    }
    // ...
}
```

The `createDevice()` method simply instantiates a new `LocalDevice`:

```java
public class LightActivity extends Activity implements PropertyChangeListener {

    protected LocalDevice createDevice() throws ValidationException, LocalServiceBindingException {
        DeviceType type = new UDADeviceType("BinaryLight", 1);

        DeviceDetails details = new DeviceDetails(
                        "Friendly Binary Light",
                        new ManufacturerDetails("ACME"),
                        new ModelDetails("AndroidLight", "A light with on/off switch.", "v1")
                );

        LocalService service = new AnnotationLocalServiceBinder().read(SwitchPower.class);

        service.setManager(new DefaultServiceManager<>(service, SwitchPower.class));

        return new LocalDevice(new DeviceIdentity(udn), type, details, createDefaultDeviceIcon(), service);
    }
    // ...
}
```

For the `SwitchPower` class, again note the dual eventing for JavaBeans and UPnP:

```java
@UpnpService(
    serviceId = @UpnpServiceId("SwitchPower"),
    serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
)
public class SwitchPower {

    private final PropertyChangeSupport propertyChangeSupport;

    public SwitchPower() {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    @UpnpStateVariable(defaultValue = "0", sendEvents = false)
    private boolean target = false;

    @UpnpStateVariable(defaultValue = "0")
    private boolean status = false;

    @UpnpAction
    public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue) {
        boolean targetOldValue = target;
        target = newTargetValue;
        boolean statusOldValue = status;
        status = newTargetValue;

        // These have no effect on the UPnP monitoring but it's JavaBean compliant
        getPropertyChangeSupport().firePropertyChange("target", targetOldValue, target);
        getPropertyChangeSupport().firePropertyChange("status", statusOldValue, status);

        // This will send a UPnP event, it's the name of a state variable that sends events
        getPropertyChangeSupport().firePropertyChange("Status", statusOldValue, status);
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
    public boolean getTarget() {
        return target;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
    public boolean getStatus() {
        return status;
    }
}
```

## Optimizing service behavior

The UPnP service consumes memory and CPU time while it is running.
Although this is typically not an issue on a regular machine, this might be a problem on an Android handset.
You can preserve memory and handset battery power if you disable certain features of the jUPnP UPnP service, or if you even pause and resume it when appropriate.

Furthermore, some Android handsets do not support multicast networking (HTC phones, for example), so you have to configure jUPnP accordingly on such a device and disable most of the UPnP discovery protocol.

### Tuning registry maintenance

There are several things going on in the background while the service is running.
First, there is the registry of the service and its maintenance thread.
If you are writing a control point, this background registry maintainer is going to renew your outbound GENA subscriptions with remote services periodically.
It will also expire and remove any discovered remote devices when the drop off the network without saying goodbye.
If you are providing a local service, your device announcements will be refreshed by the registry maintainer and inbound GENA subscriptions will be removed if they haven't been renewed in time.
Effectively, the registry maintainer prevents stale state on the UPnP network, so all participants have an up-to-date view of all other participants, and so on.

By default the registry maintainer will run every second and check if there is something to do (most of the time there is nothing to do, of course).
The default Android configuration however has a default sleep interval of three seconds, so it is already consuming less background CPU time - while your application might be exposed to somewhat outdated information.
You can further tune this setting by overriding the `getRegistryMaintenanceIntervalMillis()` in the `UpnpServiceConfiguration`.
On Android, you have to subclass the service implementation to provide a new configuration:

```java
public class BrowserUpnpService extends AndroidUpnpServiceImpl {
    @Override
    protected UpnpServiceConfiguration createConfiguration() {
        return new AndroidUpnpServiceConfiguration() {
            @Override
            public int getRegistryMaintenanceIntervalMillis() {
                return 7000;
            }
        };
    }
}
```

Don't forget to now configure `BrowserUpnpService` in your `AndroidManifest.xml` instead of the original implementation.
You also have to use this class when binding to the service in your activities instead of `AndroidUpnpServiceImpl`.

### Pausing and resuming registry maintenance

Another more effective but also more complex optimization is pausing and resuming the registry whenever your activities no longer need the UPnP service.
This is typically the case when an activity is no longer in the foreground (paused) or even no longer visible (stopped).
By default any activity state change has no impact on the state of the UPnP service unless you bind and unbind from and to the service in your activities lifecycle callbacks.

In addition to binding and unbinding from the service you can also pause its registry by calling `Registry#pause()` when your activity's `onPause()` or `onStop()` method is called.
You can then resume the background service maintenance (thread) with `Registry#resume()`, or check the status with `Registry#isPaused()`.

Please read the Javadoc of these methods for more details and what consequences pausing registry maintenance has on devices, services, and GENA subscriptions.
Depending on what your application does, this rather minor optimization might not be worth dealing with these effects.
On the other hand, your application should already be able to handle failed GENA subscription renewals, or disappearing remote devices!

### Configuring discovery

The most effective optimization is selective discovery of UPnP devices.
Although the UPnP service's network transport layer will keep running (threads are waiting and sockets are bound) in the background, this feature allows you to drop discovery messages selectively and quickly.

For example, if you are writing a control point, you can drop any received discovery message if it doesn't advertise the service you want to control - you are not interested in any other device.
On the other hand if you only *provide* devices and services, all discovery messages (except search messages for your services) can probably be dropped, you are not interested in any remote devices and their services at all.

Discovery messages are selected and potentially dropped by jUPnP as soon as the UDP datagram content is available, so no further parsing and processing is needed and CPU time/memory consumption is significantly reduced while you keep the UPnP service running even in the background on an Android handset.

To configure which services are supported by your control point application, override the configuration and provide an array of `ServiceType` instances:

```java
public class BrowserUpnpService extends AndroidUpnpServiceImpl {
    @Override
    protected UpnpServiceConfiguration createConfiguration() {
        return new AndroidUpnpServiceConfiguration() {
            @Override
            public ServiceType[] getExclusiveServiceTypes() {
                return new ServiceType[]{
                    new UDAServiceType("SwitchPower")
                };
            }
        };
    }
}
```

This configuration will ignore any advertisement from any device that doesn't also advertise a *schemas-upnp-org:SwitchPower:1* service.
This is what our control point can handle, so we don't need anything else.
If instead you'd return an empty array (the default behavior), all services and devices will be discovered and no advertisements will be dropped.

If you are not writing a control point but a server application, you can return `null` in the `getExclusiveServiceTypes()` method.
This will disable discovery completely, now all device and service advertisements are dropped as soon as they are received.
