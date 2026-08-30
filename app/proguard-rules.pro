# Add project specific ProGuard rules here.

# Suppress warnings for Netty's BlockHound integration
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

# Keep Hilt and Dagger
-keep class * extends dagger.hilt.internal.UnsafeCasts { *; }
-keep class * extends com.flux.recorder.FluxRecorderApplication { *; }

# Keep data classes and Parcelables
-keep class com.flux.recorder.data.** { *; }
-keep class * implements android.os.Parcelable { *; }