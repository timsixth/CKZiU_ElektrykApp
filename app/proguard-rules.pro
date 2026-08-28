-dontobfuscate

-keep class com.example.planlekcji.ckziu_elektryk.client.** { *; }

# Strip android.util.Log invocations in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}