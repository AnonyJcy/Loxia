# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep model classes for JSON serialization
-keep class com.cy.loxia.Wardrobe { *; }
-keep class com.cy.loxia.DressItem { *; }

# Keep BroadcastReceiver
-keep class com.cy.loxia.ReminderReceiver { *; }
-keep class com.cy.loxia.BootReceiver { *; }

# Keep AlarmScheduler
-keep class com.cy.loxia.AlarmScheduler { *; }

# Keep DataBackupManager
-keep class com.cy.loxia.DataBackupManager { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Room entities, DAO, and TypeConverters
-keep class com.cy.loxia.data.db.** { *; }

# Keep Gson TypeToken for TypeConverters
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}