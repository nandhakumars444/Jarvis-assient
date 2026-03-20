# JARVIS Android – Complete Setup Guide

## What you're building
A native Android AI voice assistant with:
- 🎙 Always-on "Hey Jarvis" wake word (foreground service)
- 🤖 Claude AI for natural language understanding
- 📱 Open any app by voice
- 🔍 Web search via voice
- 🔔 Read your notifications
- 🔊 Volume & media control
- ⏰ Set alarms & reminders
- 💧 Health dashboard (hydration, steps, sleep)
- 🎨 Iron Man arc reactor UI with live animations

---

## Quickest Way: Android Studio (Recommended)

### Step 1 – Install Android Studio
Download from: https://developer.android.com/studio
- During setup, let it install the Android SDK automatically

### Step 2 – Open the project
1. Open Android Studio
2. Click **"Open"** → select the `jarvis-android` folder
3. Wait for Gradle sync (downloads dependencies, ~3 min first time)

### Step 3 – Add fonts (required)
Download these two free fonts from Google Fonts:
- **Orbitron**: https://fonts.google.com/specimen/Orbitron → Download family
- **Share Tech Mono**: https://fonts.google.com/specimen/Share+Tech+Mono → Download family

Place the `.ttf` files into:
```
app/src/main/res/font/
  orbitron.ttf             ← rename from Orbitron-VariableFont_wght.ttf
  share_tech_mono.ttf      ← rename from ShareTechMono-Regular.ttf
```

### Step 4 – Set your Anthropic API key
Get your key from: https://console.anthropic.com

You can set it two ways:
- **In-app**: Launch JARVIS → tap Settings (gear icon) → paste key → Save
- **Before build**: Edit `local.properties` and add:
  ```
  ANTHROPIC_API_KEY=sk-ant-your-key-here
  ```

### Step 5 – Build & install
**Option A – Run on physical phone (best):**
1. Enable **Developer Options** on your Android phone:
   Settings → About Phone → tap "Build Number" 7 times
2. Enable **USB Debugging** in Developer Options
3. Connect phone via USB
4. In Android Studio: click ▶ Run (green play button)
5. Select your device → JARVIS installs and launches!

**Option B – Build APK file:**
1. In Android Studio: **Build → Build Bundle(s)/APK(s) → Build APK(s)**
2. APK saved to: `app/build/outputs/apk/debug/app-debug.apk`
3. Transfer to phone, tap to install
   (You need to allow "Install from unknown sources" in phone settings)

---

## Command-line build (if you have JDK 17 + Android SDK)

```bash
chmod +x build.sh
./build.sh
```

Or manually:
```bash
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

Install via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Permissions to grant on first launch

JARVIS will ask for these – tap **Allow** for full functionality:

| Permission | Used for |
|---|---|
| Microphone | Wake word + voice commands |
| Notifications | Post-notification permission (Android 13+) |

After launch, also grant these manually:

| Permission | How to grant |
|---|---|
| **Notification access** | Settings → tap "GRANT NOTIFICATION ACCESS" → enable JARVIS |
| **Battery optimization** | Settings → Battery → Don't optimize JARVIS (keeps wake word alive) |
| **Autostart** (MIUI/Samsung) | Allow JARVIS to autostart in phone's battery settings |

---

## Project structure

```
jarvis-android/
├── app/src/main/
│   ├── java/com/jarvis/assistant/
│   │   ├── JarvisApp.kt              ← App class, notification channels
│   │   ├── ui/
│   │   │   ├── MainActivity.kt       ← Main screen, broadcast receiver
│   │   │   ├── HealthActivity.kt     ← Health dashboard
│   │   │   ├── SettingsActivity.kt   ← API key, preferences
│   │   │   ├── ChatAdapter.kt        ← Chat RecyclerView
│   │   │   ├── ArcReactorView.kt     ← Custom animated arc reactor
│   │   │   └── WaveformView.kt       ← Animated voice waveform
│   │   ├── service/
│   │   │   ├── WakeWordService.kt    ← Always-on foreground service
│   │   │   ├── CommandRouter.kt      ← Routes commands to actions
│   │   │   ├── NotificationListenerService.kt
│   │   │   ├── HydrationReminderReceiver.kt
│   │   │   └── BootReceiver.kt       ← Restart after reboot
│   │   ├── viewmodel/
│   │   │   ├── MainViewModel.kt
│   │   │   └── HealthViewModel.kt
│   │   └── util/
│   │       ├── ClaudeClient.kt       ← Anthropic API calls
│   │       ├── JarvisTTS.kt          ← Text-to-speech
│   │       └── JarvisPrefs.kt        ← SharedPreferences
│   ├── res/
│   │   ├── layout/                   ← All XML layouts
│   │   ├── drawable/                 ← Arc ring, bubbles, icons
│   │   ├── values/                   ← Colors, strings, themes
│   │   └── font/                     ← Add fonts here
│   └── AndroidManifest.xml
├── build.sh                          ← One-command build script
└── SETUP.md                          ← This file
```

---

## Voice commands to try

```
"Hey Jarvis, open Spotify"
"Hey Jarvis, search for weather today"
"Hey Jarvis, set volume to 50"
"Hey Jarvis, play next song"
"Hey Jarvis, set alarm for 7 AM"
"Hey Jarvis, read my notifications"
"Hey Jarvis, how much water have I drunk today?"
"Hey Jarvis, what time is it?"
"Hey Jarvis, open Chrome"
"Hey Jarvis, remind me to take medicine"
"Hey Jarvis, mute"
"Hey Jarvis, tell me a joke"
"Hey Jarvis, what's my health score?"
```

---

## Troubleshooting

**Wake word stops working after a while**
→ Go to Settings → Battery → find JARVIS → set to "Unrestricted" or "No restriction"
→ On Samsung: disable "Adaptive battery" for JARVIS
→ On MIUI/Xiaomi: allow Autostart for JARVIS

**"Speech recognition not available"**
→ Make sure Google app is installed and updated
→ Go to Settings → Apps → Google → Enable

**Gradle sync fails**
→ Check your internet connection (needs to download ~200 MB dependencies)
→ In Android Studio: File → Invalidate Caches → Restart

**APK installs but crashes**
→ Check that you granted Microphone permission
→ Check logcat in Android Studio for the error

**Voice not understood**
→ Speak clearly at normal pace
→ Ensure you have internet (Google STT requires connection)
→ Try offline speech recognition: Settings → Voice Input → Download offline language
