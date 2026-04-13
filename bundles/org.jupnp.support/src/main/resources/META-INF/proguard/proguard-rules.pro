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

-dontwarn org.slf4j.Logger
-dontwarn org.slf4j.LoggerFactory
# endregion

