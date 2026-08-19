# RgaPro release hardening.
# Keep Android components discovered through the manifest.
-keep public class com.rgapro1.ocaso.SecureMainActivity { *; }
-keep public class com.rgapro1.ocaso.ExpiryNotificationReceiver { *; }
-keep class com.rgapro1.ocaso.** extends android.app.Activity { *; }
-keep class com.rgapro1.ocaso.** extends androidx.fragment.app.FragmentActivity { *; }

# Keep ML Kit entry points and Room generated implementations as required by their libraries.
# Library consumer rules are supplied by the dependencies themselves; do not disable shrinking globally.

# Never log sensitive local data in release builds.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
