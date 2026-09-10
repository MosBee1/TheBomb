# The Bomb

Every screenshot lands armed. The Bomb watches for new screenshots in real
time, pops up the moment one lands, and detonates what you don't defuse — on
your schedule, on your device.

**Free, open source (MIT), no ads, no tracking, no network access.**

| | |
|---|---|
| Language | Kotlin, 100% Jetpack Compose |
| Min SDK | 29 (Android 10) · Target/Compile SDK 34 (Android 14) |
| Storage | Room (local only) · Background: WorkManager + a foreground watcher |
| License | MIT — see [LICENSE](LICENSE) |
| Privacy | [PRIVACY.md](PRIVACY.md) — zero data collection, zero network |

## Features

- **Instant triage popup** — the moment a screenshot is taken: a notification
  with inline thumbnail and Defuse / Archive / Detonate actions, plus a
  translucent popup (full-screen intent on locked/off screens; direct launch
  with the optional "display over other apps" grant while unlocked).
- **Flexible fuse timers** — four quick presets (each fully configurable:
  amount + unit, minutes to weeks) and a custom slider with a live
  "Detonates in X" label. Live countdown chips on every timed row; cut any
  fuse before it fires. Deletions go through Android's official scoped-storage
  consent flow (`MediaStore.createDeleteRequest` /
  `RecoverableSecurityException`), with a notification fallback when you're
  not looking.
- **Daily blast** — pick a time (default 11:30 PM); archived, non-kept,
  un-fused screenshots are detonated then. Optional 30-minute reminder. The
  Home screen shows a live "Next blast in…" banner.
- **Defused** — a permanent safe zone: pinned screenshots are exempt from
  every fuse and the daily blast.
- **Stats** — animated counters plus a fallout report: processed, detonated,
  defused, storage cleared.
- **Search & filter** — by filename and date range.
- **Material You** — dynamic color on Android 12+, a hand-designed
  teal/amber/slate palette on Android 10–11, full light/dark support,
  serif/sans typography pairing, purposeful motion, celebration on all-clear.

## Permissions, and why (also in PRIVACY.md)

| Permission | Why | Required? |
|---|---|---|
| `READ_MEDIA_IMAGES` (or `READ_EXTERNAL_STORAGE` ≤ API 32) | See, thumbnail, and delete screenshots — the core function | Yes |
| `POST_NOTIFICATIONS` | Screenshot popup/actions, blast reminders, consent fallbacks | Optional |
| `USE_FULL_SCREEN_INTENT` | Popup over the lock screen (incoming-call mechanism); on Android 14 granted via system settings | Optional |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Keeps the MediaStore watcher alive in real time | When armed |
| `RECEIVE_BOOT_COMPLETED` | Restart the watcher after reboot (when armed) | When armed |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional exemption so fuses fire punctually through Doze | Optional |
| `SYSTEM_ALERT_WINDOW` | Optional: instant popup while the phone is unlocked | Optional |

Deliberately **not** requested: `INTERNET`, `CAMERA`, location, contacts,
`MANAGE_EXTERNAL_STORAGE`, `MANAGE_MEDIA`.

## Build

### Prerequisites
- JDK 17
- Android SDK with platform 34 (Android Studio Ladybug or any recent version,
  or command-line tools)
- A local Gradle ≥ 8.4 for the one-time wrapper bootstrap (or use Android
  Studio's bundled Gradle)

### Android Studio
1. **File → Open** this repository. The Gradle wrapper is already committed
   (`gradlew`, `gradle/wrapper/gradle-wrapper.jar`) — let Gradle sync and
   build.
2. Or: **File → New Project → No Activity** to regenerate the wrapper
   yourself, then copy the project files over.

### Command line
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
cat > LICENSE <<'EOF'
MIT License

Copyright (c) 2024 MosBee

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
