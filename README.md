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

The app initially opens Settings. App IDs and gRPC hosts are blank by default. Enter test app IDs, placement IDs, and an approved staging endpoint from Shopar; the demo will not send requests until those values validate. Discovery never uses the Agency session.

TLS is enabled by default. Only the Android debug manifest permits cleartext for isolated local testing; the release manifest keeps cleartext disabled. Never disable TLS for Agency sessions outside a trusted local test environment.

## iOS

Open `ios/ShoparSDKDemo.xcodeproj` in Xcode and run the `ShoparSDKDemo` scheme on iOS 16 or later. The project consumes the pinned local `vendor/shopar-ios-sdk` Swift Package.

## Configuration lifecycle

App IDs and gRPC host/port/TLS are fixed when each SDK global module initializes. Settings applies these values when the corresponding tab is first opened. Changing them after initialization marks the configuration as pending and requires quitting and reopening the demo app. Discovery placement selection and its optional device-metadata preference can be changed live.

Agency access/refresh credentials remain in the Agency SDK's platform-protected storage and are never shown by the demo. The demo cannot create app properties or placements; create them in Agency Dashboard and App Studio first. SDK errors, empty states, and network failures are displayed by the respective tab.

The demo does not include reward fixtures or a parallel earnings path. For live traffic, use an approved Shopar staging configuration and test-owned app properties.

See [`docs/test-properties.md`](docs/test-properties.md) for the four required app properties, their ownership models, and per-platform package IDs. When updating an SDK submodule, Android diagnostics read the pinned Git revision from Gradle; update `INFOPLIST_KEY_ShoparSDKSourceRevision` in the iOS project settings to the new `vendor/shopar-ios-sdk` submodule SHA as part of the same commit.
