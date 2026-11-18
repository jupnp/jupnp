---
sidebar_position: 3
---

# A first UPnP service and control point

The most basic UPnP service imaginable is the *binary light*.
This device has one service, the power switch, turning the light on and off.
In fact, the *SwitchPower:1* service and the *BinaryLight:1* device are standardized templates you can download [here](https://openconnectivity.org/index.php?s=binarylight+switchpower).

In the following sections we'll implement this UPnP service and device with the jUPnP Core library as a simple Java console application.

## The SwitchPower service implementation

This is the source of the *SwitchPower:1* service - note that although there are many annotations in the source, no runtime dependency on jUPnP exists:

```java
package example.binarylight;

import org.jupnp.binding.annotations.*;

@UpnpService(serviceId = @UpnpServiceId("SwitchPower"), serviceType = @UpnpServiceType(value = "SwitchPower", version = 1))
public class SwitchPower {

    @UpnpStateVariable(defaultValue = "0", sendEvents = false)
    private boolean target = false;

    @UpnpStateVariable(defaultValue = "0")
    private boolean status = false;

    @UpnpAction
    public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue) {
        target = newTargetValue;
        status = newTargetValue;
        System.out.println("Switch is: " + status);
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

To compile this class the jUPnP Core library has to be available on your classpath.
However, once compiled this class can be instantiated and executed in any environment, there are no dependencies on any framework or library code.

The annotations are used by jUPnP to read the metadata that describes your service, what UPnP state variables it has, how they are accessed, and what methods should be exposed as UPnP actions.
You can also provide jUPnP metadata in an XML file or programmatically through Java code - both options are discussed later in this manual.
Source code annotations are usually the best choice.

You might have expected something even simpler: After all, a *binary light* only needs a single boolean state, it is either on or off.
The designers of this service also considered that there might be a difference between switching the light on, and actually seeing the result of that action.
Imagine what happens if the light bulb is broken: The target state of the light is set to *true* but the status is still *false*, because the *SetTarget* action could not make the switch.
Obviously this won't be a problem with this simple demonstration because it only prints the status to standard console output.

## Binding a UPnP device

Devices (and embedded devices) are created programmatically in jUPnP, with plain Java code that instantiates an immutable graph of objects.
The following method creates such a device graph and binds the service from the previous section to the root device:

```java
LocalDevice createDevice() throws ValidationException, LocalServiceBindingException, IOException {
    DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("Demo Binary Light"));
    DeviceType type = new UDADeviceType("BinaryLight", 1);
    DeviceDetails details = new DeviceDetails("Friendly Binary Light", new ManufacturerDetails("ACME"),
            new ModelDetails("BinLight2000", "A demo light with on/off switch.", "v1"));
    Icon icon = new Icon("image/png", 48, 48, 8, getClass().getResource("icon.png"));

    LocalService<SwitchPower> switchPowerService = new AnnotationLocalServiceBinder().read(SwitchPower.class);
    switchPowerService.setManager(new DefaultServiceManager<>(switchPowerService, SwitchPower.class));

    return new LocalDevice(identity, type, details, icon, switchPowerService);

    /*
     * Several services can be bound to the same device:
     * return new LocalDevice(identity, type, details, icon, new LocalService[] {switchPowerService, myOtherService});
     */
}
```

Let's step through this code.
As you can see, all arguments that make up the device's metadata have to be provided through constructors, because the metadata classes are immutable and hence thread-safe.

<dl>
    <dt>DeviceIdentity</dt>
    <dd>
        Every device, no matter if it is a root device or an embedded device of a root device, requires a unique device name (UDN).
        This UDN should be stable, that is, it should not change when the device is restarted.
        When you physically unplug a UPnP appliance from the network (or when you simply turn it off or put it into standby mode), and when you make it available later on, it should expose the same UDN so that clients know they are dealing with the same device.
        The `UDN.uniqueSystemIdentifier()` method provides exactly that: A unique identifier that is the same every time this method is called on the same computer system.
        It hashes the network cards hardware address and a few other elements to guarantee uniqueness and stability.
    </dd>
    <dt>DeviceType</dt>
    <dd>
        The type of a device also includes its version, a plain integer.
        In this case the *BinaryLight:1* is a standardized device template which adheres to the UDA (UPnP Device Architecture) specification.
    </dd>
    <dt>DeviceDetails</dt>
    <dd>
        This detailed information about the device's "friendly name", as well as model and manufacturer information is optional.
        You should at least provide a friendly name value, this is what UPnP applications will display primarily.
    </dd>
    <dt>Icon</dt>
    <dd>
        Every device can have a bunch of icons associated with it which similar to the friendly name are shown to users when appropriate.
        You do not have to provide any icons if you don't want to, use a constructor of `LocalDevice` without an icon parameter.
    </dd>
    <dt>Service</dt>
    <dd>
        Finally, the most important part of the device are its services.
        Each `Service` instance encapsulates the metadata for a particular service, what actions and state variables it has, and how it can be invoked.
        Here we use the jUPnP annotation binder to instantiate a `Service`, reading the annotation metadata of the `SwitchPower` class.
    </dd>
</dl>

Because a `Service` instance is only metadata that describes the service, you have to set a `ServiceManager` to do some actual work.
This is the link between the metadata and your implementation of a service, where the rubber meets the road.
The `DefaultServiceManager` will instantiate the given `SwitchPower` class when an action which operates on the service has to be executed (this happens lazily, as late as possible).
The manager will hold on to the instance and always re-use it as long as the service is registered with the UPnP stack.
In other words, the service manager is the factory that instantiates your actual implementation of a UPnP service.

Also note that `LocalDevice` is the interface that represents a UPnP device which is "local" to the running UPnP stack on the host.
Any device that has been discovered through the network will be a `RemoteDevice` with `RemoteService`'s, you typically do not instantiate these directly.

A `ValidationException` will be thrown when the device graph you instantiated was invaild, you can call `getErrors()` on the exception to find out which property value of which class failed an integrity rule.
The local service annotation binder will provide a `LocalServiceBindingException` if something is wrong with your annotation metadata on your service implementation class.
An `IOException` can only by thrown by this particular `Icon` constructor, when it reads the resource file.

<xi:include href="server.xhtml"/>

## Creating a control point

The client application has the same basic scaffolding as the server, it also uses a shared single instance of `UpnpService`:

```java
package example.binarylight;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.controlpoint.*;
import org.jupnp.model.action.*;
import org.jupnp.model.message.*;
import org.jupnp.model.message.header.*;
import org.jupnp.model.meta.*;
import org.jupnp.model.types.*;
import org.jupnp.registry.*;

