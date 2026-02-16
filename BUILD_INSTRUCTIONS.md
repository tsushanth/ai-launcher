# Build Instructions

## Prerequisites

### Required Software
- **Android Studio**: Hedgehog (2023.1.1) or later
  - Download: https://developer.android.com/studio
- **JDK**: Version 17 (bundled with Android Studio)
- **Git**: For cloning the repository

### Required Hardware
- **For Emulator**: 8GB+ RAM recommended
- **For Physical Device**: Android 8.0 (API 26) or higher with USB debugging

## Step-by-Step Build Guide

### 1. Clone Repository

```bash
git clone https://github.com/tsushanth/ai-launcher.git
cd ai-launcher
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Click "Open" or File → Open
3. Navigate to `ai-launcher` folder
4. Click "OK"
5. Wait for Gradle sync (may take 2-5 minutes first time)

### 3. Configure Project

#### If Gradle Sync Fails:

**Check SDK Location**:
1. File → Settings (or Android Studio → Settings on Mac)
2. Appearance & Behavior → System Settings → Android SDK
3. Note the SDK Location (e.g., `/Users/username/Library/Android/sdk`)
4. If missing, download SDK Platform 34

**Check Java Version**:
1. File → Project Structure
2. SDK Location tab
3. JDK location should point to JDK 17
4. If not, select "Download JDK" and choose version 17

**Sync Again**:
1. File → Sync Project with Gradle Files
2. Wait for completion

### 4. Build the App

#### Option A: Build APK

1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Wait for build (1-3 minutes)
3. APK location: `app/build/outputs/apk/debug/app-debug.apk`
4. Success notification appears when done

#### Option B: Build and Run

1. Connect Android device via USB
   - OR click "Device Manager" and create/start an emulator
2. Click Run button (▶️) or press `Shift + F10`
3. Select device from list
4. Wait for build and installation
5. App launches automatically

### 5. Common Build Issues

#### Issue: "SDK Platform 34 not found"
**Solution**:
1. Tools → SDK Manager
2. SDK Platforms tab
3. Check "Android 14.0 (API 34)"
4. Click "Apply" → "OK"
5. Wait for download and installation

#### Issue: "Kotlin not configured"
**Solution**:
1. Tools → Kotlin → Configure Kotlin in Project
2. Select "All modules"
3. Click "OK"

#### Issue: "Failed to resolve dependencies"
**Solution**:
1. Check internet connection
2. File → Invalidate Caches → Invalidate and Restart
3. After restart: File → Sync Project with Gradle Files

#### Issue: "Execution failed for task ':app:mergeDebugResources'"
**Solution**:
1. Build → Clean Project
2. Build → Rebuild Project

#### Issue: "Unable to find method 'void android.support...'"
**Solution**: This shouldn't happen with our config, but if it does:
1. Check `gradle.properties` has:
   ```
   android.useAndroidX=true
   android.enableJetifier=true
   ```

### 6. Install on Physical Device

#### Enable USB Debugging:
1. Open Settings on Android device
2. Scroll to "About phone"
3. Tap "Build number" 7 times
4. Go back → Developer options
5. Enable "USB debugging"
6. Connect device to computer via USB
7. Allow USB debugging when prompted

#### Install via Android Studio:
1. Device should appear in device dropdown
2. Click Run (▶️)
3. App installs and launches

#### Install via ADB:
```bash
cd ~/Documents/GitHub/ai-launcher
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 7. Set as Default Launcher

1. Press **Home** button on device
2. Dialog appears: "Select default launcher"
3. Select "**AI Launcher**"
4. Choose "**Always**"
5. Launcher is now default!

To change default later:
1. Settings → Apps → Default apps → Home app
2. Select different launcher

### 8. Development Build vs Release Build

**Debug Build** (what we're building now):
- Larger APK size (~15-20 MB)
- Includes debug symbols
- Not optimized
- Good for testing

**Release Build** (for distribution):
- Smaller APK size (~8-10 MB)
- Optimized with ProGuard
- Requires signing key
- For Play Store or production use

To create release build (later):
```bash
./gradlew assembleRelease
# Requires signing configuration
```

## Verification

### Check if Build Succeeded

#### In Android Studio:
- Green checkmark in Build window
- Message: "BUILD SUCCESSFUL in X s"
- APK created in `app/build/outputs/apk/debug/`

#### Via Terminal:
```bash
cd ~/Documents/GitHub/ai-launcher
./gradlew assembleDebug

# Look for:
# BUILD SUCCESSFUL in Xs
```

### Check if App Installed

```bash
# List installed packages
adb shell pm list packages | grep launcher

# Should show:
# package:com.launcher.ai
```

### Check if App Launches

```bash
# Launch manually
adb shell am start -n com.launcher.ai/.HomeScreenActivity

# If successful, app opens on device
```

## Troubleshooting

### Gradle Build Fails

1. **Clean and Rebuild**:
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. **Check Logs**:
   - Build → Build Output window
   - Look for red error messages
   - Google the specific error

3. **Update Gradle**:
   - Help → Check for Updates
   - Update Android Studio and plugins

### App Crashes on Launch

1. **Check Logcat**:
   ```bash
   adb logcat | grep "AndroidRuntime"
   ```

2. **Common Causes**:
   - Missing permissions in AndroidManifest.xml
   - Database initialization errors
   - Missing Firebase configuration (optional)

3. **Clear App Data**:
   ```bash
   adb shell pm clear com.launcher.ai
   ```

### Device Not Detected

1. **Check USB Connection**:
   - Try different USB cable
   - Try different USB port
   - Restart ADB: `adb kill-server && adb start-server`

2. **Check Drivers** (Windows):
   - Install device-specific USB drivers
   - Google "[Device Name] USB drivers"

3. **Use Emulator Instead**:
   - Tools → Device Manager
   - Create Virtual Device
   - Select system image (API 34)
   - Start emulator

## Build Configuration

### Current Settings (from gradle files):

- **compileSdk**: 34 (Android 14)
- **minSdk**: 26 (Android 8.0 Oreo)
- **targetSdk**: 34 (Android 14)
- **Kotlin**: 1.9.10
- **Compose**: BOM 2024.02.00
- **Room**: 2.6.1

### Compatibility:

- Works on Android 8.0+ (Oreo and above)
- Optimized for Android 12+ (Material You)
- Tested on Android 14 (latest)

## Performance

### Build Times:
- **First build**: 2-5 minutes (downloads dependencies)
- **Clean build**: 1-2 minutes
- **Incremental build**: 10-30 seconds

### APK Size:
- **Debug**: ~15-20 MB
- **Release**: ~8-10 MB (when configured)

## Next Steps After Build

1. ✅ Verify app installs
2. ✅ Set as default launcher
3. ✅ Follow TESTING.md checklist
4. ✅ Report bugs in BUGS.md
5. ✅ Fix critical issues
6. ✅ Re-test

---

**Need help?** Check:
- TESTING.md - Testing guide
- README.md - Project overview
- GitHub Issues - Known problems
