package example.registry;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;
import org.testng.annotations.Test;

public class RegistryAsyncTest {

    private static final int DEVICE_COUNT = 20;
    private static final String[] UDNS = new String[DEVICE_COUNT];

    @Test
    public void addMultipleLocalDevices() throws ValidationException, InterruptedException {

        for (int i = 0; i < UDNS.length; i++) {
            UDNS[i] = "my-device-" + i;
        }

        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        final Registry registry = upnpService.getRegistry();

        new Thread(new RegistryClient(registry, 0)).start();
        new Thread(new RegistryClient(registry, 1)).start();
        new Thread(new RegistryClient(registry, 2)).start();
        new Thread(new RegistryClient(registry, 3)).start();

        Thread.sleep(5000);

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

        assertNull(deadlockedThreads);

        assertEquals(registry.getLocalDevices().size() + registry.getRemoteDevices().size(), DEVICE_COUNT);

        for (LocalDevice localDevice : registry.getLocalDevices()) {
            RemoteDevice remoteDevice = registry.getRemoteDevice(localDevice.getIdentity().getUdn(), true);
            assertNull(remoteDevice);
        }
    }

    private static LocalDevice createLocalDevice(String udn) throws ValidationException {
        DeviceIdentity identity = new DeviceIdentity(new UDN(udn));
        return new LocalDevice(identity);
    }

    private static RemoteDevice createRemoteDevice(String udn) throws ValidationException {
        RemoteDeviceIdentity identity = new RemoteDeviceIdentity(new UDN(udn), 16, null, null, null);
        return new RemoteDevice(identity);
    }

    private static class RegistryClient implements Runnable {

        private Registry registry;
        private int threadNumber;

        public RegistryClient(Registry registry, int threadNumber) {
            this.registry = registry;
            this.threadNumber = threadNumber;
        }

        @Override
        public void run() {
            try {
                /*
                 * Each thread has a different starting point and tries to add each device multiple times as both local and
                 * remote device.
                 */
                for (int i = threadNumber * 7; i < 100; i++) {
                    if (i + threadNumber % 2 == 0) {
                        LocalDevice localDevice = createLocalDevice(UDNS[i % DEVICE_COUNT]);
                        registry.addDevice(localDevice);
                    } else {
                        RemoteDevice remoteDevice = createRemoteDevice(UDNS[i % DEVICE_COUNT]);
                        registry.addDevice(remoteDevice);
                    }

                }
            } catch (ValidationException e) {
            }
        }
    }
}
