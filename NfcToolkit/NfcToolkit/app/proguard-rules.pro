# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.omar.nfctoolkit.** {
    *** Companion;
}
-keepclasseswithmembers class com.omar.nfctoolkit.** {
    kotlinx.serialization.KSerializer serializer(...);
}
