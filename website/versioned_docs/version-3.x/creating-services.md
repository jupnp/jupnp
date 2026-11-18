---
sidebar_position: 5
---

# Creating and binding services

Out of the box, any Java class can be a UPnP service.
Let's go back to the first example of a UPnP service in the previous chapter, the [SwitchPower:1](binary-light.md#the-switchpower-service-implementation) service implementation, repeated here:

```java
package example.binarylight;

import org.jupnp.binding.annotations.*;

@UpnpService(
    serviceId = @UpnpServiceId("SwitchPower"),
    serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
)
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

This class depends on the `org.jupnp.annotation` package at compile-time.
The metadata encoded in these source annotations is preserved in the bytecode and jUPnP will read it at runtime when you [bind the service](binary-light.md#binding-a-upnp-device) ("binding" is just a fancy word for reading and writing metadata).
You can load and execute this class without accessing the annotations, in any environment and without having the jUPnP libraries on your classpath.
This is a compile-time dependency only.

jUPnP annotations give you much flexibility in designing your service implementation class, as shown in the following examples.

## Annotating a service implementation

The previously shown service class had a few annotations on the class itself, declaring the name and version of the service.
Then annotations on fields were used to declare the state variables of the service and annotations on methods to declare callable actions.

Your service implementation might not have fields that directly map to UPnP state variables.

### Mapping state variables

The following example only has a single field named `power`, however, the UPnP service requires two state variables.
In this case you declare the UPnP state variables with annotations on the class:

```java
@UpnpService(
        serviceId = @UpnpServiceId("SwitchPower"),
        serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
)
@UpnpStateVariables({
        @UpnpStateVariable(name = "Target", defaultValue = "0", sendEvents = false),
        @UpnpStateVariable(name = "Status", defaultValue = "0")
})
public class SwitchPowerAnnotatedClass {

    private boolean power;

