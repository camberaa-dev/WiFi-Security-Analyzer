# WiFi Security Analyzer — v1.0.0

Android native starter project built with Kotlin and Jetpack Compose.

## Open and build
1. Extract this ZIP.
2. Open the `WiFiSecurityAnalyzer` folder in Android Studio.
3. Allow Gradle sync to complete (JDK 17).
4. Connect an Android device with WiFi enabled.
5. Run the `app` configuration.

To build an APK: Android Studio > Build > Build Bundle(s) / APK(s) > Build APK(s).

## Current implementation
- Dark Compose UI with Dashboard, Scanner, Audit and Tools sections.
- WiFi scan results through Android WifiManager.
- Passive classification from advertised capabilities.
- Basic risk score and explanatory findings.
- No Firebase or remote account requirement.

## Android limitations
- WiFi scan results are rate-limited by Android and may be cached.
- ACCESS_FINE_LOCATION permission is required by Android for scan results on supported versions; device Location services may also need to be enabled.
- SSID/BSSID visibility can be restricted by Android, device vendor, and OS version.
- WPS state, router firmware, and password strength cannot be reliably established from passive scan data.
- The displayed score is an initial heuristic, not a certification or proof that a network is secure.
- This project does not crack WiFi passwords or attack networks.

## Next development steps
- Add connection details via ConnectivityManager/WifiInfo with API-aware redaction handling.
- Add Room persistence and PDF report generation.
- Add local password-strength estimation for owner-supplied passwords without attempting network authentication.
- Add latency and connectivity diagnostics.
