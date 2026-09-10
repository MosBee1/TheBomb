# The Bomb — Privacy Policy

**The Bomb collects no data. It transmits no data. It has no ability to do
either.**

## The short version

- No analytics, no telemetry, no crash reporting, no ads, no tracking.
- The app does not request the `INTERNET` permission. It is technically
  incapable of network communication. You can verify this: inspect the merged
  manifest in any build — no internet permission appears, and
  `gradle/libs.versions.toml` contains no Firebase, Google Play Services, or
  analytics artifacts whose transitive consumers could add one.
- All data (your triage decisions, settings, statistics) lives in a local
  Room database inside the app's private storage. Uninstalling the app
  deletes it.

## What the app touches, and why

| Data / permission | Purpose | Leaves the device? |
|---|---|---|
| Screenshot list, names, timestamps, sizes (`READ_MEDIA_IMAGES`) | To detect, list, and delete screenshots | Never |
| Thumbnails (`ContentResolver.loadThumbnail`) | Shown in-app and in notifications | Never |
| Your triage actions (Defuse/Archive/Detonate/fuses) | Core functionality, stored locally | Never |
| Notifications (`POST_NOTIFICATIONS`) | Popup and reminders you configure | Never |
| Aggregates (counts, bytes cleared) | The Stats screen, computed by local SQL | Never |

Deletion of screenshots happens via Android's own MediaStore APIs, including
Android's system consent dialog. Those system dialogs are part of the OS, not
this app.

## Third parties

None. No SDKs, no servers, no accounts, no payments, no ads.

## Changes

Any future version that changes this policy will bump the app version and
update this file before release. The MIT-licensed source lets you audit every
claim here.

## Contact

Open an issue at https://github.com/MosBee1/TheBomb/issues
