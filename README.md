# Eye 20-20-20

A small Android app for the 20-20-20 eye-break rule: focus for 20 minutes, then look
at something about 20 feet away for 20 seconds.

Built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Coroutines/Flow and a
foreground service. Minimum Android 8.0 (API 26); compiled and tested against API 36.

## Build

```bash
./gradlew :app:assembleDebug
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tests

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Unit tests cover the timer state machine (including screen-off grace and break
completion), statistics date math, and settings validation. Instrumented tests cover
DataStore persistence and Room duplicate protection.

## How it behaves

- **Work timer**: counts down from the configured work duration using
  `SystemClock.elapsedRealtime()`, so it does not drift when the app is backgrounded.
- **Break**: when work reaches zero, a pure-black break screen opens with the large
  countdown and the instruction "Look at something approximately 20 feet away."
  There is no skip, cancel, or end button. The screen stays awake by default. A short
  vibration marks the start and end of the break.
- **Completed breaks only**: a break is recorded in the statistics only after its
  countdown reaches zero. Leaving the break screen or turning the screen off does not
  end it.
- **Screen off during work**: work pauses and the remaining time is saved. If the screen
  comes back on within the configured grace period (default 2 minutes), work resumes
  with the same remaining time. After a longer screen-off period, work resets to the
  full duration and shows a notice.
- **Screen off during a break**: the break keeps counting. When it ends, the next work
  session is armed; screen-on grace is applied from that moment.
- **Foreground service and recovery**: starting a timer starts the foreground service
  and its persistent notification. The service is sticky, the countdown is derived from
  `SystemClock.elapsedRealtime()`, and a synchronous snapshot is written at least every
  five seconds. If the process is killed, the next launch or service restart recovers a
  running work timer with the elapsed dead time subtracted. If work ended while the app
  was gone, a break that is still in progress is restored; otherwise the session ends.
- **Auto-restart**: on by default. After a completed break the next work session starts
  automatically. It can be turned off in Settings.
- **Settings**: work duration (1–180 min), break duration (5–900 sec), screen-off grace
  (0–60 min), auto-restart, start/end vibration, keep screen awake, and fullscreen break.
  Invalid input is rejected by the dialog and invalid persisted values fall back to the
  defaults. Duration changes apply to the next session, never to a countdown already
  running.
- **Statistics**: today, the current Monday–Sunday week by day, weekly total, all-time
  total, and a weekly bar chart. The share button renders a 1080×1440 PNG and opens the
  native Android share sheet. There are no goals, streaks, or rewards.

## Permissions

Declared in `AndroidManifest.xml`:

| Permission | Why |
| --- | --- |
| `POST_NOTIFICATIONS` | Shows the ongoing timer notification and break alert (Android 13+). |
| `FOREGROUND_SERVICE` | Keeps the work timer alive while the app is in the background. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required for the special-use foreground service type on Android 14+. |
| `USE_FULL_SCREEN_INTENT` | Lets the break screen appear over the lock screen / other apps when Android allows it. |
| `SYSTEM_ALERT_WINDOW` | Optional. Allows the break screen to open automatically above the app you are using. The timer and notification work without it. |
| `VIBRATE` | Start/end break vibrations. |

A one-time setup screen explains these permissions and links to the relevant Android
settings. On Android 13+ its **Allow notifications** button triggers the runtime
notification request. It can be skipped or completed once; completion is stored locally
so it is not shown again. The Settings screen's **Background & permissions** section
shows the live status of each item and refreshes when the app returns from system
settings.

On Android 14+, full-screen-intent access must be granted manually in system settings.
Battery optimization is not requested automatically; the setup and Settings screens
open the system battery-optimization list so the user can choose unrestricted use. No
restricted battery-optimization permission is declared.

No accessibility service, Device Owner, kiosk mode, or other device-management trick is
used. The break screen blocks the app's own back action, but Android always keeps the
Home button, system gestures, emergency dialer, and power button outside a normal app's
control.

## Limitations

- Android may stop any app after a force-stop or aggressive OEM battery restriction. A
  foreground service and unrestricted battery use make interruption less likely, but no
  app can guarantee 24/7 execution.
- The timer is recovered from a snapshot written at least every five seconds and on
  every state change. If the process died while the screen was off and the screen
  broadcast was missed, the dead period cannot be distinguished from screen-on time;
  recovery is conservative but not exact.
- Exact break-end wakeups during deep Doze are not guaranteed; the countdown is
  corrected from the monotonic clock as soon as the process runs again.
- The app requests the normal "display over other apps" permission for automatic break
  screens but does not draw overlay windows, use exact alarms, or accessibility
  automation. Those features would require additional sensitive permissions and are
  deliberately avoided.
- All data stays on the device. Uninstalling the app removes the settings and history.

## Structure

```
app/src/main/java/com/mohamed/eye2020/
  timer/        TimerEngine (pure state machine), TimerManager (app-scoped owner),
                TimerRecovery, TimerState, Clock, snapshot persistence
  service/      TimerService (foreground notification, screen receiver, break launch)
                NotificationHelper
  data/         Room break records, DataStore settings, first-run marker, repositories
  domain/       Weekly statistics date math
  ui/           Compose screens: setup, home, stats, settings, break; permission snapshot
  util/         time formatting, haptics, permission status, system settings shortcuts
```

The timer has exactly one owner: the application-scoped `TimerManager`. Activities,
the service, and notifications only observe it or send commands, so navigating or
recreating an activity cannot create a second countdown.
