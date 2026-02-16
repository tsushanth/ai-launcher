# Bug Tracker

## Active Bugs

### Critical (Blocks Core Functionality)
*None yet - add bugs as you find them during testing*

### High Priority (Major Features Broken)
*None yet*

### Medium Priority (Minor Features or Workarounds Exist)
*None yet*

### Low Priority (Polish, Edge Cases)
*None yet*

---

## Known Limitations (Not Bugs - Intentional)

### 1. Folder Creation
**Status**: Feature not implemented yet
**Workaround**: Folders can only be created programmatically via database
**Planned**: Phase 2 - Add "Create Folder" from app drawer
**Priority**: Medium

### 2. Drag and Drop
**Status**: Feature not implemented yet
**Workaround**: Use long-press menus
**Planned**: Phase 5 - Add drag/drop for rearranging
**Priority**: Low

### 3. Widget Rendering
**Status**: Simplified placeholder implementation
**Workaround**: Widgets show as placeholder boxes
**Planned**: Phase 5 - Full AppWidgetHostView integration
**Priority**: Medium

### 4. Desktop Grid Positioning
**Status**: Items show in list, not positioned grid
**Workaround**: Items display vertically
**Planned**: Phase 2 - Implement proper grid layout with positioning
**Priority**: Medium

### 5. App Info from Long-Press
**Status**: Menu option exists but not implemented
**Workaround**: Open app settings manually
**Planned**: Phase 2 - Implement app info intent
**Priority**: Low

---

## Fixed Bugs

*Bugs that have been fixed will be moved here with fix date and commit*

---

## Bug Report Template

Copy this template when reporting a bug:

```markdown
### Bug: [Short Title]

**ID**: BUG-XXX
**Priority**: Critical / High / Medium / Low
**Component**: Home Screen / App Drawer / Folders / Wallpaper / Settings / Database
**Found**: YYYY-MM-DD
**Status**: Open / In Progress / Fixed / Won't Fix

**Description**:
[Clear description of the bug]

**Steps to Reproduce**:
1. Step one
2. Step two
3. Step three

**Expected Behavior**:
[What should happen]

**Actual Behavior**:
[What actually happens]

**Frequency**:
- [ ] Always (100%)
- [ ] Often (> 50%)
- [ ] Sometimes (< 50%)
- [ ] Rare (< 10%)

**Environment**:
- Device:
- Android Version:
- Build: debug / release
- Commit:

**Logs** (if applicable):
```
[Paste relevant logcat output]
```

**Screenshots** (if applicable):
[Attach images]

**Possible Cause**:
[If you have a theory]

**Suggested Fix**:
[If you have an idea]
```

---

## Testing Progress

Track which tests have been completed:

- [ ] Installation & Setup
- [ ] Home Screen
- [ ] App Drawer
- [ ] Long-Press Menus
- [ ] Folders
- [ ] Wallpaper
- [ ] Settings
- [ ] Database & Persistence
- [ ] Performance
- [ ] Edge Cases

**Last Updated**: [Date]
**Bugs Found**: 0
**Bugs Fixed**: 0
**Bugs Remaining**: 0
