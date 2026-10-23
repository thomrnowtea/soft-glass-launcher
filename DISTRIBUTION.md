# Building and signing

The Android application ID is `com.softglass.launcher`; Java classes retain the upstream `com.gathin.tvhome` namespace. Android 8+ is required. Build with JDK17 and Android SDK34; Gradle8.9 is provided by the wrapper.

## Debug

```sh
./gradlew :app:assembleDebug
```

This creates a locally signed debug APK without needing the publisher's private release key. Use `gradlew.bat` on Windows. Debug builds cannot update an official release signed with a different certificate.

## Release

Create your own Android signing key and copy `signing.properties.example` to a private location. Replace its placeholders with your keystore path, alias and passwords. Never commit that file or the keystore.

```sh
./gradlew :app:assembleRelease -PsoftglassSigningProperties=/absolute/path/to/signing.properties
```

In PowerShell, quote the complete `-P...` argument. Output: `app/build/outputs/apk/release/app-release.apk`. Missing release signing fails the release task; it does not silently sign an official release with a debug key. Keep your signing key for future updates and increment versionCode for published Android updates.

The optional `launcherApplicationId` property supports separate identities for custom installations. Changing it creates a separate app with separate private data and permissions; it is not an account migration. Provider authorities and internal actions follow the chosen ID.

## Privacy and platform limits

Accounts, API credentials, WiZ identities/aliases and preferences belong to the installation's private storage. Android backup is disabled. The APK includes no publisher credentials.

Wi-Fi SSID access and Spotify media-session access are opt-in Android permissions. Optional startup uses a separate accessibility service with capability0: no screen-content retrieval, gestures or key interception. It observes discovered system Home packages. The upstream key-remapping services are separate, opt-in services and can request remote-key filtering.

Inputs and system actions use Android capability discovery and vendor fallbacks. Some TV firmware restricts default Home, notification access or background startup. The launcher does not automatically disable the stock Home, change audio routes, debloat apps or grant permissions. Compatibility with untested devices is not certified.
