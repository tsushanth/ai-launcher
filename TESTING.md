# AI Launcher - Testing Guide

## Pre-Testing Setup

### Requirements
- Android Studio Hedgehog or later
- Physical Android device OR emulator (API 26+)
- USB debugging enabled (for physical device)
- ~500MB free space on device

### Build Steps

1. **Open Project**
   ```bash
   cd ~/Documents/GitHub/ai-launcher
   # Open in Android Studio
   ```

2. **Sync Gradle**
   - Wait for Gradle sync to complete
   - Fix any dependency issues

3. **Build APK**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - OR run directly: `Shift + F10`

4. **Install on Device**
   - Connect device via USB
   - Click Run (▶️)
   - Select your device

---

## Testing Checklist

### ✅ Phase 1: Installation & Setup

- [ ] **Install APK**
  - App installs successfully
  - No crash on first launch
  - Permissions screen appears (if any)

- [ ] **Set as Default Launcher**
  - Press Home button
  - "AI Launcher" appears in chooser
  - Select "Always"
  - Launcher becomes default
  - Press Home again → goes to AI Launcher

- [ ] **Permissions**
  - App requests necessary permissions
  - Works without optional permissions
  - Settings → Permissions shows correct states

---

### ✅ Phase 2: Home Screen

- [ ] **Basic Display**
  - Home screen loads without crash
  - Search bar visible at top
  - Dock visible at bottom
  - Settings icon visible (top right)

- [ ] **Dock Functionality**
  - Dock shows 5 apps
  - Apps have correct icons
  - Apps have correct names (if showLabel)
  - Tapping app launches it correctly
  - Apps open in reasonable time (< 1s)

- [ ] **Search Bar**
  - Tapping search bar opens app drawer
  - No crash when opening drawer
  - Animation smooth

- [ ] **Gestures**
  - Swipe up anywhere opens app drawer
  - Swipe down does NOT close launcher (should do nothing)
  - Settings button opens settings

- [ ] **Empty Home Screen**
  - If no desktop items, shows empty space
  - No crash with empty layout
  - Long-press on empty space works

---

### ✅ Phase 3: App Drawer

- [ ] **Display**
  - All installed apps appear
  - Apps sorted alphabetically
  - App icons load correctly
  - App names display correctly
  - System apps show "System app" label

- [ ] **Search**
  - Keyboard appears automatically
  - Typing filters apps in real-time
  - Search is case-insensitive
  - Clear button (X) appears when typing
  - Clear button clears search

- [ ] **Fuzzy Search Tests**
  - Exact match: "Gmail" finds Gmail (top result)
  - Starts with: "Chr" finds Chrome
  - Contains: "tube" finds YouTube
  - Acronym: "gm" finds Gmail
  - Package: "google" finds Google apps

- [ ] **App Launch**
  - Tapping app launches it
  - App drawer closes after launch
  - Launched app works normally
  - Return to launcher works (Home button)

- [ ] **Back Navigation**
  - Back button closes app drawer
  - Returns to home screen
  - No crash on back

---

### ✅ Phase 4: Long-Press Menus

- [ ] **Long-Press on Dock App**
  - Menu appears with "App Options"
  - "Remove" option visible
  - "App Info" option visible
  - Tapping Remove removes from dock (TODO: implement)
  - Tapping App Info opens app settings (TODO: implement)
  - Close button dismisses menu

- [ ] **Long-Press on Home Screen App**
  - Menu appears with "App Options"
  - "Remove" option works
  - App removed from home screen
  - Database updated

- [ ] **Long-Press on Folder**
  - Menu appears with "Folder: [name]"
  - "Delete Folder" option visible
  - Deleting folder works
  - Apps inside folder NOT deleted (just folder)

- [ ] **Long-Press on Empty Space**
  - Menu appears with "Home Screen"
  - "Change Wallpaper" option visible
  - "Add Widget" option visible
  - Tapping Change Wallpaper opens picker
  - Tapping Add Widget shows "TODO" (for now)

---

### ✅ Phase 5: Folders

