# AI Launcher for Android

An Android launcher replacement with fuzzy app search, folder management, widget hosting, an OpenClaw-style extension system, and an optional Claude AI assistant.

## Features

- **Home Screen** — customizable grid layout, dock, swipe-up gesture to app drawer
- **App Drawer** — real-time fuzzy search (exact match, starts-with, contains, acronym)
- **Folders** — drag apps together, rename, add/remove apps
- **Wallpaper** — static from gallery, live wallpaper picker
- **Widgets** — browse and add Android widgets to home screen
- **Extensions** — plugin system (OpenClaw-style) with lifecycle hooks and AI query hooks
- **AI Assistant** *(optional)* — Claude-powered assistant with launcher context, streamed via SSE

## Architecture

```
Android App (Jetpack Compose)
       │
       ▼
launcher-backend (Express.js)   ← handles auth, themes, extension marketplace
       │
       ▼
launcher-worker (Node.js)       ← runs Claude Code CLI, streams AI responses via SSE
       │
       ▼
  Claude Code CLI               ← your Anthropic subscription, on a GCP e2-micro VM (free tier)
```

The **launcher-worker** is a small Node.js server that wraps the Claude Code CLI. It spawns a Claude process per conversation, injects launcher context (current app, notifications, clipboard), and streams the response back as Server-Sent Events. This means the AI runs against your own Anthropic account — no separate API key billing.

## Install the Launcher (Android App)

### Prerequisites

- Android Studio Hedgehog (2023.1) or later — [download](https://developer.android.com/studio)
- JDK 17 (bundled with Android Studio)
- Android SDK API 26+ (Android 8.0 minimum)
- Android device with USB debugging enabled, or an emulator

### Steps

```bash
git clone https://github.com/tsushanth/ai-launcher.git
cd ai-launcher
```

**In Android Studio:**
1. File → Open → select the `ai-launcher` folder
2. Wait for Gradle sync to complete
3. Connect your Android device via USB
4. Click Run (▶) or press `Shift+F10`
5. Select your device — the app installs as **AI Launcher**

**Or via command line:**

```bash
# Set your Android SDK path
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# Build and install
./gradlew installDebug
```

### Set as Default Launcher

1. Press the **Home button** on your device
2. Select **AI Launcher** from the picker
3. Tap **Always**

To revert: Settings → Apps → Default apps → Home app → choose your previous launcher.

### What You Get (No AI Required)

- Home screen with search bar and dock
- Swipe up → App Drawer with fuzzy search
- Long-press items for folder/settings options
- Gear icon (⚙) → Settings → Manage Extensions (3 built-in: Calculator, Weather, Notes)

## Enable the AI Assistant

The AI features require running the backend and worker separately. See:

**[INSTALLATION.md](INSTALLATION.md)** — full setup for `launcher-backend`, `launcher-worker`, and GCP VM deployment

## Project Status

| Phase | What | Status |
|-------|------|--------|
| 1 | Core launcher (home screen, drawer, folders, wallpaper, widgets) | ✅ Complete |
| 2 | Extension system (SDK, built-in extensions, management UI) | ✅ Complete |
| 4 | AI backend + Claude worker + GCP deployment | 🚧 Backend/worker built, Android overlay pending |
| 3 | Theme engine + AI theme generation | ⏳ Next |
| 5 | Polish, gestures, cloud sync | ⏳ Pending |
| 6 | Beta + Play Store | ⏳ Pending |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Android UI | Jetpack Compose + Material 3 |
| State management | ViewModel + StateFlow |
| Local database | Room |
| Networking | Retrofit + OkHttp + SSE |
| Backend | Express.js |
| Database | Supabase (PostgreSQL) |
| Auth | Firebase Authentication |
| AI runtime | Claude Code CLI (Node.js wrapper) |
| Hosting | GCP e2-micro VM (free tier) |

## Permissions

| Permission | Required | Purpose |
|-----------|----------|---------|
| `HOME` | Yes | Act as default launcher |
| `INTERNET` | Yes | Backend API calls |
| `FOREGROUND_SERVICE` | Yes | AI overlay |
| `PACKAGE_USAGE_STATS` | Optional | Detect current app for AI context |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Optional | Read notifications for AI context |
| `READ_CALENDAR` | Optional | Calendar events for theme suggestions |

Context data is only sent to the backend when the user actively sends an AI message — no background tracking.

## Credits

- Inspired by [OpenClaw](https://github.com/openclaw/openclaw) desktop launcher
- Claude worker pattern from riddle-verse project
- Built with Claude Code
