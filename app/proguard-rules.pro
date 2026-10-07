-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * { @kotlinx.serialization.Serializable <fields>; }

# MediaPipe (Hands beta): native code calls back into these classes
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
