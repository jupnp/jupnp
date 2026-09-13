# region Upnp-Support
-keep class org.jupnp.support.model.DIDLObject$Property {
    <fields>;
    <methods>;
}

-keep class org.jupnp.support.model.DIDLObject$Property$** {
    <fields>;
    <methods>;
    public <init>();
    protected <init>(...);
}

-keep interface org.jupnp.support.model.DIDLObject$*$NAMESPACE {
    <fields>;
}

-keep class * extends org.jupnp.support.lastchange.EventedValue {
    public <init>(java.util.Map$Entry[]);
}

-dontwarn org.slf4j.Logger
-dontwarn org.slf4j.LoggerFactory
# endregion

