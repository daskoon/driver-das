# Plan: Driver DAS - Android Delivery Driver Dashboard

## 1. Project Overview
The goal is to build a modern Android application that tracks mileage in real-time using a foreground service and provides a floating dashboard overlay for delivery drivers to monitor their earnings and efficiency.

## 2. Architecture & Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (Modern, declarative UI)
- **Background Task**: Foreground Service (For persistent location tracking)
- **Location Engine**: Fused Location Provider API (Google Play Services)
- **Local Database**: Room Persistence Library
- **Architecture Pattern**: MVVM (Model-View-ViewModel)

## 3. Step-by-Step Implementation Plan

### Phase 1: Core Foundation & Permissions [ALREADY STARTED]
- [x] Scaffold project with Gradle and basic directory structure.
- [x] Configure `AndroidManifest.xml` with required permissions (`FINE_LOCATION`, `BACKGROUND_LOCATION`, `FOREGROUND_SERVICE`, `SYSTEM_ALERT_WINDOW`).
- [ ] Implement a permission request handler in `MainActivity` to ensure all high-level permissions are granted before starting.

### Phase 2: Foreground Location Service (The "Engine")
- [ ] Create `LocationService.kt`:
    - [ ] Initialize `FusedLocationProviderClient`.
    - [ ] Create a persistent notification (Required for foreground services).
    - [ ] Implement location update callbacks (5-10 second intervals).
    - [ ] Logic to calculate distance (mileage) between coordinates using `Location.distanceTo()`.
- [ ] Define `ServiceState`: A singleton or Flow to share real-time mileage and status with the UI.

### Phase 3: Local Persistence (Room DB)
- [ ] Define `Shift` Entity: `startTime`, `endTime`, `totalMiles`, `platform`, `earnings`.
- [ ] Define `LocationPoint` Entity: `lat`, `lng`, `timestamp`, `shiftId`.
- [ ] Create `AppDatabase` and DAOs for saving shifts and breadcrumb points.
- [ ] Integrate database writes into `LocationService`.

### Phase 4: Floating Dashboard UI (Overlay)
- [ ] Create `FloatingService.kt`:
    - [ ] Use `WindowManager` to inflate a Compose-based overlay.
    - [ ] Implement "Draw over other apps" logic.
    - [ ] Create a minimized "bubble" and an expanded "card" view.
    - [ ] Bind real-time data from `LocationService` to the overlay.

### Phase 5: Main Dashboard UI
- [ ] Create a modern Compose dashboard:
    - [ ] Large "Start/Stop Shift" button.
    - [ ] Summary cards: "Current Shift Miles", "Earnings", "Efficiency ($/mi)".
    - [ ] History view: List of previous shifts retrieved from Room.

### Phase 6: Advanced Data Capture (Automation)
- [ ] Implement `NotificationListenerService`:
    - [ ] Filter for DoorDash/Gopuff notification keywords.
    - [ ] Parse text for payout and mileage values.
    - [ ] Automatically update the active shift's earnings data.

### Phase 7: Testing & Polishing
- [ ] Unit tests for mileage calculation logic.
- [ ] Battery impact testing (Optimizing location intervals).
- [ ] UI/UX refinements (Themes, animations).

## 4. Testing Strategy
- **Manual Testing**: Real-world driving tests to compare app mileage against vehicle odometer.
- **Log Analysis**: Use `Logcat` to verify location callbacks and database transactions.
- **Permission Flow**: Exhaustive testing of the "Grant Permissions" onboarding.
