# NFC Toolkit

A Jetpack Compose (Kotlin) Android app for reading, writing and duplicating NFC
tags, with a Google Wallet–style card view for your saved tags and Material 3
light/dark theming.

## Features
- **Read** — UID, tag type (MIFARE Classic/Ultralight, NTAG/Type 2, ISO-DEP,
  FeliCa, ISO 15693…), capability info, and fully parsed NDEF records (text, URL,
  Wi-Fi, vCard, MIME, external, smart poster). Keeps a recent-scan history.
- **Write** — compose Text, URL, Wi-Fi (WPA2), Contact (vCard), Phone, Email or
  App-launch records onto blank/writable tags. Optional "lock after writing".
- **Copy** — duplicate the NDEF content of one tag onto another (two-step flow).
- **Wallet** — saved tags shown as swipeable gradient cards. Pick one to re-write
  to a tag or to share over NFC via Host Card Emulation (phone presents it as an
  NDEF Type 4 tag).
- **Settings** — System/Light/Dark theme, Material You dynamic colour (Android
  12+), haptics toggle, and an honest capabilities note.

## Build & run
1. Open the folder in **Android Studio** (Koala / Ladybug or newer).
2. Let it sync Gradle. If it flags a dependency version, accept its suggestion —
   this is source only, not a prebuilt APK, and the versions below are a known-good
   starting point that Android Studio may nudge upward.
3. Run on a **physical device with NFC** (the emulator has no NFC radio).

### Toolchain assumed
- AGP 8.7.0, Gradle 8.9, Kotlin 2.0.21, JDK 17
- compileSdk/targetSdk 35, minSdk 24
- Compose BOM 2024.09.03, Navigation Compose, DataStore, kotlinx.serialization

## Scope / limitations (important)
This app works with **NDEF data** and NDEF-based HCE sharing only. It deliberately
does **not** clone or emulate access fobs, keycards, transit cards or bank cards:
- Android HCE can't spoof a tag's **UID** or emulate **MIFARE Classic**.
- Phone NFC is 13.56 MHz only — it can't read **125 kHz** (LF) fobs at all.
- Those tasks require dedicated hardware (Proxmark3, Chameleon, Flipper Zero), and
  cloning credentials you don't own is generally illegal.

So "Copy" = NDEF duplication, and "NFC Share" = presenting your own NDEF record.
