---
sidebar_position: 7
---

# Advanced options

## Custom client/server information

Sometimes your service has to implement different procedures depending on the client who makes the action request, or you want to send a request with some identifying information about your client.

### Adding extra request headers

By default, jUPnP will add all necessary headers to all outbound request messages.
For HTTP-based messages such as descriptor retrieval, action invocation, and GENA messages, the `User-Agent` HTTP header will be set to a default value, obtained from your `StreamClientConfiguration`.

You can override this behavior for descriptor retrieval and GENA subscription messages with a custom configuration.
For example, this configuration will send extra HTTP headers when device and service descriptors have to be retrieved for a particular UDN:

```java
UpnpService upnpService = new UpnpServiceImpl(
    new DefaultUpnpServiceConfiguration() {
        @Override
        public UpnpHeaders getDescriptorRetrievalHeaders(RemoteDeviceIdentity identity) {
            if (identity.getUdn().getIdentifierString().equals("aa-bb-cc-dd-ee-ff")) {
                UpnpHeaders headers = new UpnpHeaders();
                headers.add(UpnpHeader.Type.USER_AGENT.getHttpName(), "MyCustom/Agent");
                headers.add("X-Custom-Header", "foo");
                return headers;
            }
            return null;
        }
    }
);
```

For GENA subscription, renewal, and unsubscribe messages, you can set extra headers depending on the service you are subscribing to:

```java
UpnpService upnpService = new UpnpServiceImpl(
    new DefaultUpnpServiceConfiguration() {
        @Override
        public UpnpHeaders getEventSubscriptionHeaders(RemoteService service) {
            if (service.getServiceType().implementsVersion(new UDAServiceType("Foo", 1))) {
                UpnpHeaders headers = new UpnpHeaders();
                headers.add("X-Custom-Header", "bar");
                return headers;
            }
            return null;
        }
    }
);
```

For action invocations to remote services, you can set custom headers when constructing the `ActionInvocation`:

```java
UpnpHeaders extraHeaders = new UpnpHeaders();
extraHeaders.add(UpnpHeader.Type.USER_AGENT.getHttpName(), "MyCustom/Agent");
extraHeaders.add("X-Custom-Header", "foo");

ActionInvocation actionInvocation = new ActionInvocation(action, new ClientInfo(extraHeaders));
```

Any of these settings only affect outbound request messages! Any outbound response to a remote request will have only headers required by the UPnP protocols.
See the next section on how to customize response headers for remote action requests.

Very rarely you have to customize SSDP (MSEARCH and its response) messages.
First, subclass the default `ProtocolFactoryImpl` and override the instantiation of the protocols as necessary.
For example, override `createSendingSearch()` and return your own instance of the `SendingSearch` protocol.
Next, override `prepareOutgoingSearchRequest(OutgoingSearchRequest)` of the `SendingSearch` protocol and modify the message.
The same procedure can be applied to customize search responses with the `ReceivingSearch` protocol.

### Accessing remote client information

Theoretically, your service implementation should work with any client, as UPnP is supposed to provide a compatibility layer.
In practice, this never works as no UPnP client and server is fully compatible with the specifications (except jUPnP, of course).

If your action method has a last (or only parameter) of type `RemoteClientInfo`, jUPnP will provide details about the control point calling your service:

```java
@UpnpAction
public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue,
        RemoteClientInfo clientInfo) {
    power = newTargetValue;
    System.out.println("Switch is: " + power);

    if (clientInfo != null) {
        System.out.println("Client's address is: " + clientInfo.getRemoteAddress());
        System.out.println("Received message on: " + clientInfo.getLocalAddress());
        System.out.println("Client's user agent is: " + clientInfo.getRequestUserAgent());
        System.out.println(
                "Client's custom header is: " + clientInfo.getRequestHeaders().getFirstHeader("X-MY-HEADER"));

        // Return some extra headers in the response
        clientInfo.getExtraResponseHeaders().add("X-MY-HEADER", "foobar");
    }
}
```

The `RemoteClientInfo` argument will only be available when this action method is processing a remote client call, an `ActionInvocation` executed by the local UPnP stack on a local service does not have remote client information and the argument will be `null`.

A client's remote and local address might be `null` if the jUPnP transport layer was not able to obtain the connection's address.

You can set extra response headers on the `RemoteClientInfo`, which will be returned to the client with the response of your UPnP action.
There is also a `setResponseUserAgent()` method for your convenience.

