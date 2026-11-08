# Demo test properties

The demo app IDs are intentionally blank in the public repository. Create production properties in the Shopar dashboards, then add the returned IDs to the ignored local configuration files. `app_id` is public configuration, not a credential; never put session tokens in demo build configuration.

| Demo app | SDK module | Package/bundle ID | Owner model |
| --- | --- | --- | --- |
| Android demo | Agency | `app.shopar.demos.android` | Register under the test Agency. |
| Android demo | Discovery | `app.shopar.demos.android` | Bind to the internal Shopar user who owns demo earnings. |
| iOS demo | Agency | `app.shopar.sdkdemo.ios` | Register under the test Agency. |
| iOS demo | Discovery | `app.shopar.sdkdemo.ios` | Bind to the same internal Shopar owner used for the Android Discovery demo. |

Agency and Discovery are separate SDK properties even when they share the host package/bundle ID. Android and iOS properties also have distinct `app_id` values. Create at least one Discovery placement for each Discovery property; paste its `placement_id` into Settings. Placement format and content rules are managed in App Studio, not by the demo app.

For production demos use `grpc.shopar.app:443` with TLS. Do not point the demo at Momy's `grpc.momy.app` endpoint. Discovery sends no Momy identity, Agency PWT, or consumer account data. Do not enable optional device metadata unless the test user has made the matching privacy choice.

The apps do not create properties or placements or embed credentials. Agency users sign in with the normal Shopar OTP flow; the SDK keeps their session credentials in platform-protected storage. Each Discovery property must have its beneficiary set in App Studio before test traffic is generated. Add the returned IDs to `android/demo.properties` and `ios/ShoparSDKDemo.local.xcconfig` to prefill the installed apps; both files are git-ignored.
