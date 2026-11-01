# BLE Packet Broadcaster & Spam Detector

An Android application built with **Kotlin** and **Jetpack Compose (Material 3)** for broadcasting custom Bluetooth Low Energy advertisement payloads and detecting nearby BLE proximity spam in real time.

## Status
Work in Progress — Active Development

## Architecture & Key Features
- **MVVM Architecture with Kotlin Coroutines & StateFlow:** Reactive state management via `MainViewModel` across three Compose screens: `AdvertiseScreen`, `ScannerScreen`, and `SettingsScreen`.
- **BLE Advertiser Foreground Service (`BleAdvertiserService`):**
  - Broadcasts multi-vendor BLE proximity payloads including Apple Continuity, Google Fast Pair, Samsung Easy Setup, and Microsoft Swift Pair.
  - Supports both modern `AdvertisingSet` API with Scan Response data and legacy `BluetoothLeAdvertiser` fallback.
  - Configurable transmission power levels and custom advertisement intervals in milliseconds.
- **Defensive BLE Spam Scanner Service (`BleScannerService`):**
  - Low-latency BLE scanning via `BluetoothLeScanner` to detect and classify nearby anomalous BLE advertisements and Flipper Zero signatures.
  - Parses raw manufacturer-specific byte arrays for Apple `0x004C`, Samsung `0x0075`, and Microsoft `0x0006` packets.
  - Tracks detected devices with MAC address, packet classification, RSSI signal strength, and timestamp.

## Project Structure
- `src/main/java/com/example/ble/MainActivity.kt` — Jetpack Compose navigation and runtime BLE permission handling for Android 12+ and legacy SDKs
- `src/main/java/com/example/ble/service/BleAdvertiserService.kt` — Foreground service managing BLE packet broadcasting and coroutine cycling
- `src/main/java/com/example/ble/service/BleScannerService.kt` — Foreground service for real-time BLE packet inspection and spam detection
- `src/main/java/com/example/ble/viewmodel/MainViewModel.kt` — StateFlow management for payload selection, scanner results, and transmission settings
- `src/main/java/com/example/ble/data/` — Structured BLE payload definitions and manufacturer byte arrays
- `src/main/java/com/example/ble/ui/` — Material 3 Compose screens for Advertising, Scanning, and Settings

## Tech Stack
- **Language:** Kotlin, Coroutines, StateFlow
- **UI Framework:** Jetpack Compose, Material 3
- **Android APIs:** `BluetoothLeAdvertiser`, `AdvertisingSet`, `BluetoothLeScanner`, Foreground Services