**Note**: Folders need to be created via database currently. UI for creating folders from app drawer coming in Phase 2.

- [ ] **Display**
  - Folder icon shows on home screen
  - Folder shows 2x2 grid preview of apps
  - Folder name displays below icon
  - "Unnamed" shown for folders without name

- [ ] **Open Folder**
  - Tapping folder opens FolderDialog
  - Dialog shows all apps in folder
  - Apps in grid layout (4 columns)
  - Folder name shown in header

- [ ] **Launch from Folder**
  - Tapping app in folder launches it
  - App launches correctly
  - Folder closes after launch

- [ ] **Rename Folder**
  - Tapping Edit icon enables rename
  - Text field appears
  - Typing changes name
  - Tapping Done/Close saves name
  - Name updates in database
  - Name updates on home screen icon

- [ ] **Remove App from Folder**
  - Long-press app in folder (if implemented)
  - OR use delete mechanism
  - App removed from folder
  - If folder has < 2 apps, folder deleted
  - Folder disappears from home screen

---

### ✅ Phase 6: Wallpaper

- [ ] **Open Wallpaper Picker**
  - Long-press empty space
  - Select "Change Wallpaper"
  - Wallpaper picker dialog opens
  - Shows 3-4 options

- [ ] **Choose from Gallery**
  - Tap "Choose from Gallery"
  - Photo picker opens
  - Select an image
  - Image sets as wallpaper
  - Wallpaper visible on home screen
  - Success message shown

- [ ] **Live Wallpaper**
  - Tap "Live Wallpaper" (if device supports)
  - Live wallpaper picker opens
  - Select a live wallpaper
  - Configure if needed
  - Live wallpaper applies
  - Success message shown

- [ ] **Default Wallpaper**
  - Tap "Default Wallpaper"
  - Wallpaper resets to system default
  - Success message shown

- [ ] **AI-Generated (Placeholder)**
  - Tap "AI-Generated"
  - Shows "Coming soon" message
  - Dialog stays open (can try other options)

---

### ✅ Phase 7: Settings

- [ ] **Open Settings**
  - Tap settings icon (top right)
  - Settings activity opens
  - No crash

- [ ] **Settings Display**
  - "Launcher Settings" title shown
  - Back button visible
  - Sections: General, AI Assistant, Extensions, About
  - Cards display correctly

- [ ] **Settings Items**
  - Grid Size shows "4 x 5 (Coming soon)"
  - Icon Pack shows "Default (Coming soon)"
  - AI Assistant toggle present (disabled)
  - Extension Marketplace shows "Coming soon"
  - Version shows "1.0.0 (Alpha)"

- [ ] **Back Navigation**
  - Back button returns to home screen
  - Hardware back button works
  - No crash

---

### ✅ Phase 8: Database & Persistence

- [ ] **Dock Persistence**
  - Close launcher (swipe away from recents)
  - Reopen launcher
  - Dock apps same as before
  - Order preserved

- [ ] **Layout Persistence**
  - Add items to home screen (via database)
  - Close launcher
  - Reopen launcher
  - Items still present
  - Positions preserved (TODO: test when grid implemented)

- [ ] **Folder Persistence**
  - Create folder
  - Add apps to folder
  - Rename folder
  - Close launcher
  - Reopen launcher
  - Folder still present
  - Apps still in folder
  - Name preserved

- [ ] **Wallpaper Persistence**
  - Set custom wallpaper
  - Close launcher
  - Reopen launcher
  - Wallpaper still set

---

### ✅ Phase 9: Performance

- [ ] **App Launch Speed**
  - Apps launch in < 1 second
  - No noticeable lag
  - Smooth animations

- [ ] **Scrolling**
  - App drawer scrolls at 60 FPS
  - No jank or stuttering
  - Smooth scroll with fast swipe

- [ ] **Search Performance**
  - Search updates in < 100ms
  - No lag while typing
  - Results update smoothly

- [ ] **Memory Usage**
  - Launcher uses < 150MB RAM
  - No memory leaks (use Android Profiler)
  - Doesn't get killed by system

