# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI Launcher is a comprehensive Android launcher replacement with AI capabilities, an extension system, and AI-powered features. It's built in three main parts:

1. **Android App** (`app/`) - Jetpack Compose + Material 3 launcher interface
2. **Backend** (`launcher-backend/`) - Express.js API server (Supabase integration)
3. **Worker** (`launcher-worker/`) - Node.js worker for Claude CLI integration (runs on GCP VM)

Current status: Phase 1 ✅ (Core launcher), Phase 2 ✅ (Extensions), Phase 4 🚧 (AI Backend/Worker)

## Build & Development Commands

### Android App

```bash
# Build debug APK
./gradlew assembleDebug

# Install and run on device
./gradlew installDebug

# Clean and rebuild
./gradlew clean build

# Run tests
./gradlew test

# Run a specific test
./gradlew test --tests "com.launcher.path.ClassName"

# Android Studio (recommended)
# Click Run (▶️) or Shift + F10
# Select device/emulator

# View logs
adb logcat | grep "com.launcher"

# Clear app data
adb shell pm clear com.launcher.ai
```

### Backend (Express.js)

```bash
cd launcher-backend

# Install dependencies
npm install

# Development server (auto-reload with nodemon)
npm run dev

# Production server
npm start

# Server runs on http://localhost:3000
```

### Worker (Claude CLI integration)

```bash
cd launcher-worker

# Install dependencies
npm install

# Development server (auto-reload)
npm run dev

# Production server
npm start
```

## Architecture Overview

### Android App Architecture

**UI Layer (Jetpack Compose)**
- `HomeScreenActivity` - Main launcher activity with HOME intent filter
- `AppDrawerActivity` - App drawer with search
- `LauncherSettingsActivity` - Settings UI
- `ui/HomeScreen.kt` - Home screen grid, dock, wallpaper
- `ui/AppDrawer.kt` - App list with fuzzy search
- `ui/ExtensionsScreen.kt` - Extension management UI
- `ui/FolderView.kt` - Folder creation/management
- `ui/theme/` - Material 3 theming

**State Management (ViewModel + StateFlow)**
- `viewmodels/HomeScreenViewModel.kt` - Home screen state & actions
- `viewmodels/AppDrawerViewModel.kt` - App list & search logic

**Data Layer**
- `data/LauncherDatabase.kt` - Room database with DAOs
- `data/LauncherRepository.kt` - Data access abstraction
- `data/models/HomeScreenLayout.kt` - Layout model (grid items, folders, widgets)
- `data/models/AppInfo.kt` - App metadata with search scoring

**Extension System**
- `extensions/LauncherExtension.kt` - Extension interface
- `extensions/ExtensionLoader.kt` - Load extensions from registry
- `extensions/ExtensionManager.kt` - Manage installed extensions
- Built-in extensions: Weather, Calculator, Notes (packaged in `resources/extensions/`)

**Utilities**
- `utils/WallpaperManager.kt` - Static & live wallpaper integration
- `utils/WidgetHost.kt` - AppWidgetHost for widget hosting

**Key Dependencies**
- Jetpack Compose + Material 3 (UI)
- Room (database)
- DataStore (preferences)
- Retrofit + OkHttp (networking + SSE)
- Kotlin Coroutines (async)
- Coil (image loading)
- Firebase (auth, analytics, optional)
- Kotlin Scripting (for script-based extensions)

### Backend Architecture

Express.js server with endpoints:
- `GET /health` - Health check
- `POST /api/launcher/chat` - AI chat (proxies to worker, streams responses via SSE)
- `GET /api/launcher/themes` - Get available themes
- `POST /api/launcher/themes/generate` - Generate theme via AI
- `GET /api/launcher/extensions` - List extensions

**Flow**: Android App → Backend → Worker (Claude CLI)

### Worker Architecture

Runs on GCP VM (e2-micro). Receives requests from backend and:
1. Executes Claude CLI in isolated sessions
2. Streams responses back to backend via HTTP
3. Manages context (current app, clipboard, calendar, etc.)

See `launcher-worker/deploy/` for GCP deployment scripts.

## Database Schema

### Key Entities (Room)

**HomeScreenLayout** - Represents home screen grid state
- `id` - Unique identifier
- `gridWidth`, `gridHeight` - Grid dimensions
- `items` - List of desktop items (apps, folders, widgets)
- `timestamp` - Last modified

**DesktopItem** - Individual item on home screen
- `id`, `x`, `y` - Position
- `itemType` - "app", "folder", or "widget"
- `appPackage` - App package name
- `folderId` - References folder if folder type

**Folder** - App folder
- `id`, `name` - Folder metadata
- `appPackages` - List of app package names

**AppInfo** - Installed app metadata (cached)
- `packageName`, `label`, `icon` - App info
- `searchScore` - Calculated for search algorithm

See `data/LauncherDatabase.kt` for complete schema.

## Key Concepts

### Search Algorithm (AppInfo)
- **Exact match**: "Gmail" finds Gmail with highest score
- **Starts with**: "chr" finds Chrome
- **Contains**: "tube" finds YouTube
- **Acronym**: "gm" finds Gmail
- **Package**: "google" finds Google apps

Implementation in `data/models/AppInfo.kt:calculateSearchScore()`

### Extension System
1. Extensions implement `LauncherExtension` interface
2. Loaded from registry (JSON) at runtime
3. Can be Kotlin script-based or native code
4. UI composables integrated into `ExtensionsScreen`

Built-in extensions in `app/src/main/res/extensions/`:
- weather.json
- calculator.json
- notes.json

### Material 3 Theming
- Dynamic colors from Material You (Android 12+)
- Light/dark mode support
- Theme files in `ui/theme/` and `res/values/themes.xml`

## Common Development Tasks