The `RemoteClientInfo` is also useful if you have to deal with potentially long-running actions.

## Long-running actions

An action of a service might take a long time to complete and consume resources.
For example, if your service has to process significant amounts of data, you might want to stop processing when the client calling your action is actually no longer connected.
On the client side, you might want to give your users the option to interrupt and abort action requests if the service takes too long to respond.
These are two distinct issues, and we'll first look at it from the client's perspective.

### Cancelling an action invocation

You call actions of services with the `ControlPoint#execute(myCallback)` method.
So far you probably haven't considered the optional return value of this method, a `Future` which can be used to cancel the invocation:

```java
Future future = upnpService.getControlPoint().execute(setTargetCallback);
Thread.sleep(500);
future.cancel(true);
```

Here we are calling the `SetTarget` action of a *SwitchPower:1* service, and after waiting a (short) time period, we cancel the request.
What happens now depends on the invocation and what service you are calling.
If it's a local service, and no network access is needed, the thread calling the local service (method) will simply be interrupted.
If you are calling a remote service, jUPnP will abort the HTTP request to the server.

Most likely you want to handle this explicit cancellation of an action call in your action invocation callback, so you can present the result to your user.
Override the `failure()` method to handle the interruption:

```java
ActionCallback setTargetCallback = new ActionCallback(setTargetInvocation) {
    @Override
    public void success(ActionInvocation invocation) {
        // Will not be called if invocation has been cancelled
    }

    @Override
    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        if (invocation.getFailure() instanceof ActionCancelledException) {
            // Handle the cancellation here...
        }
    }
};
```

A special exception type is provided if the action call was indeed cancelled.

Several important issues have to be considered when you try to cancel action calls to remote services:

There is no guarantee that the server will actually stop processing your request.
When the client closes the connection, the server doesn't get notified.
The server will complete the action call and only fail when trying to return the response to the client on the closed connection.
jUPnP's server transports offer a special heartbeat feature for checking client connections, we'll discuss this feature later in this chapter.
Other UPnP servers will most likely not detect a dropped client connection immediately.

Not all HTTP client transports in jUPnP support interruption of requests:

| **Transport**                                                          | **Supports Interruption?** |
|------------------------------------------------------------------------|:--------------------------:|
| `org.jupnp.transport.impl.StreamClientImpl` (default)                  |             NO             |
| `org.jupnp.transport.impl.jetty.StreamClientImpl` (default on Android) |             YES            |

Transports which do not support cancellation won't produce an error when you abort an action invocation, they silently ignore the interruption and continue waiting for the server to respond.

### Reacting to cancellation on the server

By default, an action method of your service will run until it completes, it either returns or throws an exception.
If you have to perform long-running tasks in a service, your action method can avoid doing unnecessary work by checking if processing should continue.
Think about processing in batches: You work for a while, then you check if you should continue, then you work some more, check again, and so on.

There are two checks you have to perform:

* If a local control point called your service, and meanwhile cancelled the action call, the thread running your action method will have its interruption flag set.
  When you see this flag you can stop processing, as any result of your action method will be ignored anyway.
* If a remote control point called your service, it might have dropped the connection while you were processing data to return.
  Unfortunately, checking if the client's connection is still open requires, on a TCP level, writing data on the socket.
  This is essentially a heartbeat signal: Every time you check if the client is still there, a byte of (hopefully) insignificant data will be send to the client.
  If there wasn't any error sending data, the connection is still alive.

These checks look as follows in your service method:

```java
@UpnpAction
public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue,
        RemoteClientInfo remoteClientInfo) throws InterruptedException {
    boolean interrupted = false;
    while (!interrupted) {
        // Do some long-running work and periodically test if you should continue...

        // ... for local service invocation
        if (Thread.interrupted()) {
            interrupted = true;
        }

        // ... for remote service invocation
        if (remoteClientInfo != null && remoteClientInfo.isRequestCancelled()) {
            interrupted = true;
        }
    }
    throw new InterruptedException("Execution interrupted");
}
```

