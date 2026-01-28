# Notification System Analysis & Fixes

## Current Flow

### 1. **Sending Notifications** ✅
- **Admin sends notification** via `SendNotificationScreen.kt`
- Calls `EmployeeViewModel.sendNotification()`
- Writes to Firestore:
  - `notifications_queue` for user notifications (ALL, SINGLE, DISTRICT, STATION)
  - `admin_notifications` for admin-only notifications

### 2. **Cloud Function Processing** ⚠️ **ISSUE FOUND**
- Cloud Function `sendNotification` in `functions/index.js` listens to `notifications_queue`
- **PROBLEM**: Admin notifications go to `admin_notifications` but Cloud Function doesn't listen to it
- **RESULT**: Admin notifications won't trigger FCM push notifications

### 3. **FCM Push Notifications** ✅
- Cloud Function sends FCM push notifications to matching users
- Uses `fcmToken` from `employees` collection
- Filters by target type (SINGLE, DISTRICT, STATION, ADMIN, ALL)

### 4. **Receiving Push Notifications** ✅
- `MyFirebaseMessagingService.onMessageReceived()` receives FCM messages
- Shows system notification with title and body
- Adds action buttons (Approve/Reject) if KGID is present

### 5. **In-App Notification Display** ✅
- `EmployeeViewModel` has real-time Firestore listeners:
  - `updateUserNotificationListener()` - listens to `notifications_queue`
  - `updateAdminNotificationListener()` - listens to `admin_notifications`
- `NotificationsScreen.kt` displays notifications from ViewModel
- Filters notifications based on user's profile (KGID, district, station, admin status)

## Issues Found & Fixed

### ✅ **Fixed Issue 1: Admin Notifications Not Sent via FCM**
- **Problem**: Admin notifications were written to `admin_notifications` collection, but Cloud Function only listens to `notifications_queue`
- **Fix Applied**: Modified `EmployeeViewModel.sendNotification()` to:
  - Write ALL notifications (including ADMIN) to `notifications_queue` for Cloud Function processing
  - Also write ADMIN notifications to `admin_notifications` for in-app display
  - This ensures FCM push notifications are sent for admin notifications

### ✅ **Fixed Issue 2: Notification Channel Creation**
- **Problem**: Notification channel was only created when first notification arrived
- **Fix Applied**: 
  - Created notification channel early in `MainActivity.onCreate()`
  - Added fallback channel creation in `MyFirebaseMessagingService`
  - Standardized channel ID to `pmd_notifications_channel` across both files

### ✅ **Fixed Issue 3: Notification Intent Handling**
- **Problem**: Notification intents weren't being handled when app opened from notification
- **Fix Applied**: 
  - Added `handleNotificationIntent()` in MainActivity
  - Added `onNewIntent()` override to handle notification taps
  - Notification intent now includes `notification_action` and `target_kgid` extras

## Current Status

✅ **All critical issues fixed!**

### Notification Flow Summary:
1. **Sending**: Admin sends → writes to `notifications_queue` → Cloud Function processes → FCM push sent
2. **Receiving**: FCM message received → System notification shown → User taps → App opens
3. **Display**: Real-time Firestore listeners → In-app notification list → Mark as read

### Remaining Enhancements (Optional):
- Navigate to notifications screen when notification is tapped (requires Compose navigation context)
- Add notification badge/indicator in UI
- Add notification sound customization

