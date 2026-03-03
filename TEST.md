# Test Plan: Driver DAS Verification

## Proposed Test Plan
Since this is an Android project and we are in a CLI environment without a connected device or emulator, the automated tests will focus on **Unit Testing** and **Static Verification**.

1. **Unit Testing**:
   - Run existing `MileageHelperTest.kt` to verify calculation logic.
   - *Requirement*: Local Gradle or Kotlinc to execute.
2. **Manifest Verification**:
   - Verify all required permissions for Foreground Service, Location, and Overlay are present.
   - Verify Service declarations in `AndroidManifest.xml`.
3. **Database Schema Verification**:
   - Review `Entities.kt` and `AppDatabase.kt` for correctness.
4. **Code Quality Check**:
   - Verify the `LocationService` logic for correctly handling location updates and calculating mileage.
   - Verify `FloatingService` lifecycle management.
5. **Missing Test Investigation**:
   - Investigate why `ShiftDao` and `LocationDao` tests mentioned in `TODO.md` are not found in `app/src/test/`.

**Please approve this plan or provide specific testing instructions.**
