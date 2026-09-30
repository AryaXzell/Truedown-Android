# Truedown Android ProGuard / R8 Rules

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Moshi JSON Converter
-keep class com.squareup.moshi.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# WorkManager
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep Data Entities and Models
-keep class com.example.data.local.** { *; }
-keep class com.example.domain.model.** { *; }
-keep class com.example.data.preferences.** { *; }
