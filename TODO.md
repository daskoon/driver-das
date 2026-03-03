# TODO: Driver DAS Implementation Task List

## Phase 1: Core Foundation & Permissions [android]
- [x] Initial Project Scaffold (Gradle, settings, build files)
- [x] Configure permissions in `AndroidManifest.xml`
- [x] Implement Permission Request Logic in `MainActivity.kt`
  - [x] Request `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`
  - [x] Handle `ACCESS_BACKGROUND_LOCATION` (Android 10+)
  - [x] Request `POST_NOTIFICATIONS` (Android 13+)
  - [x] Request `SYSTEM_ALERT_WINDOW` (Overlay permission)

## Phase 2: Foreground Location Service [service]
- [x] Create `LocationService.kt`
  - [x] Set up Notification Channel for Foreground Service
  - [x] Initialize `FusedLocationProviderClient`
  - [x] Configure `LocationRequest` (High accuracy, 5-10s interval)
- [x] Implementation logic:
  - [x] Location callback to receive updates
  - [x] Calculate cumulative distance using `Location.distanceTo()`
  - [x] Broadcast/StateFlow current mileage for UI binding
- [x] Register Service in Manifest with `foregroundServiceType="location"`

## Phase 3: Local Persistence (Room DB) [database]
- [x] Set up Room Entities:
  - [x] `ShiftEntity`: (id, startTime, endTime, totalMiles, earnings, platform)
  - [x] `LocationPointEntity`: (id, shiftId, lat, lng, timestamp)
- [x] Create DAOs and AppDatabase:
  - [x] `ShiftDao` (insert, update, delete, getAll)
  - [x] `LocationPointDao` (bulk insert for breadcrumbs)
- [x] Implement `Repository` to handle data flow between Service and DB (can be parallelized)

## Phase 4: Floating Dashboard Overlay [ui] [service]
- [x] Create `FloatingService.kt`:
  - [x] Implement `WindowManager` view inflation
  - [x] Compose-based overlay view
  - [x] Draggable logic (Touch listener)
- [x] Create UI components:
  - [x] Minimized "Bubble" view
  - [x] Expanded "Card" with real-time stats
- [x] Data binding: Connect `FloatingService` to `LocationService`'s state flow

## Phase 5: Main Dashboard UI [ui]
- [x] Create `DashboardViewModel.kt`:
  - [x] Handle "Start/Stop" shift state
  - [x] Format mileage and efficiency data
- [x] Implement Compose Screen:
  - [x] Large "Shift Toggle" button
  - [x] Live summary cards (Miles, Earnings, $/mi)
  - [x] History list (Past shifts from Room)

## Phase 6: Advanced Data Capture [automation]
- [x] Create `NotificationListenerService.kt`:
  - [x] Filter for target app packages (DoorDash, Gopuff)
  - [x] Implement regex/text parsing for earnings data
- [x] Integrate with `ShiftRepository` to auto-populate earnings

## Phase 7: Testing & Polishing [test]
- [x] Unit Tests:
  - [x] Distance calculation helper functions
  - [x] Database DAO operations
- [x] Integration Tests:
  - [x] Service start/stop lifecycle
  - [x] Permission denial handling
- [x] Performance: Optimize battery drain for GPS updates
