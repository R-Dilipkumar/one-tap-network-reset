# Net One Tap

Small Android app (Kotlin, Android 10+):

1. **One tap = mobile network reset.** Restarts only the cellular radio of each SIM, like airplane mode does, but Wi‑Fi, Bluetooth, airplane mode and the mobile data switch are not touched. No screen opens.
2. **5G Unlimited notification.** Shows `SIM n: 5G Unlimited activated` when that SIM's *NR State (NSA)* is `CONNECTED` and *NR Frequency* is `HIGH` (same values as the `*#*#4636#*#*` screen). Other SIMs are hidden.

Research and details: **https://r-dilipkumar.github.io/Network_One_Tap/**

## How the reset gets permission

A normal app cannot restart the radio. The app uses, in order:

| Way | Needs | Result |
| --- | --- | --- |
| [Shizuku](https://github.com/RikkaApps/Shizuku) | Shizuku started (Wireless debugging, no root) | Real reset |
| Root | Rooted phone (Magisk etc.) | Real reset |
| Neither | — | Soft refresh only (asks Android to re-check the network) |

## Build

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Install and set up

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

1. Tap **Net One Tap** once. Allow *Phone* and *Notifications*.
2. Install Shizuku and start it (Wireless debugging). Tap Net One Tap again and allow it in Shizuku.
3. Optional: add the **Net Reset** tile in Quick Settings.

Ways to reset: app icon, Quick Settings tile, or the **Reset network** button on the notification.

Logs while testing:

```bash
adb logcat -s NetOneTap
```

If signal ever does not come back after a reset, toggle airplane mode once or restart the phone.
