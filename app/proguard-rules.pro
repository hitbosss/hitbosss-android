# R8 keep rules for the release build.
# Goal: obfuscate/shrink the app (Play optimization) without breaking JSON (kotlinx.serialization
# over Retrofit), Media3 GL effects, or reflection-based libs. Most libs (Retrofit, OkHttp, Hilt,
# Firebase, Media3, CameraX, Compose) ship their own consumer rules; the rules below cover the app.

# ---- Attributes needed by serialization / generics / annotations ----
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature,EnclosingMethod,Exceptions

# ---- kotlinx.serialization ----
# The runtime bundles consumer rules; these keep the generated serializers + Companions of our types.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Belt-and-suspenders: keep the JSON model classes + members intact so wire keys never break.
# These packages are a tiny fraction of the app, so obfuscation coverage stays high.
-keep,includedescriptorclasses class com.hitbosss.data.remote.dto.** { *; }
-keepclassmembers class com.hitbosss.domain.model.** { *; }

# Serialized enums: keep values()/valueOf used by the generated serializers.
-keepclassmembers enum com.hitbosss.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Media3 custom GL effect (used directly; keep to be safe against effect wiring) ----
-keep class com.hitbosss.presentation.feature.hit.GlitchEffect { *; }

# ---- Quiet known-safe warnings from transitive libs ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn org.jetbrains.annotations.**
