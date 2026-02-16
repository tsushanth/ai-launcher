# AI Launcher for Android

A comprehensive Android launcher replacement with AI capabilities, OpenClaw-style extension system, and AI-powered theme generation.

## Project Structure

```
ai-launcher/
├── app/                                    # Android application
│   ├── src/main/
│   │   ├── AndroidManifest.xml             # Launcher manifest with HOME intent
│   │   ├── java/com/launcher/
│   │   │   ├── HomeScreenActivity.kt       # Main launcher activity ✅
│   │   │   ├── AppDrawerActivity.kt        # App drawer with search ✅
│   │   │   ├── LauncherSettingsActivity.kt # Settings ✅
│   │   │   ├── ui/
│   │   │   │   ├── HomeScreen.kt           # Home screen UI ✅
│   │   │   │   ├── AppDrawer.kt            # App drawer UI ✅
│   │   │   │   ├── theme/                  # Material 3 theming ✅
│   │   │   │   └── viewmodels/             # ViewModels ✅
│   │   │   ├── data/
│   │   │   │   └── models/
│   │   │   │       ├── AppInfo.kt          # App data model ✅
│   │   │   │       └── HomeScreenLayout.kt # Layout models ✅
│   │   │   ├── extensions/                 # Extension system (TODO)
│   │   │   ├── themes/                     # Theme engine (TODO)
│   │   │   ├── ai/                         # AI integration (TODO)
│   │   │   └── utils/                      # Utilities
│   │   └── res/
│   │       ├── values/
│   │       │   ├── strings.xml             # String resources ✅
│   │       │   ├── colors.xml              # Color scheme ✅
│   │       │   └── themes.xml              # App themes ✅
│   │       └── xml/                        # Service configs
│   └── build.gradle.kts                    # App dependencies ✅
├── launcher-backend/                       # Express.js backend (TODO)
├── launcher-worker/                        # Claude worker (TODO)
├── build.gradle.kts                        # Project build config ✅
├── settings.gradle.kts                     # Project settings ✅
└── gradle.properties                       # Gradle properties ✅
```

## What's Been Built (Phase 1 - Core Launcher)

### ✅ Completed

1. **Project Setup**
   - Jetpack Compose + Material 3
   - Android project structure
   - Gradle build configuration
   - Theme system (Material You)

2. **Home Screen**
   - Launcher activity with HOME intent filter
   - Search bar (opens app drawer)
   - Dock with 5 app slots
   - Swipe up gesture to open app drawer
   - Settings button

3. **App Drawer**
   - Alphabetical app list
   - Real-time fuzzy search
   - Search score algorithm (exact match, starts with, contains, acronym, package name)
   - Pull from installed apps
   - Filter system/user apps

4. **Settings Activity**
   - Basic settings UI
   - Placeholder for grid size, icon pack, AI, extensions

5. **Data Models & Database**
   - `AppInfo` - App metadata with search scoring
   - `HomeScreenLayout` - Grid layout, desktop items (apps, folders, widgets)
   - Room database implementation (LauncherDatabase, HomeScreenDao)
   - LauncherRepository for data operations
   - Type converters for complex data types

6. **Folder Management** ✅
   - FolderIcon with preview grid
   - FolderDialog for viewing/managing contents
   - CreateFolderDialog for new folders
   - Rename folders
   - Add/remove apps from folders
   - Repository methods for folder operations

7. **Wallpaper Support** ✅
   - LauncherWallpaperManager utility
   - Static wallpaper from gallery
   - Live wallpaper picker integration
   - WallpaperPickerDialog UI
   - Clear/reset to default wallpaper

8. **Widget Hosting** ✅
   - LauncherWidgetHost with AppWidgetHost
   - Widget allocation and lifecycle
   - WidgetPickerDialog to browse widgets
   - Widget configuration support
   - Size calculation utilities

9. **AndroidManifest**
   - HOME intent filter (can be set as default launcher)
   - Permissions: INTERNET, FOREGROUND_SERVICE, PACKAGE_USAGE_STATS, etc.
   - Queries declaration for launcher apps

### 🚧 TODO (Phase 1 Integration)

10. **Home Screen Integration**
   - Integrate folder icons into home screen grid
   - Add widget views to home screen
   - Long-press menu for items (edit, remove, add to folder)
   - Drag and drop for rearranging
   - Persist layout to database

11. **Testing & Polish**
   - Test as default launcher on device
   - Performance optimization
   - Fix edge cases

## How to Build & Run

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK (API 26+)
- Physical Android device or emulator

### Steps

1. **Clone the repository**
   ```bash
   cd ~/Documents/GitHub/ai-launcher
   ```

2. **Open in Android Studio**
   - File → Open → Select `ai-launcher` folder
   - Wait for Gradle sync

3. **Add Firebase (Optional for now)**
   - Download `google-services.json` from Firebase Console
   - Place in `app/` directory

