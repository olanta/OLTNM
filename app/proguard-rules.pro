-keepattributes *Annotation*, InnerClasses, Signature

# kotlinx.serialization keeps generated serializers reachable through reflection.
-keepclassmembers class app.oltnm.data.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class app.oltnm.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