### Adding a New Screen
1. Create `Activity` class (e.g., `NewActivity.kt`)
2. Create composable UI in `ui/NewScreen.kt`
3. Create ViewModel if needed (`viewmodels/NewViewModel.kt`)
4. Add intent filter to `AndroidManifest.xml`
5. Add navigation from existing screens

### Adding a Database Entity
1. Create data class with `@Entity` annotation in `data/models/`
2. Create DAO interface in `data/LauncherDatabase.kt`
3. Create repository methods in `LauncherRepository.kt`
4. Use in ViewModel via repository

### Adding a Gradle Dependency
1. Edit `app/build.gradle.kts`
2. Add to `dependencies {}` block
3. Sync Gradle: `Ctrl+Shift+O` or File → Sync
4. For specific version: use version variable (e.g., `val roomVersion = "2.6.1"`)

### Testing Locally on Device

1. **Build and Run**: Click Run (▶️) in Android Studio or `./gradlew installDebug`
2. **Set as Default Launcher**: Press Home → Select "AI Launcher" → "Always"
3. **View Logs**: `adb logcat | grep "com.launcher"`
4. **Use Layout Inspector**: Tools → Layout Inspector (debug UI hierarchy)
5. **Use Database Inspector**: View → Tool Windows → App Inspection (inspect Room DB)
6. **Use Profiler**: View → Tool Windows → Profiler (monitor memory, CPU)

## File Structure Cheat Sheet

```
app/
├── src/main/
│   ├── java/com/launcher/
│   │   ├── *Activity.kt          # Activities (entry points)
│   │   ├── ui/                   # Composable screens
│   │   ├── ui/viewmodels/        # ViewModels
│   │   ├── ui/theme/             # Material 3 theme
│   │   ├── data/                 # Database & models
│   │   ├── extensions/           # Extension system
│   │   └── utils/                # Utilities (wallpaper, widgets)
│   ├── res/
│   │   ├── values/               # Strings, colors, themes
│   │   └── extensions/           # Built-in extensions (JSON)
│   └── AndroidManifest.xml       # App config & permissions
└── build.gradle.kts              # Dependencies & build config

launcher-backend/
├── server.js                      # Express server
├── package.json                   # Dependencies
└── .env                           # Configuration

launcher-worker/
├── server.js                      # Worker server
├── package.json                   # Dependencies
├── .env                           # Configuration
└── deploy/                        # GCP deployment scripts
```

## Testing

### Unit Tests
Run: `./gradlew test`
- Search algorithm: `AppInfo.calculateSearchScore()`
- Theme color validation
- Extension loader

### Instrumented Tests (Android)
Run: `./gradlew connectedAndroidTest`
- Home screen layout persistence
- App drawer search
- Database operations

### Manual Testing Checklist
See `TESTING.md` for comprehensive testing guide with:
- Installation & setup
- Home screen functionality
- App drawer & search
- Folders & wallpaper
- Settings
- Database persistence
- Performance targets
- Known issues

## Gradle Configuration

**Key Settings** (from `app/build.gradle.kts`):
- `compileSdk = 34` (Android 14)
- `minSdk = 26` (Android 8.0)
- `targetSdk = 34` (Android 14)
- Kotlin 1.9.10
- Compose BOM 2024.02.00
- Room 2.6.1

**Build Types**:
- **debug**: Unminified, debuggable, ~15-20 MB APK
- **release**: Minified with ProGuard, optimized, ~8-10 MB APK

## Important Notes

### Permissions
Required:
- `INTERNET` - API calls
- `HOME` - Default launcher
- `FOREGROUND_SERVICE` - AI overlay (future)

Optional (for AI context):
- `PACKAGE_USAGE_STATS` - Detect current app
- `READ_CALENDAR` - Calendar events
- `READ_CONTACTS` - Contact context (future)

See `AndroidManifest.xml` for complete list.

### AI Integration Flow (Not yet fully integrated)
1. User sends message in AI overlay
2. Android app collects context (current app, clipboard, notifications)
3. Sends to backend (`/api/launcher/chat`)
4. Backend proxies to worker
5. Worker executes Claude CLI with context
6. Response streamed back via SSE
7. UI updates in real-time

### Backend Configuration
Set environment variables in `launcher-backend/.env`:
- `PORT` - Server port (default 3000)
- `SUPABASE_URL` - Supabase project URL
- `SUPABASE_ANON_KEY` - Public anon key
- `WORKER_URL` - Worker server URL

### Worker Configuration
Set environment variables in `launcher-worker/.env`:
- `PORT` - Server port (default 3001)
- `CLAUDE_API_KEY` - For Claude CLI (if using direct API)

## Debugging Tips

- **App crashes**: Check `adb logcat | grep "AndroidRuntime"`
- **Gradle sync fails**: File → Sync with Gradle Files, check SDK/JDK
- **Missing icons**: Check Material Icons Extended library version
- **Database issues**: Use App Inspection to query Room directly
- **Layout problems**: Use Layout Inspector or Compose Preview
- **Memory leaks**: Use Android Profiler → Memory tab

## Known Issues & TODOs

### Phase 1 Integrations
- [ ] Grid-based drag & drop for home screen items
- [ ] Long-press context menus (partially done)
- [ ] Widget rendering (placeholder only)

### Phase 3 (Theme System)
- [ ] AI theme generation via Claude
- [ ] Theme marketplace
- [ ] Seasonal theme suggestions

### Phase 5 (Polish)
- [ ] Pinch gesture for settings
- [ ] Swipe down for notifications
- [ ] App shortcuts (deep linking)
- [ ] Cloud sync (Supabase)
- [ ] Analytics (Firebase)

See `BUGS.md` for reported issues and `PROGRESS.md` for detailed phase breakdown.