    @UpnpAction
    public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue) {
        power = newTargetValue;
        System.out.println("Switch is: " + power);
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
    public boolean getTarget() {
        return power;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
    public boolean getStatus() {
        return power;
    }
}
```

The `power` field is not mapped to the state variables and you are free to design your service internals as you like.
Did you notice that you never declared the datatype of your state variables?
Also, how can jUPnP read the "current state" of your service for GENA subscribers or when a "query state variable" action is received?
Both questions have the same answer.

Let's consider GENA eventing first.
This example has an evented state variable called `Status`, and if a control point subscribes to the service to be notified of changes, how will jUPnP obtain the current status?
If you'd have used `@UpnpStateVariable` on your fields, jUPnP would then directly access field values through Java Reflection.
On the other hand if you declare state variables not on fields but on your service class, jUPnP will during binding detect any JavaBean-style getter method that matches the derived property name of the state variable.

In other words, jUPnP will discover that your class has a `getStatus()` method.
It doesn't matter if that method is also an action-mapped method, the important thing is that it matches JavaBean property naming conventions.
The `Status` UPnP state variable maps to the `status` property, which is expected to have a `getStatus()` accessor method.
jUPnP will use this method to read the current state of your service for GENA subscribers and when the state variable is manually queried.

If you do not provide a UPnP datatype name in your `@UpnpStateVariable` annotation, jUPnP will use the type of the annotated field or discovered JavaBean getter method to figure out the type.
The supported default mappings between Java types and UPnP datatypes are shown in the following table:

| **Java Type**                                    | **UPnP Datatype** |
|--------------------------------------------------|-------------------|
| `java.lang.Boolean`                              | `boolean`         |
| `boolean`                                        | `boolean`         |
| `java.lang.Short`                                | `i2`              |
| `short`                                          | `i2`              |
| `java.lang.Integer`                              | `i4`              |
| `int`                                            | `i4`              |
| `org.jupnp.model.types.UnsignedIntegerOneByte`   | `ui1`             |
| `org.jupnp.model.types.UnsignedIntegerTwoBytes`  | `ui2`             |
| `org.jupnp.model.types.UnsignedIntegerFourBytes` | `ui4`             |
| `java.lang.Float`                                | `r4`              |
| `float`                                          | `r4`              |
| `java.lang.Double`                               | `float`           |
| `double`                                         | `float`           |
| `java.lang.Character`                            | `char`            |
| `char`                                           | `char`            |
| `java.lang.String`                               | `string`          |
| `java.util.Calendar`                             | `datetime`        |
| `byte[]`                                         | `bin.base64`      |
| `java.net.URI`                                   | `uri`             |

jUPnP tries to provide smart defaults.
For example, the previously shown service classes did not name the related state variable of action output arguments, as required by UPnP.
jUPnP will automatically detect that the `getStatus()` method is a JavaBean getter method (its name starts with `get` or `is`) and use the JavaBean property name to find the related state variable.
In this case that would be the JavaBean property `status` and jUPnP is also smart enough to know that you really want the uppercase UPnP state variable named `Status`.

### Explicitly naming related state variables

If your mapped action method does not match the name of a mapped state variable, you have to provide the name of (any) argument's related state variable:

```java
@UpnpAction(name = "GetStatus", out = @UpnpOutputArgument(name = "ResultStatus", stateVariable = "Status"))
public boolean retrieveStatus() {
    return status;
}
```

Here the method has the name `retrieveStatus`, which you also have to override if you want it be known as a the `GetStatus` UPnP action.
Because it is no longer a JavaBean accessor for `status`, it explicitly has to be linked with the related state variable `Status`.
You always have to provide the related state variable name if your action has more than one output argument.

The "related statevariable" detection algorithm in jUPnP has one more trick up its sleeve however.
The UPnP specification says that a state variable which is only ever used to describe the type of an input or output argument should be named with the prefix `A_ARG_TYPE_`.
So if you do not name the related state variable of your action argument, jUPnP will also look for a state variable with the name `A_ARG_TYPE_[Name Of Your Argument]`.
In the example above, jUPnP is therefore also searching (unsuccessfully) for a state variable named `A_ARG_TYPE_ResultStatus`.
(Given that direct querying of state variables is already deprecated in UDA 1.0, there are *NO* state variables which are anything but type declarations for action input/output arguments.
This is a good example why UPnP is such a horrid specification.)

For the next example, let's assume you have a class that was already written, not necessarily as a service backend for UPnP but for some other purpose.
You can't redesign and rewrite your class without interrupting all existing code.
jUPnP offers some flexibility in the mapping of action methods, especially how the output of an action call is obtained.

### Getting an output value from another method

In the following example, the UPnP action has an output argument but the mapped method is void and does not return any value:

```java
public boolean getStatus() {
    return status;
}

@UpnpAction(name = "GetStatus", out = @UpnpOutputArgument(name = "ResultStatus", getterName = "getStatus"))
public void retrieveStatus() {
    // NOOP in this example
}
```

By providing a <code>getterName</code> in the annotation you can instruct jUPnP to call this getter method when the action method completes, taking the getter method's return value as the output argument value.
If there are several output arguments you can map each to a different getter method.

Alternatively, and especially if an action has several output arguments, you can return multiple values wrapped in a JavaBean from your action method.

### Getting output values from a JavaBean

Here the action method does not return the output argument value directly, but a JavaBean instance is returned which offers a getter method to obtain the output argument value:

```java
@UpnpAction(name = "GetStatus", out = @UpnpOutputArgument(name = "ResultStatus", getterName = "getWrapped"))
public StatusHolder getStatus() {
    return new StatusHolder(status);
}

public static class StatusHolder {
    boolean wrapped;

    public StatusHolder(boolean wrapped) {
        this.wrapped = wrapped;
    }

    public boolean getWrapped() {
        return wrapped;
    }
}
```

jUPnP will detect that you mapped a getter name in the output argument and that the action method is not `void`.
It now expects that it will find the getter method on the returned JavaBean.
If there are several output arguments, all of them have to be mapped to getter methods on the returned JavaBean.

An important piece is missing from the [SwitchPower:1](binary-light.md#the-switchpower-service-implementation) implementation: It doesn't fire any events when the status of the power switch changes.
This is in fact required by the specification that defines the *SwitchPower:1* service.
The following section explains how you can propagate state changes from within your UPnP service to local and remote subscribers.

## Providing events on service state changes

The standard mechanism in the JDK for eventing is the `PropertyChangeListener` reacting on a `PropertyChangeEvent`.
jUPnP utilizes this API for service eventing, thus avoiding a dependency between your service code and proprietary APIs.

Consider the following modification of the original [SwitchPower:1](binary-light.md#the-switchpower-service-implementation) implementation:

```java
package example.localservice;

import org.jupnp.binding.annotations.*;
import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;

@UpnpService(
    serviceId = @UpnpServiceId("SwitchPower"),
    serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
)
public class SwitchPowerWithPropertyChangeSupport {

    private final PropertyChangeSupport propertyChangeSupport;

    public SwitchPowerWithPropertyChangeSupport() {
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

        // This will send a UPnP event, it's the name of a state variable that triggers events
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

The only additional dependency is on `java.beans.PropertyChangeSupport`.
jUPnP detects the `getPropertyChangeSupport()` method of your service class and automatically binds the service management on it.
You will have to have this method for eventing to work with jUPnP.
You can create the `PropertyChangeSupport` instance in your service's constructor or any other way, the only thing jUPnP is interested in are property change events with the "property" name of a UPnP state variable.

Consequently, `firePropertyChange("NameOfAStateVariable")` is how you tell jUPnP that a state variable value has changed.
It doesn't even matter if you call `firePropertyChange("Status", null, null)` or `firePropertyChange("Status", oldValue, newValue)`.
jUPnP *only* cares about the state variable name; it will then check if the state variable is evented and pull the data out of your service implementation instance by accessing the appropriate field or a getter.
Any "old" or "new" value you pass along is ignored.

Also note that `firePropertyChange("Target", null, null)` would have no effect, because `Target` is mapped with `sendEvents="false"`.

Most of the time a JavaBean property name is *not* the same as UPnP state variable name.
For example, the JavaBean `status` property name is lowercase, while the UPnP state variable name is uppercase `Status`.
The jUPnP eventing system ignores any property change event that doesn't exactly name a service state variable.
This allows you to use JavaBean eventing independently from UPnP eventing, e.g. for GUI binding (Swing components also use the JavaBean eventing system).

Let's assume for the sake of the next example that `Target` actually is also evented, like `Status`.
If several evented state variables change in your service, but you don't want to trigger individual change events for each variable, you can combine them in a single event as a comma-separated list of state variable names:

```java
@UpnpAction
public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue) {
    target = newTargetValue;
    status = newTargetValue;

    // If several evented variables changed, bundle them in one event separated with commas:
    getPropertyChangeSupport().firePropertyChange("Target, Status", null, null);
}
```

More advanced mappings are possible and often required, as shown in the next examples.
We are now leaving the *SwitchPower* service behind, as it is no longer complex enough.

## Converting string action argument values

The UPnP specification defines no framework for custom datatypes.
The predictable result is that service designers and vendors are overloading strings with whatever semantics they consider necessary for their particular needs.
For example, the UPnP A/V specifications often require lists of values (like a list of strings or a list of numbers), which are then transported between service and control point as a single string - the individual values are represented in this string separated by commas.

jUPnP supports these conversions and it tries to be as transparent as possible.

### String value converters

Consider the following service class with all state variables of `string` UPnP datatype - but with a much more specific Java type:

```java
import org.jupnp.binding.annotations.*;
import org.jupnp.model.types.csv.CSV;
import org.jupnp.model.types.csv.CSVInteger;

@UpnpService(
    serviceId = @UpnpServiceId("MyService"),
    serviceType = @UpnpServiceType(namespace = "mydomain", value = "MyService"),
    stringConvertibleTypes = MyStringConvertible.class
)
public class MyServiceWithStringConvertibles {

    @UpnpStateVariable
    private URL myURL;

    @UpnpStateVariable
    private URI myURI;

    @UpnpStateVariable(datatype = "string")
    private List<Integer> myNumbers;

    @UpnpStateVariable
    private MyStringConvertible myStringConvertible;

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public URL getMyURL() {
        return myURL;
    }

    @UpnpAction
    public void setMyURL(@UpnpInputArgument(name = "In") URL myURL) {
        this.myURL = myURL;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public URI getMyURI() {
        return myURI;
    }

    @UpnpAction
    public void setMyURI(@UpnpInputArgument(name = "In") URI myURI) {
        this.myURI = myURI;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public CSV<Integer> getMyNumbers() {
        CSVInteger wrapper = new CSVInteger();
        if (myNumbers != null) {
            wrapper.addAll(myNumbers);
        }
        return wrapper;
    }

    @UpnpAction
    public void setMyNumbers(@UpnpInputArgument(name = "In") CSVInteger myNumbers) {
        this.myNumbers = myNumbers;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public MyStringConvertible getMyStringConvertible() {
        return myStringConvertible;
    }

    @UpnpAction
    public void setMyStringConvertible(
            @UpnpInputArgument(name = "In") MyStringConvertible myStringConvertible) {
        this.myStringConvertible = myStringConvertible;
    }
}
```

The state variables are all of UPnP datatype `string` because jUPnP knows that the Java type of the annotated field is "string convertible".
This is always the case for `java.net.URI` and `java.net.URL`.

Any other Java type you'd like to use for automatic string conversion has to be named in the `@UpnpService` annotation on the class, like the `MyStringConvertible`.
Note that these types have to have an appropriate `toString()` method and a single argument constructor that accepts a `java.lang.String` ("from string" conversion).

The `List<Integer>` is the collection you'd use in your service implementation to group several numbers.
Let's assume that for UPnP communication you need a comma-separated representation of the individual values in a string, as is required by many of the UPnP A/V specifications.
First, tell jUPnP that the state variable really is a string datatype, it can't infer that from the field type.
Then, if an action has this output argument, instead of manually creating the comma-separated string you pick the appropriate converter from the classes in `org.jupnp.model.types.csv.*` and return it from your action method.
These are actually `java.util.List` implementations, so you could use them *instead* of `java.util.List` if you don't care about the dependency.
Any action input argument value can also be converted from a comma-separated string representation to a list automatically - all you have to do is use the CSV converter class as an input argument type.

### Working with enums

Java `enum`'s are special, unfortunately: You can't instantiate an enum value through reflection.
So jUPnP can convert your enum value into a string for transport in UPnP messages, but you have to convert it back manually from a string.
This is shown in the following service example:

```java
@UpnpService(
    serviceId = @UpnpServiceId("MyService"),
    serviceType = @UpnpServiceType(namespace = "mydomain", value = "MyService"),
    stringConvertibleTypes = MyStringConvertible.class
)
public class MyServiceWithEnum {

    public enum Color {
        Red,
        Green,
        Blue
    }

    @UpnpStateVariable
    private Color color;

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public Color getColor() {
        return color;
    }

    @UpnpAction
    public void setColor(@UpnpInputArgument(name = "In") String color) {
        this.color = Color.valueOf(color);
    }
}
```

jUPnP will automatically assume that the datatype is a UPnP string if the field (or getter) or getter Java type is an enum.
Furthermore, an `<allowedValueList>` will be created in your service descriptor XML, so control points know that this state variable has in fact a defined set of possible values.

## Restricting allowed state variable values

The UPnP specification defines a set of rules for restricting legal values of state variables, in addition to their type.
For string-typed state variables, you can provide a list of exclusively allowed strings.
For numeric state variables, a value range with minimum, maximum, and allowed "step" (the interval) can be provided.

### Exclusive list of string values

If you have a static list of legal string values, set it directly on the annotation of your state variable's field:

```java
@UpnpStateVariable(allowedValues = { "Foo", "Bar", "Baz" })
private String restricted;
```

Alternatively, if your allowed values have to be determined dynamically when your service is being bound, you can implement a class with the `org.jupnp.binding.AllowedValueProvider` interface:

```java
public static class MyAllowedValueProvider implements AllowedValueProvider {
    @Override
    public String[] getValues() {
        return new String[] { "Foo", "Bar", "Baz" };
    }
}
```

Then, instead of specifying a static list of string values in your state variable declaration, name the provider class:

```java
@UpnpStateVariable(allowedValueProvider = MyAllowedValueProvider.class)
private String restricted;
```

Note that this provider will only be queried when your annotations are being processed, once when your service is bound in jUPnP.

### Restricting numeric value ranges

For numeric state variables, you can limit the set of legal values within a range when declaring the state variable:

```java
@UpnpStateVariable(allowedValueMinimum = 10, allowedValueMaximum = 100, allowedValueStep = 5)
private int restricted;
```

Alternatively, if your allowed range has to be determined dynamically when your service is being bound, you can implement a class with the `org.jupnp.binding.AllowedValueRangeProvider` interface:

```java
public static class MyAllowedValueProvider implements AllowedValueRangeProvider {
    @Override
    public long getMinimum() {
        return 10;
    }

    @Override
    public long getMaximum() {
        return 100;
    }

    @Override
    public long getStep() {
        return 5;
    }
}
```

Then, instead of specifying a static list of string values in your state variable declaration, name the provider class:

```java
@UpnpStateVariable(allowedValueRangeProvider = MyAllowedValueProvider.class)
private int restricted;
```

Note that this provider will only be queried when your annotations are being processed, once when your service is bound in jUPnP.