- [ ] **Battery Drain**
  - No significant battery drain
  - No wakelocks
  - Background services behave correctly

---

### ✅ Phase 10: Edge Cases

- [ ] **Empty States**
  - No apps installed (unlikely) - shows empty drawer
  - No dock apps - dock empty or shows placeholder
  - No desktop items - empty home screen
  - Empty folder - gets deleted

- [ ] **Uninstall App**
  - App in dock - gets removed from dock
  - App on home screen - gets removed
  - App in folder - gets removed from folder
  - Folder becomes empty - folder deleted

- [ ] **Reinstall App**
  - Apps reappear in drawer
  - Drawer updates automatically
  - No duplicates

- [ ] **Orientation Change**
  - Rotate device
  - Launcher survives rotation
  - Layout preserved
  - No crash

- [ ] **Low Memory**
  - Put launcher in background
  - Open many apps
  - Return to launcher
  - Launcher survives (or gracefully restarts)

---

## Known Issues (Pre-Testing)

### Expected Issues

1. **Missing Icons**
   - Some Material Icons might be missing
   - Fix: Add missing icons or use alternatives

2. **Database Initialization**
   - First run might have empty dock
   - Fix: Initialize with default apps

3. **Widget Rendering**
   - Widgets show as placeholders
   - Fix: Proper AppWidgetHostView implementation (Phase 5)

4. **Drag and Drop**
   - Not implemented yet
   - Workaround: Use long-press menus

5. **Grid Positioning**
   - Desktop items show in list, not grid
   - Fix: Implement proper grid layout (Phase 2)

6. **Folder Creation from UI**
   - Can only create folders via database
   - Fix: Add "Create Folder" in app drawer (Phase 2)

---

## Bug Report Template

When you find a bug, use this template:

```markdown
### Bug: [Short Description]

**Priority**: High / Medium / Low
**Component**: Home Screen / App Drawer / Folders / Settings / Database

**Steps to Reproduce**:
1.
2.
3.

**Expected Behavior**:


**Actual Behavior**:


**Screenshots/Logs**:
[Attach if available]

**Device Info**:
- Device:
- Android Version:
- Build:
```

---

## Testing Tools

### Android Studio Tools

1. **Logcat**
   - View → Tool Windows → Logcat
   - Filter by "com.launcher"
   - Check for errors/crashes

2. **Layout Inspector**
   - Tools → Layout Inspector
   - View UI hierarchy
   - Debug layout issues

3. **Database Inspector**
   - View → Tool Windows → App Inspection
   - Select "launcher_database"
   - View/edit database records

4. **Profiler**
   - View → Tool Windows → Profiler
   - Monitor CPU, Memory, Network
   - Detect performance issues

### ADB Commands

```bash
# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep "com.launcher"

# Clear app data (reset)
adb shell pm clear com.launcher.ai

# Launch app
adb shell am start -n com.launcher.ai/.HomeScreenActivity

# Set as default launcher
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

---

## Post-Testing

After completing all tests:

1. **Document Bugs**
   - Create BUGS.md
   - List all found bugs
   - Prioritize by severity

2. **Create Issues**
   - GitHub Issues for each bug
   - Use bug template above
   - Assign priorities

3. **Plan Fixes**
   - Critical bugs: Fix immediately
   - High priority: Fix before Phase 2
   - Medium/Low: Backlog

4. **Re-test**
   - Fix bugs
   - Re-run failed tests
   - Mark as passing

---

## Success Criteria

Phase 1 is "bug-free" when:

- ✅ No crashes during normal usage
- ✅ All core features work (launch apps, drawer, search)
- ✅ Launcher can be set as default
- ✅ Dock and layout persist across restarts
- ✅ No critical bugs (data loss, crashes, unusable features)
- ✅ Acceptable performance (60 FPS scrolling, < 1s app launch)

Minor issues are acceptable:
- Missing features (drag & drop, widget rendering)
- UI polish (animations, transitions)
- Edge cases (rare scenarios)

---

**Ready to test!** Start with "Phase 1: Installation & Setup" and work through each section.
