# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
# --- Android Core ---
-keepattributes SourceFile,LineNumberTable
-keep class androidx.lifecycle.** { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Gson ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# --- Retrofit ---
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# --- Coroutines ---
-keep class kotlinx.coroutines.** { *; }

# --- Models (Keep all data classes used in JSON parsing) ---
-keep class com.example.policemobiledirectory.model.** { *; }
-keep class com.example.policemobiledirectory.data.local.** { *; }

# --- App Specific ---
-keep class com.example.policemobiledirectory.di.** { *; }
