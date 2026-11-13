# Firebase

The Android and iOS apps use Firebase Crashlytics for crash reports and Firebase App Distribution to send test
builds to testers. Firebase Analytics is not included; the apps collect no analytics events. The Web app does not use
Firebase.

## How it behaves

| Build | Crashlytics |
| --- | --- |
| Android staging and release, iOS Release | On when the Firebase configuration file was present at build time |
| Android debug, iOS Debug | Off; development crashes stay out of the dashboard |
| Any build without the configuration file, including CI and contributor builds | Off; Firebase is not started |

Crashlytics sends crash stack traces, device model, OS version, app version, and a random installation ID. Mention
this in store privacy disclosures and in any privacy policy before a public release.

## Configuration files

The configuration files identify the Firebase project, so they stay out of Git (`.gitignore`). Download them from
**Firebase console → Project settings → Your apps**, or with the Firebase CLI:

```sh
firebase apps:list --project <project-id>
firebase apps:sdkconfig ANDROID <android-app-id> --project <project-id> -o androidApp/google-services.json
firebase apps:sdkconfig IOS <ios-app-id> --project <project-id> -o iosApp/iosApp/GoogleService-Info.plist
```

Register `dev.vladleesi.braindanceapp` for Android and iOS, and `dev.vladleesi.braindanceapp.staging` for Android
staging builds. `google-services.json` must contain both Android apps; the console's download from project settings
includes every Android app in the project.

With `androidApp/google-services.json` present, the Android build generates the Firebase resources and uploads the R8
mapping file to Crashlytics for each staging and release build; without it, the Google Services and Crashlytics
Gradle plugins skip both. After removing the file, run `./gradlew clean`: the build otherwise keeps the Firebase
resources it generated earlier. Xcode copies `GoogleService-Info.plist` into the app when it exists, and archives
upload their dSYM symbol files.

## Android staging builds

Testers get the `staging` build type: optimized like release, with Crashlytics on, installed next to other builds
as **Braindance Staging** (`dev.vladleesi.braindanceapp.staging`, version name suffix `-staging`). It is signed with
a staging key, separate from the future store key; release builds stay unsigned. Add the staging keystore to the
ignored root `local.properties`:

```properties
STAGING_STORE_FILE=/absolute/path/to/staging.jks
STAGING_STORE_PASSWORD=<password>
STAGING_KEY_ALIAS=<alias>
STAGING_KEY_PASSWORD=<password>
```

Set all four or none. Without them, as in CI, staging APKs are unsigned. Keep a backup of the keystore and its
password outside this machine: installed staging builds can only be updated by APKs signed with the same key.

## Send a build to testers

Testers get an email invitation, accept it with their Google account, and install builds through the App Tester
app on Android or the invitation link on iOS. Add testers in **Firebase console → App Distribution → Testers &
Groups**, or with the CLI:

```sh
firebase appdistribution:testers:add --project <project-id> tester@example.com
```

### Android

```sh
./gradlew :androidApp:assembleStaging
firebase appdistribution:distribute androidApp/build/outputs/apk/staging/androidApp-staging.apk \
  --app <android-staging-app-id> --testers "tester@example.com" --release-notes "What to test"
```

Use `--groups <group-alias>` instead of `--testers` for a group created in the console. Staging builds use
`BACKEND_BASE_URL` from `local.properties`, like release builds.

### iOS

iOS test builds need a paid Apple Developer Program membership, because App Distribution installs ad hoc builds:

1. Testers register their devices from the App Distribution invitation; their UDIDs appear in the console under
   **App Distribution → Testers & Groups**.
2. Add those UDIDs to the Apple Developer account and create an ad hoc provisioning profile for
   `dev.vladleesi.braindanceapp`.
3. Archive the Release configuration in Xcode (**Product → Archive**) and export it with
   **Distribute App → Release Testing** (ad hoc) as an `.ipa`.
4. Distribute it:

   ```sh
   firebase appdistribution:distribute <path-to-ipa> --app <ios-app-id> --testers "tester@example.com"
   ```

Devices added later need a new profile and a new build.

## Limitations

- On iOS, an uncaught Kotlin exception from `shared` is reported as a native crash (`SIGABRT`). The Kotlin message
  is not attached, and Kotlin frames are symbolicated only when the shared framework's dSYM is uploaded.
- Mapping and dSYM uploads need network access during staging and release builds and archives.
