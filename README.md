# NFC Toolkit

An Android app for reading, writing and copying NFC tags, built with Kotlin and Jetpack Compose.

## Features

- **Read:** UID, tag type (MIFARE Classic/Ultralight, ISO-DEP, FeliCa, ISO 15693), capacity, parsed NDEF records, scan history
- **Write:** Text, URL, Wi-Fi (WPA2), Contact (vCard), Phone, Email and App-launch records, with an optional lock-after-write toggle
- **Copy:** duplicate NDEF content from one tag to another in a two-step source/target flow
- **Wallet:** saved tags as swipeable gradient cards, with an active card shared over NFC via Host Card Emulation
- **Settings:** System/Light/Dark theme, Material You dynamic colour, haptics toggle

## Limitations

Copy and emulation cover **NDEF content only**. Stock Android can't clone or emulate access fobs, keycards or MIFARE Classic cards: HCE can't spoof a tag's UID, and phone NFC (13.56 MHz) can't read 125 kHz fobs. That needs dedicated hardware such as a Proxmark3 or Flipper Zero.

## Tech

- Kotlin 2.0.21, Jetpack Compose, Material 3
- DataStore for saved tags and settings
- HCE service implementing the NFC Forum Type 4 Tag protocol
- Custom Wi-Fi Simple Configuration (WSC) builder/parser
- AGP 8.7.0, compileSdk 35

## Build

1. Open the project root (the folder containing `settings.gradle.kts`) in Android Studio
2. Let Gradle sync finish
3. Run on a physical device with NFC. Emulators don't support it.

## License

MIT. See [LICENSE](LICENSE).
