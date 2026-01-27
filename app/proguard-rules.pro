# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep data classes
-keepclassmembers class com.brickgame.tetris.game.** { *; }
-keepclassmembers class com.brickgame.tetris.ui.theme.** { *; }
