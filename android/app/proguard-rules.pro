# ProGuard rules for Jalan3D
# Keep Retrofit and Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.jalan3d.data.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
