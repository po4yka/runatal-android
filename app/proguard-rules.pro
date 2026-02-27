# ====================================================================================================
# Runic Quotes - ProGuard/R8 Optimization Rules
# ====================================================================================================
# These rules optimize the app for release builds while preserving necessary classes
# and methods for runtime reflection, serialization, and Android framework interactions.
#
# For more details, see:
#   http://developer.android.com/guide/developing/tools/proguard.html
# ====================================================================================================

# ====================================================================================================
# DEBUGGING & CRASH REPORTING
# ====================================================================================================
# Keep line numbers and source file names for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations for debugging and runtime processing
-keepattributes *Annotation*, Signature, Exception, InnerClasses, EnclosingMethod

# ====================================================================================================
# KOTLIN & COROUTINES
# ====================================================================================================
# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin serialization
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.po4yka.runicquotes.**$$serializer { *; }
-keepclassmembers class com.po4yka.runicquotes.** {
    *** Companion;
}
-keepclasseswithmembers class com.po4yka.runicquotes.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ====================================================================================================
# JETPACK COMPOSE
# ====================================================================================================
# R8 handles Compose natively via consumer rules shipped with the Compose libraries.
# Only keep what the consumer rules do not already cover.

# Keep Compose compiler metadata for reflection
-keepattributes RuntimeVisibleAnnotations

# ====================================================================================================
# HILT / DAGGER
# ====================================================================================================
# Keep Hilt generated classes
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep @Inject constructors
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep Hilt modules
-keep @dagger.hilt.InstallIn class *
-keep @dagger.Module class *
-keep @dagger.hilt.components.SingletonComponent class *

# Keep entry points
-keep @dagger.hilt.EntryPoint class *

# Don't warn about Hilt dependencies
-dontwarn com.google.errorprone.annotations.**
-dontwarn dagger.hilt.android.internal.**

# ====================================================================================================
# ROOM DATABASE
# ====================================================================================================
# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Room DAO methods
-keepclassmembers,allowobfuscation class * extends androidx.room.RoomDatabase {
    public abstract * *Dao();
}

# Keep Room entity fields
-keepclassmembers class * {
    @androidx.room.* <fields>;
}

# Don't warn about Room paging
-dontwarn androidx.room.paging.**

# ====================================================================================================
# DATASTORE
# ====================================================================================================
# Keep DataStore classes
-keep class androidx.datastore.*.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ====================================================================================================
# WORKMANAGER
# ====================================================================================================
# Keep WorkManager workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ====================================================================================================
# GLANCE (WIDGETS)
# ====================================================================================================
# Keep Glance widget classes
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver
-dontwarn androidx.glance.**

# ====================================================================================================
# NAVIGATION COMPOSE
# ====================================================================================================
# Keep Navigation classes
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }

# Keep serializable navigation arguments
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ====================================================================================================
# APP-SPECIFIC RULES
# ====================================================================================================
# Keep domain models (used by Room, serialization, and UI)
-keep class com.po4yka.runicquotes.domain.model.** { *; }
-keep class com.po4yka.runicquotes.data.local.entity.** { *; }
-keep class com.po4yka.runicquotes.data.preferences.** { *; }

# Keep transliterators (core app functionality)
-keep class com.po4yka.runicquotes.domain.transliteration.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep enum classes (RunicScript, etc.)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ====================================================================================================
# OPTIMIZATION
# ====================================================================================================
# Enable aggressive optimization
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Optimize methods
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove logging in release (optional - uncomment if desired)
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
# }

# ====================================================================================================
# WARNINGS TO IGNORE
# ====================================================================================================
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn edu.umd.cs.findbugs.annotations.**
