# Pocket Clone ProGuard Rules

# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Gson
-keep class com.pocketclone.app.data.api.** { *; }

# Keep Room entities
-keep class com.pocketclone.app.data.db.** { *; }
