# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Optimization flags for smaller APK
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# More aggressive optimizations for smaller size
-overloadaggressively
-mergeinterfacesaggressively
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove System.out.println calls
-assumenosideeffects class java.lang.System {
    public static void out.println(...);
    public static void err.println(...);
}

# Remove printStackTrace calls
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

# Keep essential classes
-keep class com.offgrid.android.protocol.** { *; }
-keep class com.offgrid.android.crypto.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }

# Keep SecureIdentityStateManager from being obfuscated to prevent reflection issues
-keep class com.offgrid.android.identity.SecureIdentityStateManager {
    private android.content.SharedPreferences prefs;
    *;
}

# Keep all classes that might use reflection
-keep class com.offgrid.android.favorites.** { *; }
-keep class com.offgrid.android.nostr.** { *; }
-keep class com.offgrid.android.identity.** { *; }

# Keep Tor implementation (always included)
-keep class com.offgrid.android.net.RealTorProvider { *; }

# Arti (Custom Tor implementation in Rust) ProGuard rules
-keep class info.guardianproject.arti.** { *; }
-keep class org.torproject.arti.** { *; }
-keepnames class org.torproject.arti.**
-dontwarn info.guardianproject.arti.**
-dontwarn org.torproject.arti.**

# Fix for AbstractMethodError on API < 29 where LocationListener methods are abstract
-keepclassmembers class * implements android.location.LocationListener {
    public <methods>;
}

# Aggressive optimization - remove unused code
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-allowaccessmodification
-repackageclasses ''

# Remove attributes for smaller size
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Remove unnecessary metadata
-keepattributes *Annotation*,Signature,Exception