# Demo test properties

The demo app IDs are intentionally blank in the repository. Create real properties in the Shopar dashboards in an approved staging environment, then paste the returned IDs into each app's Settings tab. `app_id` is public configuration, not a credential, but use test-owned properties so demo traffic and earnings are easy to identify.

| Demo app | SDK module | Package/bundle ID | Owner model |
| --- | --- | --- | --- |
| Android demo | Agency | `app.shopar.demos.android` | Register under the test Agency. |
| Android demo | Discovery | `app.shopar.demos.android` | Bind to the internal Shopar user who owns demo earnings. |
| iOS demo | Agency | `app.shopar.sdkdemo.ios` | Register under the test Agency. |
| iOS demo | Discovery | `app.shopar.sdkdemo.ios` | Bind to the same internal Shopar owner used for the Android Discovery demo. |

Agency and Discovery are separate SDK properties even when they share the host package/bundle ID. Android and iOS properties also have distinct `app_id` values. Create at least one Discovery placement for each Discovery property; paste its `placement_id` into Settings. Placement format and content rules are managed in App Studio, not by the demo app.

Use `grpc.shopar.app` only when the approved environment routes demo requests there. Otherwise enter the staging Shopar gRPC host, port, and TLS setting. Do not point the demo at Momy's `grpc.momy.app` endpoint. Discovery sends no Momy identity, Agency PWT, or consumer account data. Do not enable optional device metadata unless the test user has made the matching privacy choice.

The apps do not create properties or placements, store production IDs, or embed credentials. Agency test users can sign in with the normal Shopar OTP flow; the SDK keeps their session credentials in platform-protected storage. A Discovery test property must have its beneficiary set in App Studio before test traffic is generated.
