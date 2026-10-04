# Proguard rules for SenseBridge

# --- Google ML Kit ---
-keep class com.google.mlkit.** { *; }

# --- TensorFlow Lite & Native JNI ---
-keep class org.tensorflow.** { *; }
-keepclassmembers class * {
    native <methods>;
}

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- SenseBridge Domain Models & Entities ---
-keep class com.sensebridge.core.model.** { *; }
-keep class com.sensebridge.data.local.entity.** { *; }
-keep class com.sensebridge.data.repository.UserPreferences { *; }

# --- Coroutines ---
-dontwarn kotlinx.coroutines.**
