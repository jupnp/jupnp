# region Upnp
-keepattributes *Annotation*

-keepclassmembers class ** extends org.jupnp.model.message.header.UpnpHeader {
   public <init>(...);
}

# When optimization is enabled, it is essential to preserve the annotations.
-keep @interface org.jupnp.binding.annotations.**

# Uncertain about the reason for the removal of annotations.
# These lines are used to preserve the names of classes, which in turn keeps the annotations intact.
# Since we create the UpnpService via AnnotationLocalServiceBinder.read(Class<?>), it is safe to allow shrinking.
-keepnames @org.jupnp.binding.annotations.UpnpService class **

-keepclassmembers @org.jupnp.binding.annotations.UpnpService class ** {
    public <init>(...);
    @org.jupnp.binding.annotations.UpnpStateVariable <fields>;
    @org.jupnp.binding.annotations.UpnpAction <methods>;
    public <methods>;
    public <fields>;
}

-dontwarn org.osgi.service.component.annotations.Component
-dontwarn org.osgi.service.metatype.annotations.Designate
# endregion
