# One Tap Network Reset

[![Latest release](https://img.shields.io/github/v/release/R-Dilipkumar/one-tap-network-reset)](https://github.com/R-Dilipkumar/one-tap-network-reset/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)

**Mobile internet stuck? Fix it in one tap — no airplane mode needed.**

Turning airplane mode on and off often makes slow mobile data fast again. This Android app does the same thing with a single tap, but only restarts the **mobile radio**. Wi‑Fi, Bluetooth, airplane mode and your mobile data switch stay as they are.

It also shows a **5G status notification** for each SIM.

The app is called **Net One Tap** on your phone.

## Features

- **One-tap network reset.** Tap the app icon, the Quick Settings tile, or the button in the notification. No screen opens.
- **5G Unlimited notification.** Shows `SIM n: 5G Unlimited activated` when that SIM's *NR State (NSA)* is `CONNECTED` and *NR Frequency* is `HIGH` — the same values as the hidden `*#*#4636#*#*` phone info screen. Other SIMs are hidden.
- **Dual SIM** support.
- **Safe:** skips the reset during a call, and always turns the radio back on.

## Download

Get the APK from **[Releases](https://github.com/R-Dilipkumar/one-tap-network-reset/releases/latest)** and open it on your phone (allow installing from unknown sources).

Tested on POCO X7 Pro (Android 16, HyperOS 3, Airtel 5G NSA).

## Setup

1. Open **Net One Tap** once. Allow *Phone* and *Notifications*. The 5G notification appears.
2. For the real reset, install [Shizuku](https://github.com/RikkaApps/Shizuku) and start it (Wireless debugging). Open Net One Tap again and allow it in Shizuku. Rooted phones can skip Shizuku.
3. Xiaomi / POCO / Redmi: also turn on **USB debugging (Security settings)** in Developer options, or the reset may be blocked.
4. Optional: add the **Net Reset** tile in Quick Settings.

After a phone restart, open Shizuku and tap Start again (not needed with root).

## Why does it need Shizuku or root?

Android does not let normal apps restart the mobile radio. The app uses, in order:

| Way | Needs | Result |
| --- | --- | --- |
| Shizuku | Shizuku started (Wireless debugging, no root) | Real reset |
| Root | Rooted phone (Magisk etc.) | Real reset |
| Neither | — | Soft refresh only (asks Android to re-check the network) |

Full research: **https://r-dilipkumar.github.io/one-tap-network-reset/**

## Good to know

- If the phone hides the SIM list from apps, the notification watches the mobile-data SIM only.
- With two SIMs, usually only the mobile-data SIM shows 5G.
- On 5G SA (standalone) networks, *NR State (NSA)* can stay `NONE`, so the 5G Unlimited line may not show.
- If signal ever does not come back after a reset, toggle airplane mode once or restart the phone.

## Build from source

```bash
./gradlew assembleDebug
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Logs while testing:

```bash
adb logcat -s NetOneTap
```

## License

MIT — see [LICENSE](LICENSE).
