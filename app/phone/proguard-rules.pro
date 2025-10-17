# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep class names of all classes for easy debugging (and fix navigation route checking)
-keepnames class dev.jdtech.jellyfin.** { *; }

# These classes are from okhttp and are not used in Android
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# JUPnP library rules for DLNA support
-keep class org.jupnp.** { *; }
-keep interface org.jupnp.** { *; }
-keepclassmembers class org.jupnp.** { *; }
-dontwarn org.jupnp.**
-dontwarn org.osgi.**
-dontwarn javax.enterprise.**

# Keep Android UPnP Service
-keep class org.jupnp.android.** { *; }
-keepclassmembers class org.jupnp.android.** { *; }

# Keep our custom DLNA classes
-keep class dev.jdtech.jellyfin.dlna.** { *; }
-keepclassmembers class dev.jdtech.jellyfin.dlna.** { *; }

# Jetty rules for DLNA HTTP server
-keep class org.eclipse.jetty.** { *; }
-keepclassmembers class org.eclipse.jetty.** { *; }
-dontwarn org.eclipse.jetty.**
-dontwarn javax.servlet.**
