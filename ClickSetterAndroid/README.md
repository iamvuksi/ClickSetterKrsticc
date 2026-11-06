# Click Setter for Android

An Android version of the Krstic click timer. It lets you capture a screen point, preview the saved point, set a repeat delay in seconds, and use a floating countdown with a Stop button while another app is open.

## Build

Open this folder (`ClickSetterAndroid`) in Android Studio and let Gradle sync. Choose **Build > Build APK(s)** to create an installable debug APK. You can also run `gradlew.bat assembleDebug` from this folder after installing the Android SDK.

This workspace does not have Android Studio or the Android SDK installed, so I could not build an APK here. A Gradle wrapper is included; Android Studio or the Android SDK is needed to produce the APK.

## First use on your phone

1. Install and open Click Setter.
2. Tap **Set Point**. If prompted, allow **Display over other apps**. Tap the exact screen location; the app returns and displays its X/Y coordinates.
3. Use **Preview Point** to briefly see a crosshair at the saved location.
4. Set the repeat interval in seconds.
5. In Android Settings, enable **Click Setter** under **Accessibility**. The Accessibility Service is what sends the screen tap gestures.
6. Return to Click Setter and tap **Start**. The floating countdown panel appears over other apps; use its **STOP** button to end the timer.

Android requires both the overlay permission for the point picker/countdown and the user-enabled Accessibility Service for gesture dispatch. The service uses Android's documented `dispatchGesture` capability. [Android AccessibilityService documentation](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)

Minimum Android version: Android 8.0 (API 26).

