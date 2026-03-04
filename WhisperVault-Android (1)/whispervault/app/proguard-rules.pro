# GhostChat ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Retrofit models
-keep class com.ghostchat.data.remote.** { *; }

# Keep Room entities
-keep class com.ghostchat.data.local.** { *; }

# Keep Socket.IO
-keep class io.socket.** { *; }
-keep class org.json.** { *; }

# Keep crypto classes
-keep class com.ghostchat.security.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
