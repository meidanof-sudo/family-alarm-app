# Family Alarm

An Android app that lets any family member trigger a loud alarm on every other family member's phone — **even if their phone is on Do Not Disturb, silent, or vibrate mode**.

## How It Works

1. **One person creates a family group** and gets a 6-character code
2. **Other family members join** using that code
3. **Press the big red button** to alarm everyone in the group
4. Each receiving phone plays a loud alarm sound, vibrates, and shows a full-screen alert — even on the lock screen

### DND / Silent Mode Bypass

The alarm bypasses Do Not Disturb through multiple mechanisms:

- **`AudioAttributes.USAGE_ALARM`** — The sound plays on the alarm audio stream, which Android allows through DND by default
- **`setBypassDnd(true)`** — The notification channel is configured to bypass DND
- **Alarm stream volume override** — The app temporarily sets the alarm stream to maximum volume
- **High-priority FCM data messages** — Firebase Cloud Messaging data messages with `priority: high` wake the device from Doze mode
- **Foreground service** — The alarm runs as a foreground service so Android won't kill it

## Architecture

```
┌─────────────┐    ┌───────────────┐    ┌──────────────┐
│  Sender App  │───>│   Firestore   │───>│ Cloud Function│
│  (tap alarm) │    │ /alarms/{id}  │    │  (triggered)  │
└─────────────┘    └───────────────┘    └──────┬───────┘
                                               │
                                          FCM (high priority)
                                               │
                    ┌──────────────────────────┐│
                    │  Receiver App             ▼│
                    │  AlarmFCMService          ││
                    │    → AlarmSoundService    ││
                    │    → AlarmActivity        ││
                    └──────────────────────────┘
```

- **Firestore** stores family groups and triggers alarm events
- **Cloud Function** watches for new alarm documents and sends FCM messages
- **FCM data messages** (not notification messages) ensure the app's `onMessageReceived` is always called
- **AlarmSoundService** is a foreground service that plays the alarm using the alarm audio stream

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- A Firebase project
- Node.js 18+ (for deploying Cloud Functions)

### 1. Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
3. Enable **Authentication** → Sign-in method → **Anonymous**
4. Enable **Cloud Firestore** (start in test mode or use the provided `firestore.rules`)
5. Add an **Android app** with package name `com.familyalarm`
6. Download `google-services.json` and place it in the `app/` directory

### 2. Deploy Cloud Functions

```bash
cd cloud-functions
npm install
firebase deploy --only functions
```

### 3. Deploy Firestore Rules

```bash
firebase deploy --only firestore:rules
```

### 4. Build the Android App

1. Open the project in Android Studio
2. Make sure `app/google-services.json` is in place
3. Build and run on a device (FCM requires Google Play Services)

### 5. Grant Permissions

On first launch, the app will prompt for:

- **Notification permission** (Android 13+)
- **Do Not Disturb access** — required so the notification channel can bypass DND

## Project Structure

```
family-alarm-app/
├── app/
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/familyalarm/
│   │   │   ├── FamilyAlarmApp.kt          # Application class, notification channel
│   │   │   ├── data/
│   │   │   │   ├── FamilyRepository.kt    # Firestore operations
│   │   │   │   └── PrefsManager.kt        # Local preferences
│   │   │   ├── service/
│   │   │   │   ├── AlarmFCMService.kt     # Receives FCM messages
│   │   │   │   ├── AlarmSoundService.kt   # Plays alarm (bypasses DND)
│   │   │   │   └── AlarmTrigger.kt        # Sends alarm via Firestore
│   │   │   ├── receiver/
│   │   │   │   └── AlarmDismissReceiver.kt
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt        # Create/join family, send alarm
│   │   │       └── AlarmActivity.kt       # Full-screen alarm display
│   │   └── res/
│   │       ├── layout/
│   │       ├── drawable/
│   │       └── values/
├── cloud-functions/
│   ├── index.js                           # Firestore trigger → FCM sender
│   └── package.json
├── firestore.rules
├── build.gradle.kts
└── settings.gradle.kts
```

## Key Android Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_NOTIFICATION_POLICY` | Override Do Not Disturb |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep alarm playing in background |
| `USE_FULL_SCREEN_INTENT` | Show alarm on lock screen |
| `WAKE_LOCK` | Wake device when alarm arrives |
| `VIBRATE` | Vibrate during alarm |

## Usage

1. Install the app on all family members' phones
2. One person taps **Create Family** and shares the 6-character code
3. Others tap **Join Family** and enter the code
4. Grant DND override permission when prompted
5. Any member can now tap the red **SEND ALARM** button to alarm everyone else