You abort processing by throwing an `InterruptedException`, jUPnP will do the rest.
jUPnP will send a heartbeat to the client whenever you check if the remote request was cancelled with the optional `RemoteClientInfo`, see [this section](#accessing-remote-client-information).

:::danger

Not all HTTP clients can deal with jUPnP's heartbeat signal.
Not even all bundled `StreamClient`'s of jUPnP can handle such a signal.
You should only use this feature if you are sure that all clients of your service will ignore the meaningless heartbeat signal.
jUPnP sends a space character (this is configurable) to the HTTP client to check the connection.
Hence, the HTTP client sees a response such as '[space][space][space]HTTP/1.0', with a space character for each alive check.
If your HTTP client does not trim those space characters before parsing the response, it will fail processing your otherwise valid response.

:::

The following jUPnP-bundled client transports can deal with a heartbeat signal:

| Transport                                                              | Accepts Heartbeat? |
|------------------------------------------------------------------------|:------------------:|
| `org.jupnp.transport.impl.StreamClientImpl` (default)                  |         NO         |
| `org.jupnp.transport.impl.jetty.StreamClientImpl` (default on Android) |         YES        |

Equally important, not all server transports in jUPnP can send heartbeat signals, as low-level socket access is required.
Some server APIs do not provide this low-level access.
If you check the connection state with those transports, the connection is always "alive":

| Transport                                                                                                                                | Sends Heartbeat? |
|------------------------------------------------------------------------------------------------------------------------------------------|:----------------:|
| `org.jupnp.transport.impl.StreamServerImpl` (default)                                                                                    |        NO        |
| `org.jupnp.transport.impl.AsyncServletStreamServerImpl` with `org.jupnp.transport.impl.jetty.JettyServletContainer` (default on Android) |        YES       |

In practice, this heartbeat feature is less useful than it sounds in theory: As you usually don't control which HTTP clients will access your server, sending them "garbage" bytes before responding properly will most likely cause interoperability problems.

## Switching XML descriptor binders

UPnP utilizes XML documents to transport device and service information between the provider and any control points.
These XML documents have to be parsed by jUPnP to produce the `Device` model, and of course generated from a `Device` model.
The same approach is used for the `Service` model.
This parsing, generating, and binding is the job of the `org.jupnp.binding.xml.DeviceDescriptorBinder` and the `org.jupnp.binding.xml.ServiceDescriptorBinder`.

The following implementations are bundled with jUPnP Core for device descriptor binding:

<dl>
    <dt>`org.jupnp.binding.xml.UDA10DeviceDescriptorBinderImpl` (default)</dt>
    <dd>
        This implementation reads and writes UDA 1.0 descriptor XML with the JAXP-provided DOM API provided by JDK 6.
        You do not need any additional libraries to use this binder.
        Use this binder to validate strict specification compliance of your applications.
    </dd>
    <dt>`org.jupnp.binding.xml.UDA10DeviceDescriptorBinderSAXImpl`</dt>
    <dd>
        This implementation reads and writes UDA 1.0 descriptor XML with the JAXP-provided SAX API, you don't have to install additional libraries to use it.
        This binder may consume less memory when
        reading XML descriptors and perform better than the DOM-based parser.
        In practice, the difference is usually insignificant, even on very slow machines.
    </dd>
    <dt>`org.jupnp.binding.xml.RecoveringUDA10DeviceDescriptorBinderImpl`</dt>
    <dd>
        This implementation extends `UDA10DeviceDescriptorBinderImpl` and tries to recover from parsing failures, and works around known problems of other UPnP stacks.
        This is the binder you want for best interoperability in real-world UPnP applications.
        Furthermore, you can override the `handleInvalidDescriptor()` and `handleInvalidDevice()` methods to customize error handling, or if you want to repair invalid device information manually.
        It is the default binder for `AndroidUpnpServiceConfiguration`.
    </dd>
</dl>

The following implementations are bundled with jUPnP Core for service descriptor binding:

<dl>
    <dt>`org.jupnp.binding.xml.UDA10ServiceDescriptorBinderImpl` (default)</dt>
    <dd>
        This implementation reads and writes UDA 1.0 descriptor XML with the JAXP-provided DOM API provided by JDK 6.
        You do not need any additional libraries to use this binder.
        Use this binder to validate strict specification compliance of your applications.
    </dd>
    <dt>`org.jupnp.binding.xml.UDA10ServiceDescriptorBinderSAXImpl`</dt>
    <dd>
        This implementation reads and writes UDA 1.0 descriptor XML with the JAXP-provided SAX API, you don't have to install additional libraries to use it.
        This binder may consume less memory when reading XML descriptors and perform better than the DOM-based parser.
        In practice, the difference is usually insignificant, even on very slow machines.
        It is the default binder for `AndroidUpnpServiceConfiguration`.
    </dd>
</dl>

You can switch descriptor binders by overriding the `UpnpServiceConfiguration`:

```java
UpnpService upnpService = new UpnpServiceImpl(
    new DefaultUpnpServiceConfiguration() {
        @Override
        public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
            // Recommended for best interoperability with broken UPnP stacks!
            return new RecoveringUDA10DeviceDescriptorBinderImpl();
        }
    
        @Override
        public ServiceDescriptorBinder getServiceDescriptorBinderUDA10() {
            return new UDA10ServiceDescriptorBinderSAXImpl();
        }
    }
);
```

Performance problems with UPnP discovery are usually caused by too many XML descriptors, not by their size.
This is inherent in the bad design of UPnP; each device may expose many individual service descriptors, and jUPnP will always retrieve all device metadata.
The HTTP requests necessary to retrieve dozens of descriptor files usually outweighs the cost of parsing each.

Note that binders are only used for device and service descriptors, not for UPnP action and event message processing.

## Switching XML processors

All control and event UPnP messages have an XML payload, and the control messages are even wrapped in SOAP envelopes.
Handling XML for control and eventing is encapsulated in the jUPnP transport layer, with an extensible service provider interface:

<dl>
    <dt>`org.jupnp.transport.spi.SOAPActionProcessor`</dt>
    <dd>
        This processor reads and writes UPnP SOAP messages and transform them from/to `ActionInvocation` data.
        The protocol layer, on top of the transport layer, handles `ActionInvocation` only.
    </dd>
    <dt>`org.jupnp.transport.spi.GENAEventProcessor`</dt>
    <dd>
        This processor reads and writes UPnP GENA event messages and transform them from/to a `List<StateVariableValue>`.
    </dd>
</dl>

For the `SOAPActionProcessor`, the following implementations are bundled with jUPnP Core:

<dl>
    <dt>`org.jupnp.transport.impl.SOAPActionProcessorImpl` (default)</dt>
    <dd>
        This implementation reads and writes XML with the JAXP-provided DOM API provided by JDK 6.
        You do not need any additional libraries to use this processor.
        However, its strict compliance with the UPnP specification can cause problems in real-world UPnP applications.
        This processor will produce errors during reading when XML messages violate the UPnP specification.
        Use it to test a UPnP stack or application for strict specification compliance.
    </dd>
    <dt>`org.jupnp.transport.impl.PullSOAPActionProcessorImpl`</dt>
    <dd>
        This processor uses the XML Pull API to read messages, and the JAXP DOM API to write messages.
        You need an implementation of the XML Pull API on your classpath to use this processor, for example, [XPP3](https://search.maven.org/artifact/xpp3/xpp3) or [kXML 2](https://github.com/kobjects/kxml2).
        Compared with the default processor, this processor is much more lenient when reading action message XML.
        It can deal with broken namespacing, missing SOAP envelopes, and other problems.
        In UPnP applications where interoperability is more important than specification compliance, you should use this parser.
    </dd>
    <dt>`org.jupnp.transport.impl.RecoveringSOAPActionProcessorImpl`</dt>
    <dd>
        This processor extends the `PullSOAPActionProcessorImpl` and additionally will work around known bugs of UPnP stacks in the wild and try to recover from parsing failures by modifying the XML text in different ways.
        This is the processor you should use for best interoperability with other (broken) UPnP stacks.
        Furthermore, it let's you handle a failure when reading an XML message easily by overriding the `handleInvalidMessage()` method, e.g. to create or log an error report.
        It is the default processor for `AndroidUpnpServiceConfiguration`.
    </dd>
</dl>

For the `GENAEventProcessor`, the following implementations are bundled with jUPnP Core:

<dl>
    <dt>`org.jupnp.transport.impl.GENAEventProcessorImpl` (default)</dt>
    <dd>
        This implementation reads and writes XML with the JAXP-provided DOM API provided by JDK 6.
        You do not need any additional libraries to use this processor.
        However, its strict compliance with the UPnP specification can cause problems in real-world UPnP applications.
        This processor will produce errors during reading when XML messages violate the UPnP specification.
        Use it to test a UPnP stack or application for strict specification compliance.
    </dd>
    <dt>`org.jupnp.transport.impl.PullGENAEventProcessorImpl`</dt>
    <dd>
        This processor uses the XML Pull API to read messages, and the JAXP DOM API to write messages.
        You need an implementation of the XML Pull API on your classpath to use this processor, for example, [XPP3](https://search.maven.org/artifact/xpp3/xpp3) or [kXML 2](https://github.com/kobjects/kxml2).
        Compared with the default processor, this processor is much more lenient when reading action message XML.
        It can deal with broken namespacing, missing root element, and other problems.
        In UPnP applications where compatibility is more important than specification compliance, you should use this parser.
    </dd>
    <dt>`org.jupnp.transport.impl.RecoveringGENAEventProcessorImpl`</dt>
    <dd>
        This processor extends the `PullGENAEventProcessorImpl` and additionally will work around known bugs of UPnP stacks in the wild and try to recover from parsing failures by modifying the XML text in different ways.
        This is the processor you should use for best interoperability with other (broken) UPnP stacks.
        Furthermore, it will return partial results, when at least one single state variable value was successfully read from the event XML.
        It is the default processor for `AndroidUpnpServiceConfiguration`.
    </dd>
</dl>

You can switch XML processors by overriding the `UpnpServiceConfiguration`:

```java
UpnpService upnpService = new UpnpServiceImpl(
    new DefaultUpnpServiceConfiguration() {
        @Override
        public SOAPActionProcessor getSoapActionProcessor() {
            // Recommended for best interoperability with broken UPnP stacks!
            return new RecoveringSOAPActionProcessorImpl();
        }

        @Override
        public GENAEventProcessor getGenaEventProcessor() {
            // Recommended for best interoperability with broken UPnP stacks!
            return new RecoveringGENAEventProcessorImpl();
        }
    }
);
```

## Solving discovery problems

Device discovery in UPnP is the job of SSDP, the Simple Service Discovery Protocol.
Of course, this protocol is not simple at all and many device manufacturers and UPnP stacks get it wrong.
jUPnP has some extra settings to deal with such environments; if you want best interoperability for your application, you have to read the following sections.

### Maximum age of remote devices

If you are writing a control point and remote devices seem to randomly disappear from your `Registry`, you are probably dealing with a remote device that doesn't send regular alive NOTIFY heartbeats through multicast.
Or, your control point runs on a device that doesn't properly receive multicast messages.
(Android devices from HTC are known to have this issue.)

jUPnP will usually expire remote devices once their initially advertised "maximum age"
has been reached and there was no ALIVE message to refresh the advertisement.
You can change this behavior with `UpnpServiceConfiguration`:

```java
UpnpService upnpService = new UpnpServiceImpl(
    new DefaultUpnpServiceConfiguration() {
        @Override
        public Integer getRemoteDeviceMaxAgeSeconds() {
            return 0;
        }
    }
);
```

If you return zero maximum age, all remote devices will forever stay in your `Registry` once they have been discovered, jUPnP will not expire them.
You have to manually remove them from the `Registry` if you know they are gone (e.g. once an action request fails with "no response").

Alternatively, you can return the number of seconds jUPnP should keep a remote device in the `Registry`, ignoring the device's advertised maximum age.

### Alive messages at regular intervals

Some control points have difficulties with M-SEARCH responses.
They search for your device, then can't process the (specification-compliant) response made by jUPnP and therefore don't discover your device when they search.
However, such control points typically have no problem with alive NOTIFY messages, only with search responses.

The solution then is to repeat alive NOTIFY messages for all your local devices on the network very frequently, let's say every 5 seconds:

```java
UpnpService upnpService = new UpnpServiceImpl(
    new DefaultUpnpServiceConfiguration() {
        @Override
        public int getAliveIntervalMillis() {
            return 5000;
        }
    }
);
```

By default this method returns `0`, disabling alive message flooding and relying on the regular triggering of local device advertisements (which depends on the maximum age of each `LocalDeviceIdentity`).

If you return a non-zero value, jUPnP will send alive NOTIFY messages repeatedly with the given interval, and remote control points should be able to discover your device within that period.
The downside is of course more traffic on your network.

### Using discovery options for local devices

If you create a `LocalDevice` that you don't want to announce to remote control points, add it to the `Registry` with `addDevice(localDevice, new DiscoveryOptions(false))`.

The `DiscoveryOptions` class offers several parameters to influence how jUPnP handles device discovery.

With disabled advertising, jUPnP will then not send *any* NOTIFY messages for a device; you can enable advertisement again with `Registry#setDiscoveryOptions(UDN, null)`, or by providing different options.
                    
Note that remote control points will still be able to discover your device if they know your device descriptor URL.
They will also be able to call actions and subscribe to services.
This is not a switch to make a `LocalDevice` "private", it only disables (multicast) advertising.

A rarely used setting of `DiscoveryOptions` is `byeByeBeforeFirstAlive`:
If enabled, jUPnP will send a byebye NOTIFY message before sending the first alive NOTIFY message.
This happens only once, when a `LocalDevice` is added to the `Registry`, and it wasn't registered before.

### Manual advertisement of local devices

You can force immediate advertisement of all registered `LocalDevice`s with `Registry#advertiseLocalDevices()`.
Note that no announcements will be made for any device with disabled advertising (see previous section).
## Configuring network transports

jUPnP has to accept and make HTTP requests to implement UPnP discovery, action processing, and GENA eventing.
This is the job of the `StreamServer` and `StreamClient` implementations, working together with the `Router` as the jUPnP network transport layer.

For the `StreamClient` SPI, the following implementations are bundled with jUPnP:

<dl>
    <dt>`org.jupnp.transport.impl.StreamClientImpl` (default)</dt>
    <dd>
        This implementation uses the JDK's `HTTPURLConnection`, it doesn't require any additional libraries.
        Note that jUPnP has to customize (with an ugly hack, really) the VM's `URLStreamHandlerFactory` to support additional HTTP methods such as `NOTIFY`, `SUBSCRIBE`, and `UNSUBSCRIBE`.
        The designers of the JDK do not understand HTTP very well and made this extremely difficult to extend.
        jUPnP's patch only works if no other code in your environment has already set a custom `URLStreamHandlerFactory`, you will get an exception on startup if this issue is detected; then you have to switch to another `StreamClient` implementation.
        Note that this implementation does *NOT WORK* on Android, the `URLStreamHandlerFactory` can't be patched on Android!
    </dd>
    <dt>`org.jupnp.transport.impl.jetty.JettyStreamClientImpl`</dt>
    <dd>
        This implementation is based on the *Jetty 9.4* HTTP client, you need the artifact `org.eclipse.jetty:jetty-client:9.4` on your classpath to use it.
        This implementation works in any environment, including Android.
        It is the default transport for `AndroidUpnpServiceConfiguration`.
    </dd>
</dl>

For the `StreamServer` SPI, the following implementations are bundled with jUPnP:

<dl>
    <dt>`org.jupnp.transport.impl.async.AsyncServletStreamServerImpl`</dt>
    <dd>
        This implementation is based on the standard *Servlet 3.0* API and can be used in any environment with a compatible servlet container.
        It requires a `ServletContainerAdapter` to integrate with the servlet container, the bundled `JettyServletContainer` is such an adapter for a standalone *Jetty 9* server.
        You need the artifact `org.eclipse.jetty:jetty-servlet:9.4` on your classpath to use it.
        This implementation works in any environment, including Android.
        It is the default transport for `AndroidUpnpServiceConfiguration`.
        For other containers, write your own adapter and provide it to the `AsyncServletStreamServerConfigurationImpl`.
    </dd>
</dl>

Each `StreamClient` and `StreamServer` implementation is paired with an implementation of `StreamClientConfiguration` and `StreamServerConfiguration`.
This is how you override the jUPnP network transport configuration:

```java
...
public class MyUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

    @Override
    protected Namespace createNamespace() {
        return new Namespace("/upnp"); // This will be the servlet context path
    }

    @Override
    public StreamClient createStreamClient() {
        return new org.jupnp.transport.impl.jetty.StreamClientImpl(
            new org.jupnp.transport.impl.jetty.JettyStreamClientConfigurationImpl(
                getSyncProtocolExecutor()
            )
        );
    }

    @Override
    public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
        return new org.jupnp.transport.impl.AsyncServletStreamServerImpl(
            new org.jupnp.transport.impl.AsyncServletStreamServerConfigurationImpl(
                org.jupnp.transport.impl.jetty.JettyServletContainer.INSTANCE,
                networkAddressFactory.getStreamListenPort()
            )
        );
    }

}
```

The above configuration will use the Jetty client and the Jetty servlet container.
The `JettyServletContainer.INSTANCE` adapter is managing a standalone singleton server, it is started and stopped when jUPnP starts and stops the UPnP stack.
If you have run jUPnP with an existing, external servlet container, provide a custom adapter.
