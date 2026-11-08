# Shopar SDK Demo Hosts

- This repository contains host applications only; SDK implementation stays in the Android and iOS SDK repositories.
- Never commit live `app_id`/`placement_id` values, credentials, PWTs, refresh tokens, private user data, or raw merchant URLs.
- Keep Agency and Discovery settings, sessions, and activity separate. Discovery never attaches Agency auth.
- Agency and Discovery SDK configuration is immutable after each global SDK has initialized. Settings must show when an app relaunch is required to apply startup configuration changes.
- Do not add a feature flag, kill switch, custom credential encryption, or demo-only reward behavior.
- Placement format, eligibility, product assignment, displayed offer, and attribution come from Shopar's backend. The demo may select a registered placement ID; it must not override the response.
- Use real IDs entered by the operator; blank or placeholder IDs must leave the module unconfigured and issue no requests.
