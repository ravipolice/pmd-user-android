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
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# --- Gson ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}


# --- Retrofit ---
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# --- Coroutines ---
-keep class kotlinx.coroutines.** { *; }

# --- Credential Manager & Identity ---
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

# --- Models (Keep all data classes used in JSON parsing) ---
-keep class com.example.policemobiledirectory.model.** { *; }
-keep class com.example.policemobiledirectory.data.local.** { *; }

# --- Retrofit API Interfaces ---
-keep interface com.example.policemobiledirectory.api.** { *; }
-keep class com.example.policemobiledirectory.api.** { *; }

# --- Remote Data/Services (Fixes ImageRepository Crash) ---
-keep class com.example.policemobiledirectory.data.remote.** { *; }
-keep interface com.example.policemobiledirectory.data.remote.** { *; }

# --- App Specific ---
-keep class com.example.policemobiledirectory.di.** { *; }

# --- Apache POI ---
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**

# --- PDFBox ---
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# --- uCrop ---
-keep class com.yalantis.ucrop.** { *; }
-keep interface com.yalantis.ucrop.** { *; }
-dontwarn com.yalantis.ucrop.**

# --- Javax / AWT classes for POI ---
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