public class BinaryLightClient implements Runnable {

    public static void main(String[] args) {
        // Start a user thread that runs the UPnP stack
        Thread clientThread = new Thread(new BinaryLightClient());
        clientThread.setDaemon(false);
        clientThread.start();
    }

    @Override
    public void run() {
        try {
            UpnpService upnpService = new UpnpServiceImpl();

            // Add a listener for device registration events
            upnpService.getRegistry().addListener(createRegistryListener(upnpService));

            // Broadcast a search message for all devices
            upnpService.getControlPoint().search(new STAllHeader());
        } catch (Exception e) {
            System.err.println("Exception occurred: " + e);
            System.exit(1);
        }
    }
}
```

Typically a control point sleeps until a device with a specific type of service becomes available on the network.
The `RegistryListener` is called by jUPnP when a remote device has been discovered - or when it announced itself automatically.
Because you usually do not want to wait for the periodic announcements of devices, a control point can also execute a search for all devices (or devices with certain service types or UDN), which will trigger an immediate discovery announcement from those devices that match the search query.

You can already see the `ControlPoint` API here with its `search(...)` method, this is one of the main interfaces you interact with when writing a UPnP client with jUPnP.

If you compare this code with the server code from the previous section you can see that we are not shutting down the `UpnpService` when the application quits.
This is not an issue here, because this application does not have any local devices or service event listeners (not the same as registry listeners) bound and registered.
Hence, we do not have to announce their departure on application shutdown and can keep the code simple for the sake of the example.

Let's focus on the registry listener implementation and what happens when a UPnP device has been discovered on the network.

## Executing an action

The control point we are creating here is only interested in services that implement *SwitchPower*.
According to its template definition this service has the `SwitchPower` service identifier, so when a device has been discovered we can check if it offers that service:

```java
RegistryListener createRegistryListener(final UpnpService upnpService) {
    return new DefaultRegistryListener() {
        final ServiceId serviceId = new UDAServiceId("SwitchPower");

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            Service switchPower;
            if ((switchPower = device.findService(serviceId)) != null) {

                System.out.println("Service discovered: " + switchPower);
                executeAction(upnpService, switchPower);
            }
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            Service switchPower;
            if ((switchPower = device.findService(serviceId)) != null) {
                System.out.println("Service disappeared: " + switchPower);
            }
        }
    };
}
```

If a service becomes available we immediately execute an action on that service.
When a `SwitchPower` device disappears from the network a log message is printed.
Remember that this is a very trivial control point, it executes a single a fire-and-forget operation when a service becomes available:

```java
void executeAction(UpnpService upnpService, Service switchPowerService) {
    ActionInvocation setTargetInvocation = new SetTargetActionInvocation(switchPowerService);

    // Executes asynchronous in the background
    upnpService.getControlPoint().execute(new ActionCallback(setTargetInvocation) {
        @Override
        public void success(ActionInvocation invocation) {
            assert invocation.getOutput().length == 0;
            System.out.println("Successfully called action!");
        }

        @Override
        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
            System.err.println(defaultMsg);
        }
    });
}

