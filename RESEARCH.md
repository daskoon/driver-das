# Research Report: Android Delivery Driver Dashboard (Driver DAS)

## 1. Goal Overview
The objective is to build an Android application ("Driver DAS") that:
1.  **Extracts Historical Mileage**: Processes Google Maps Timeline JSON files efficiently.
2.  **Live Mileage Tracking**: A persistent foreground service with a manual "Start Shift" button.
3.  **Floating Dashboard**: A "Draw over other apps" overlay for real-time stats while dashing.
4.  **Automation (Future)**: Leveraging notifications or accessibility services to capture order data.
5.  **Efficiency Metrics**: Real-time "Earnings per Mile" and "Wait Time" calculations.

---

## 2. Technical Findings: Historical Mileage (JSON)
Google Maps Timeline exports are often >100MB.
- **Library**: GSON `JsonReader` (Streaming API).
- **Process**:
    - Stream the file to avoid `OutOfMemoryError`.
    - Filter for `activityType: "IN_VEHICLE"` or `"DRIVING"`.
    - Correlate timestamps with user-provided shift dates.
    - Use `Location.distanceBetween()` for raw coordinate pairs if `distanceMeters` is missing.
- **Risk**: GPS "noise" in historical data can lead to slight overestimation (~5-10%).

---

## 3. Technical Findings: Live Dashboard & Tracking
### Background Execution
- **Foreground Service**: Mandatory for mileage tracking to prevent the OS from killing the process.
- **Permissions**: `FOREGROUND_SERVICE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`.
- **Location Engine**: **Fused Location Provider API** (Google Play Services).
    - *Best Practice*: Use `PRIORITY_HIGH_ACCURACY` with a 5-10 second interval for mileage precision.

### Floating Overlay (Dashboard)
- **API**: `WindowManager` with `LayoutParams.TYPE_APPLICATION_OVERLAY`.
- **Permission**: `android.permission.SYSTEM_ALERT_WINDOW` ("Display over other apps").
- **UI Design**: A small, draggable bubble or a minimized card showing "Current Miles" and "Time Active".

---

## 4. Technical Findings: Automation & Data Capture
### Notification Listening
- **API**: `NotificationListenerService`.
- **Use Case**: Detecting "New Order" or "Payout" alerts from DoorDash/GoPuff.
- **Constraint**: Notifications must contain the data (e.g., "$8.50 for 3 miles") in the text field. If the app only sends a generic "New order available," this will fail.

### Accessibility Service (Alternative)
- **API**: `AccessibilityService`.
- **Use Case**: "Scraping" the screen of the DoorDash app to extract the restaurant name, payout, and distance directly from the UI.
- **Risk**: Higher battery drain and stricter Google Play Store scrutiny. Recommended only if notifications are insufficient.

---

## 5. Local Data Architecture
- **Database**: **Room Persistence Library**.
- **Schema**:
    - `ShiftTable`: `startTime`, `endTime`, `totalMiles`, `platform`, `basePay`, `tips`.
    - `LocationTable`: `lat`, `lng`, `timestamp`, `shiftId` (for breadcrumb mapping).
- **State Management**: **Jetpack Compose** for the UI and **StateFlow/LiveData** for real-time updates from the Service to the Dashboard.

---

## 6. Competitive Analysis & Inspiration
- **Driver Utility Helper (DUH)**: A real Android app that auto-declines/accepts orders using these exact APIs. Confirms technical feasibility.
- **TrickTrack (Open Source)**: Uses Jetpack Compose and automated driving detection.
- **Stride / MileIQ**: Industry standards for mileage tracking; emphasize low battery impact and "Contemporaneous Logs" for IRS compliance.

---
**Research Complete.** I have identified the specific Android APIs, libraries, and design patterns required to build the Driver DAS app.
