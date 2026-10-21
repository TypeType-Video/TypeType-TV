-keepattributes *Annotation*,InnerClasses,Signature
-keepclassmembers class video.typetype.tv.data.dto.** { *; }
-keep,includedescriptorclasses class video.typetype.tv.data.dto.**$$serializer { *; }
-keepclassmembers class video.typetype.tv.data.dto.** {
    *** Companion;
}
-keepclasseswithmembers class video.typetype.tv.data.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.crypto.tink.**