static class SetTargetActionInvocation extends ActionInvocation {
    SetTargetActionInvocation(Service service) {
        super(service.getAction("SetTarget"));
        try {
            // Throws InvalidValueException if the value is of wrong type
            setInput("NewTargetValue", true);
        } catch (InvalidValueException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
```

The `Action` (metadata) and the `ActionInvocation` (actual call data) APIs allow very fine-grained control of how an invocation is prepared, how input values are set, how the action is executed, and how the output and outcome is handled.
UPnP is inherently asynchronous, so just like the registry listener, executing an action is exposed to you as a callback-style API.

It is recommended that you encapsulate specific action invocations within a subclass of `ActionInvocation`, which gives you an opportunity to further abstract the input and output values of an invocation.
Note however that an instance of `ActionInvocation` is not thread-safe and should not be executed in parallel by two threads.

The `ActionCallback` has two main methods you have to implement, one is called when the execution was successful, the other when it failed.
There are many reasons why an action execution might fail, read the API documentation for all possible combinations or just print the generated user-friendly default error message.

## Starting the application

Compile the binary light demo application:

```shell
$ javac -cp /path/to/jupnp-core.jar \
        -d classes/ \
        src/example/binarylight/BinaryLightServer.java \
        src/example/binarylight/BinaryLightClient.java \
        src/example/binarylight/SwitchPower.java
```

Don't forget to copy your `icon.png` file into the `classes` output directory as well, into the right package from which it is loaded as a reasource (the `example.binarylight` package if you followed the previous sections verbatim).

You can start the server or client first, which one doesn't matter as they will discover each other automatically:
        
```shell
$ java -cp /path/to/jupnp-core.jar:classes/ \
        example.binaryLight.BinaryLightServer
```

```shell
$ java -cp /path/to/jupnp-core.jar:classes/ \
        example.binaryLight.BinaryLightClient
```

You should see discovery and action execution messages on each console.
You can stop and restart the applications individually (press CTRL+C on the console).

## Debugging and logging

Although the binary light is a very simple example, you might run into problems.
jUPnP Core helps you resolve most problems with extensive logging.
Internally, jUPnP Core uses now SLF4J API logging instead of `java.util.logging`.

SLF4J can be configured according your preferences.

Next you want to configure logging levels for different logging categories.
jUPnP Core will output some INFO level messages on startup and shutdown, but is otherwise silent during runtime unless a problem occurs - it will then log messages at WARNING or ERROR level.

For debugging, usually more detailed logging levels for various log categories are required.
The logging categories in jUPnP Core are package names, e.g the root logger is available under the name `org.jupnp`.
The following tables show typically used categories and the recommended level for debugging:

| Network/Transport                                                                                                                                      |                                            |
|--------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| `org.jupnp.transport.spi.DatagramIO` (TRACE) <br/> `org.jupnp.transport.spi.MulticastReceiver` (TRACE)                                                 | UDP communication                          |
| `org.jupnp.transport.spi.DatagramProcessor` <br/> (TRACE)                                                                                              | UDP datagram processing and content        |
| `org.jupnp.transport.spi.UpnpStream` (TRACE) <br/> `org.jupnp.transport.spi.StreamServer` (TRACE) <br/> `org.jupnp.transport.spi.StreamClient` (TRACE) | TCP communication                          |
| `org.jupnp.transport.spi.SOAPActionProcessor` (TRACE)                                                                                                  | SOAP action message processing and content |
| `org.jupnp.transport.spi.GENAEventProcessor` (TRACE)                                                                                                   | GENA event message processing and content  |
| `org.jupnp.transport.impl.HttpHeaderConverter` (TRACE)                                                                                                 | HTTP header processing                     |

| UPnP Protocol                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |                                       |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------|
| `org.jupnp.protocol.ProtocolFactory` (TRACE) <br/> `org.jupnp.protocol.async` (TRACE)                                                                                                                                                                                                                                                                                                                                                                                                            | Discovery (Notification &amp; Search) |
| `org.jupnp.protocol.ProtocolFactory` (TRACE) <br/> `org.jupnp.protocol.RetrieveRemoteDescriptors` (TRACE) <br/> `org.jupnp.protocol.sync.ReceivingRetrieval` (TRACE) <br/> `org.jupnp.binding.xml.DeviceDescriptorBinder` (TRACE) <br/> `org.jupnp.binding.xml.ServiceDescriptorBinder` (TRACE)                                                                                                                                                                                                  | Description                           |
| `org.jupnp.protocol.ProtocolFactory` (TRACE) <br/> `org.jupnp.protocol.sync.ReceivingAction` (TRACE) <br/> `org.jupnp.protocol.sync.SendingAction` (TRACE)                                                                                                                                                                                                                                                                                                                                       | Control                               |
| `org.jupnp.model.gena` (TRACE) <br/> `org.jupnp.protocol.ProtocolFactory` (TRACE) <br/> `org.jupnp.protocol.sync.ReceivingEvent` (TRACE) <br/> `org.jupnp.protocol.sync.ReceivingSubscribe` (TRACE) <br/> `org.jupnp.protocol.sync.ReceivingUnsubscribe` (TRACE) <br/> `org.jupnp.protocol.sync.SendingEvent` (TRACE) <br/> `org.jupnp.protocol.sync.SendingSubscribe` (TRACE) <br/> `org.jupnp.protocol.sync.SendingUnsubscribe` (TRACE) <br/> `org.jupnp.protocol.sync.SendingRenewal` (TRACE) | GENA                                  |

| Core                                                                                                                                                                                                                               |                                        |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------|
| `org.jupnp.transport.Router` (TRACE) <br/>                                                                                                                                                                                         |              Message Router            |
| `org.jupnp.registry.Registry` (TRACE) <br/> `org.jupnp.registry.LocalItems` (TRACE) <br/> `org.jupnp.registry.RemoteItems` (TRACE) <br/>                                                                                           | Registry                               |
| `org.jupnp.binding.annotations` (TRACE) <br/> `org.jupnp.model.meta.LocalService` (TRACE) <br/> `org.jupnp.model.action` (TRACE) <br/> `org.jupnp.model.state` (TRACE) <br/> `org.jupnp.model.DefaultServiceManager` (TRACE) <br/> | Local service binding &amp; invocation |
| `org.jupnp.controlpoint` (TRACE) <br/>                                                                                                                                                                                             | Control Point interaction              |

One way to configure SLF4J is to use logback with a config file.
For example, create the following file as `logback.xml`:

```xml
<configuration>

	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<!-- encoders are assigned the type ch.qos.logback.classic.encoder.PatternLayoutEncoder 
			by default -->
		<encoder>
			<pattern>%d{HH:mm:ss.SSS} [%-20thread] %-5level %-70(%logger{36}.%M:%line) - %msg%n
			</pattern>
		</encoder>
	</appender>

	<!--  Extra settings for various categories -->
	<logger name="org.jupnp.protocol" level="TRACE" />
	<logger name="org.jupnp.registry.Registry" level="TRACE" />
	<logger name="org.jupnp.registry.LocalItems" level="TRACE" />
	<logger name="org.jupnp.registry.RemoteItems" level="TRACE" />

	<!--  Extra settings to see on-the-wire traffic -->
	<logger name="org.jupnp.transport.spi.DatagramProcessor" level="TRACE" />
	<logger name="org.jupnp.transport.spi.SOAPActionProcessor" level="TRACE" />

	<!--  default root level -->
	<root level="INFO">
		<appender-ref ref="STDOUT" />
	</root>

</configuration>
```

You can now start your application with a system property that names your logging configuration:

```shell
$ java -cp /path/to/jupnp-core.jar:classes/ \
        -Dlogback.configurationFile=/path/to/logback.xml \
        example.binaryLight.BinaryLightServer
```

You should see the desired log messages printed on `System.out`.
