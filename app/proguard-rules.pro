# ===================================================================
# Flux Recorder - Advanced R8 Optimization & ProGuard Configuration
# ===================================================================

# Aggressive optimizations: allow access modification & class repackaging
-allowaccessmodification
-repackageclasses 'com.flux.recorder.a'
-optimizationpasses 5

# Strip release debug logging to save bytecode, CPU, and memory
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Suppress warnings for Netty BlockHound
-dontwarn reactor.blockhound.integration.BlockHoundIntegration

# Keep Media3 Transformer, Effects, Codecs, and UI
-keep class androidx.media3.transformer.** { *; }
-keep class androidx.media3.effect.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**

# Keep Camera2 and CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep Hilt & Application
-keep class * extends dagger.hilt.internal.UnsafeCasts { *; }
-keep class * extends com.flux.recorder.FluxRecorderApplication { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.app.Activity { *; }

# Keep data models, Enums, and Parcelables
-keepclassmembers enum * { *; }
-keepclassmembers class com.flux.recorder.data.** { *; }
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}