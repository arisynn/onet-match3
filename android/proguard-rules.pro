# R8 / ProGuard Optimization Rules for Onet Match 3 (libGDX + Kotlin)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# libGDX core & Android backend reflection and JNI natives
-keep class com.badlogic.gdx.** { *; }
-keep interface com.badlogic.gdx.** { *; }
-keep class * implements com.badlogic.gdx.utils.Disposable { *; }
-keep class * implements com.badlogic.gdx.ApplicationListener { *; }
-keep class * implements com.badlogic.gdx.Screen { *; }

# Preserve native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve Game Launcher, screens, domain engines, and models
-keep class com.onetmatch3.game.** { *; }
-keep class game.** { *; }
-keep interface game.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**
-dontwarn java.awt.**
