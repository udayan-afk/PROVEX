# Keep kotlinx.serialization generated serializers for network DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.provex.app.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.provex.app.data.remote.dto.** {
    *** Companion;
}