4. **Build and Run**
   - Click Run (▶️) or `Shift + F10`
   - Select device/emulator
   - App will install as "AI Launcher"

5. **Set as Default Launcher**
   - Press Home button on device
   - Select "AI Launcher"
   - Choose "Always" or "Just once"

## Current Features

### Home Screen
- **Search Bar**: Tap to open app drawer with search
- **Dock**: Shows 5 favorite apps (currently first 5 alphabetically)
- **Gestures**: Swipe up anywhere to open app drawer
- **Settings**: Top-right gear icon

### App Drawer
- **Search**: Real-time fuzzy search as you type
- **Scoring**: Smart app ranking (exact match > starts with > contains > acronym)
- **List**: Alphabetically sorted apps
- **System Apps**: Tagged with "System app" label

### Settings
- Grid size (placeholder)
- Icon pack (placeholder)
- AI Assistant toggle (placeholder)
- Extensions marketplace (placeholder)
- Version info

## Next Steps

### Immediate (Complete Phase 1)
1. Implement folder creation and management
2. Add static wallpaper support
3. Implement basic widget hosting
4. Test setting as default launcher on various devices

### Phase 2: Extension System (Weeks 5-6)
- Extension SDK (`LauncherExtension` interface)
- Kotlin script engine for runtime extension loading
- Extension marketplace backend
- Sample extensions (weather, calculator, notes)

### Phase 3: Theme System (Weeks 7-8)
- Theme engine (colors, icon packs, wallpapers)
- AI theme generation via Claude
- Theme suggestions based on occasions (birthday, holidays, etc.)
- Material You dynamic colors

### Phase 4: AI Integration (Weeks 9-11)
- Launcher worker (Express.js + Claude CLI on GCP VM)
- Backend routes (launcher, themes, extensions, sync)
- Context collector (current app, notifications, clipboard, calendar)
- AI overlay (floating orb)
- SSE streaming for real-time responses

### Phase 5: Polish (Weeks 12-14)
- Gestures (pinch for settings, swipe down for notifications)
- App shortcuts (long-press menu)
- Widget resizing
- Cloud sync (Supabase)
- Analytics (Firebase)
- Performance optimization

### Phase 6: Beta & Launch (Weeks 15-16)
- Beta testing
- Privacy policy
- Play Store submission

## Architecture

### Android App
- **UI**: Jetpack Compose + Material 3
- **State**: ViewModel + StateFlow (MVI pattern)
- **Database**: Room (for layouts, settings, themes)
- **Preferences**: DataStore
- **Networking**: Retrofit + OkHttp + SSE

### Backend (Not yet built)
- **Framework**: Express.js
- **Database**: Supabase (PostgreSQL)
- **Auth**: Firebase Authentication
- **Storage**: Supabase Storage (themes, extensions)

### Worker (Not yet built)
- **Runtime**: Node.js on GCP e2-micro VM
- **AI**: Claude Code CLI
- **Streaming**: SSE (Server-Sent Events)
- **Pattern**: Copied from riddle-verse/game-worker

## Dependencies

Key dependencies (see `app/build.gradle.kts`):
- Jetpack Compose (UI)
- Material 3 (Design system)
- Room (Database)
- DataStore (Preferences)
- Firebase (Auth, Analytics, Crashlytics)
- Retrofit + OkHttp (Networking + SSE)
- Coil (Image loading)
- Kotlin Coroutines (Async)
- Kotlin Scripting (Extensions)

## Testing

### Unit Tests
- `AppInfo.calculateSearchScore()` - Search algorithm
- Theme color validation
- Extension loader

### Integration Tests
- Home screen layout persistence
- App drawer search
- Settings persistence

### E2E Tests
- Set as default launcher
- Launch app from home screen
- Launch app from app drawer
- Create folder
- Apply theme

## Performance Targets
- App drawer scroll: 60 FPS
- App search: < 100ms response time
- Memory usage: < 150MB baseline
- Battery drain: < 2% per day

## Privacy & Permissions

### Required Permissions
- `INTERNET` - API calls to backend
- `FOREGROUND_SERVICE` - AI overlay
- `HOME` - Default launcher

### Optional Permissions (for AI context)
- `PACKAGE_USAGE_STATS` - Current app detection
- `BIND_NOTIFICATION_LISTENER_SERVICE` - Notification access
- `READ_CALENDAR` - Calendar events
- `ACCESS_FINE_LOCATION` - Location context
- `BIND_ACCESSIBILITY_SERVICE` - Screen text extraction

All context data is sent to backend ONLY when user sends AI message. No continuous tracking.

## Contributing

This is a personal project following the OpenClaw model. Extension system will allow community contributions via the marketplace.

## License

TBD

## Credits

- Inspired by OpenClaw desktop launcher
- Architecture patterns from riddle-verse project
- Built with Claude Code

---

**Current Status**: Phase 1 in progress (70% complete)

**Next Milestone**: Complete Phase 1 (folders, wallpaper, widgets) → Ready for testing as basic launcher
