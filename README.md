# Shopar SDK Demo Apps

Standalone Android and iOS host apps for demonstrating the public Shopar Agency and Discovery SDKs together. This repository is a native integration demo, not an SDK implementation.

Both apps have **Discovery**, **Agency**, and **Settings** tabs. Settings accepts separate Agency and Discovery app IDs and endpoints, plus Discovery placement IDs. No live IDs or credentials are included in this repository.

## SDK source used by this demo

The `vendor/` directories are pinned public SDK submodules. Clone them with:

```sh
git clone --recurse-submodules git@github.com:mobinova/shopar-sdk-demos.git
```

The demo builds against those pinned source revisions until versioned alpha SDK artifacts are published. It does not read a sibling checkout or a private repository.

## Android

Open `android/` in Android Studio or run:

```sh
cd android
./gradlew :app:assembleDebug
```

The app opens Settings when there is no complete Discovery setup. A preconfigured build opens Discovery when it has a valid app ID and selected placement. To prepare one, copy `android/demo.properties.example` to the ignored `android/demo.properties`; copy `ios/ShoparSDKDemo.local.xcconfig.example` to the ignored `ios/ShoparSDKDemo.local.xcconfig`. Fill the production app IDs and Discovery placement IDs there, keep `grpc.shopar.app:443` with TLS enabled, then rebuild and reinstall. Settings lets you choose a separate Shopar or Midnight appearance for Agency and Discovery and save those choices in local profiles. Style changes after SDK initialization are marked pending until relaunch. Discovery never uses the Agency session.

TLS is enabled by default. Only the Android debug manifest permits cleartext for isolated local testing; the release manifest keeps cleartext disabled. Never disable TLS for Agency sessions outside a trusted local test environment.

## iOS

Open `ios/ShoparSDKDemo.xcodeproj` in Xcode and run the `ShoparSDKDemo` scheme on iOS 16 or later. The project consumes the pinned local `vendor/shopar-ios-sdk` Swift Package.

## Configuration lifecycle

App IDs and gRPC host/port/TLS are fixed when each SDK global module initializes. Settings applies these values when the corresponding tab is first opened. Changing them after initialization marks the configuration as pending and requires quitting and reopening the demo app. Discovery placement selection and its optional device-metadata preference can be changed live.

Agency access/refresh credentials remain in the Agency SDK's platform-protected storage and are never shown by the demo. The demo cannot create app properties or placements; create them in Agency Dashboard and App Studio first. SDK errors, empty states, and network failures are displayed by the respective tab.

The demo does not include reward fixtures or a parallel earnings path. For production traffic, use only the test-owned demo properties described in [`docs/test-properties.md`](docs/test-properties.md).

See [`docs/test-properties.md`](docs/test-properties.md) for the four required app properties, their ownership models, and per-platform package IDs. When updating an SDK submodule, Android diagnostics read the pinned Git revision from Gradle; update `INFOPLIST_KEY_ShoparSDKSourceRevision` in the iOS project settings to the new `vendor/shopar-ios-sdk` submodule SHA as part of the same commit.
