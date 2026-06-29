# kotlinx.serialization: keep @Serializable metadata and generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.l5rcm.companion.**$$serializer { *; }
-keepclassmembers class com.l5rcm.companion.** {
    *** Companion;
}
-keep class com.l5rcm.companion.data.** { *; }
