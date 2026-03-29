# CLAUDE.md — Shadow Contacts Development Context

## Overview

Shadow Contacts is a private, offline Android contact book. It stores contacts locally using Room (SQLite) and never syncs with cloud services. It includes a Caller ID popup system that shows contact info over the dialer during incoming calls, with notification persistence for missed calls.

The app's UI is modeled after **Dailygraph** (a sister journal app by the same developer). Both apps share the same iOS-inspired Material 3 design language, color palette, dialog patterns, and multi-select toolbar approach.

## Build Commands

```bash
# ── Full variant (GitHub / F-Droid / sideloading) — includes Caller ID ──
./gradlew assembleFullDebug          # Debug APK
./gradlew assembleFullRelease        # Release APK
./gradlew installFullDebug           # Install on device

# ── Play Store variant — no telephony permissions, no Caller ID ──
./gradlew assemblePlaystoreDebug     # Debug APK
./gradlew assemblePlaystoreRelease   # Release AAB/APK
./gradlew installPlaystoreDebug      # Install on device

# ── General ──
./gradlew clean assembleFullDebug    # Clean + build
./gradlew assembleDebug              # Builds BOTH flavors
```

## Build Flavor System

The project has two build flavors in the `distribution` dimension:

| Flavor | `BuildConfig.HAS_CALLER_ID` | Purpose |
|---|---|---|
| `full` | `true` | GitHub, F-Droid, sideloading — all features |
| `playstore` | `false` | Google Play Store — no telephony permissions |

### How it works

**Gradle** (`app/build.gradle.kts`):
```kotlin
flavorDimensions += "distribution"
productFlavors {
    create("full") {
        dimension = "distribution"
        buildConfigField("boolean", "HAS_CALLER_ID", "true")
    }
    create("playstore") {
        dimension = "distribution"
        buildConfigField("boolean", "HAS_CALLER_ID", "false")
    }
}
```

**Manifest merging**:
- `src/main/AndroidManifest.xml` — common permissions (READ_CONTACTS only), all activities
- `src/full/AndroidManifest.xml` — adds READ_PHONE_STATE, READ_CALL_LOG, SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE, POST_NOTIFICATIONS, CallerIdService declaration, and static `IncomingCallReceiver` declaration with `PHONE_STATE` intent-filter
- `playstore` has NO extra manifest — so those permissions and service don't exist in the merged manifest

**Runtime checks** — Code uses `BuildConfig.HAS_CALLER_ID`:
- `MainActivity.kt` → `onPrepareOptionsMenu()`: "Caller ID Popup" menu item is ALWAYS visible. In `full`, it has a checkbox (checkable=true). In `playstore`, no checkbox (checkable=false).
- `MainActivity.kt` → `onOptionsItemSelected()`: In `full`, tapping calls `toggleCallerId()` (permission flow + enable/disable). In `playstore`, tapping calls `showCallerIdPromo()` — an 85%-width dialog explaining the Play Store limitation with an "Open GitHub" button linking to the repo.
- `InstructionsActivity.kt` → `setupFlavorVisibility()`: In `full`, shows Caller ID, Permissions, Recommended Settings, and GitHub cards. In `playstore`, hides those 4 cards and shows a "Full Version Available" promo card with clickable GitHub link.
- The caller ID Kotlin classes (`callerid/` package) compile in both flavors but are never invoked in `playstore`

**GitHub URL constant**: `InstructionsActivity.GITHUB_URL` — single source of truth for the repo URL (`https://github.com/amishounak/ShadowContacts`). Used by the Instructions GitHub card (full), the playstore promo card, and the caller ID promo dialog in `MainActivity`.

### Why this split exists

Google Play restricts READ_CALL_LOG and READ_PHONE_STATE to apps that are the default dialer or SMS handler. Shadow Contacts is neither. The `playstore` flavor ships without these permissions to pass Play Store review. The `full` flavor has everything and is distributed via GitHub/F-Droid/APK.

### Build variant selection in Android Studio

In Android Studio: **Build → Select Build Variant** (or the "Build Variants" panel on the left sidebar). You'll see four options:
- `fullDebug` / `fullRelease`
- `playstoreDebug` / `playstoreRelease`

Select `fullDebug` for development. Select `playstoreRelease` when building for Play Store submission.

## Tech Stack

- **Language**: Kotlin 2.0.21
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (SQLite) with KSP annotation processing
- **Async**: Kotlin Coroutines + LiveData
- **UI**: Material 3 + ViewBinding (no Compose, no DataBinding)
- **Serialization**: Gson for JSON import/export
- **AGP**: 8.7.0, **Gradle**: 8.9
- **Min SDK**: 26 (Android 8.0), **Target SDK**: 35 (Android 15)
- **Package**: `com.shadowcontacts.app`

## Project Structure

```
ShadowContacts/
├── .gitignore
├── build.gradle.kts                    # Root: AGP 8.7.0, Kotlin 2.0.21, KSP 2.0.21-1.0.27
├── settings.gradle.kts                 # dependencyResolutionManagement, FAIL_ON_PROJECT_REPOS
├── gradle.properties                   # JVM args, AndroidX, nonTransitiveRClass
├── gradle/wrapper/
│   └── gradle-wrapper.properties       # Gradle 8.9
├── README.md
├── CLAUDE.md                           # This file
└── app/
    ├── build.gradle.kts                # Flavors: full / playstore. Deps: Room 2.6.1, Material 1.11.0, Gson 2.10.1
    ├── proguard-rules.pro
    └── src/
        ├── main/                       # Shared code + resources (both flavors)
        │   ├── AndroidManifest.xml     # Common: READ_CONTACTS only. Activities. NO caller ID permissions/service.
        │   ├── java/com/shadowcontacts/app/
        │   │   ├── ShadowContactsApp.kt           # Application: theme init only
        │   │   ├── data/
        │   │   │   ├── Contact.kt                  # @Entity: id, groupId, prefix, name, suffix, phone, description, timestamps
        │   │   │   ├── Group.kt                    # @Entity: id, name, isDefault
        │   │   │   ├── ContactDao.kt               # Room DAO with LiveData queries
        │   │   │   └── ContactDatabase.kt          # Room singleton, default group on first run
        │   │   ├── repository/
        │   │   │   └── ContactRepository.kt        # Thin wrapper over DAO
        │   │   ├── viewmodel/
        │   │   │   └── ContactViewModel.kt         # AndroidViewModel, MediatorLiveData, multi-select
        │   │   ├── ui/
        │   │   │   ├── MainActivity.kt             # Main list, search, dual-toolbar multi-select, caller ID promo dialog
        │   │   │   ├── ContactDetailActivity.kt    # View/edit/create, Call/SMS/WhatsApp, start_edit intent
        │   │   │   ├── PhoneContactPickerActivity.kt # Multi-select phone contact import picker
        │   │   │   ├── PhoneContactPickAdapter.kt  # Picker adapter
        │   │   │   ├── InstructionsActivity.kt     # Flavor-aware guide: hides caller ID cards in playstore, shows promo card + GitHub link
        │   │   │   ├── ContactAdapter.kt           # List adapter: section headers, checkboxes, action buttons
        │   │   │   ├── GroupAdapter.kt             # Legacy (unused)
        │   │   │   └── GroupManagerActivity.kt     # Legacy (unused)
        │   │   ├── callerid/
        │   │   │   ├── IncomingCallReceiver.kt     # BroadcastReceiver, handles Android 10+ double-broadcast
        │   │   │   └── CallerIdService.kt          # Overlay + notification + drag + position memory
        │   │   └── utils/
        │   │       ├── ThemeHelper.kt              # Light/Dark/Auto
        │   │       ├── IOSStyleDialog.kt           # AlertDialog wrapper, 85% min width, enforceMinWidthPublic()
        │   │       ├── ImportExportHelper.kt       # Gson JSON backup/restore
        │   │       ├── PhoneContactImporter.kt     # ContentResolver reader for ContactsContract
        │   │       ├── CallerIdPreferences.kt      # SharedPreferences toggle
        │   │       └── GroupPreferences.kt         # Active group persistence
        │   └── res/
        │       ├── layout/
        │       │   ├── activity_main.xml           # Dual toolbar, Dailygraph search bar, empty state, FAB
        │       │   ├── activity_contact_detail.xml # Call/SMS/WhatsApp buttons, form card
        │       │   ├── activity_phone_contact_picker.xml # Search, selection bar, list, import button
        │       │   ├── activity_instructions.xml   # Cards: cardGitHub, cardCallerId, cardPermissions, cardRecommended (full only), cardFullVersion (playstore only)
        │       │   ├── activity_group_manager.xml  # Legacy
        │       │   ├── item_contact.xml            # Card: checkbox + avatar + name/phone/desc + round action buttons
        │       │   ├── item_phone_contact.xml      # Picker: checkbox + avatar + name/phone + EXISTS badge
        │       │   ├── item_section_header.xml     # Alphabetical letter header
        │       │   └── overlay_caller_id.xml       # Dark card: name, phone, description, drag hint
        │       ├── menu/
        │       │   ├── menu_main.xml               # Import from Phone, Import from Backup, Export, Appearance, Caller ID Popup, Instructions
        │       │   ├── menu_multi_select.xml       # Select All, Edit (ic_edit), Delete (ic_delete, danger color)
        │       │   └── menu_contact_detail.xml     # Save (always shown), Edit/Cancel/Delete (all in overflow menu)
        │       ├── drawable/                       # Vector icons, backgrounds, selectors
        │       ├── values/                         # colors, themes, strings (light)
        │       └── values-night/                   # colors, themes (dark)
        │
        ├── full/                        # Full flavor overlay
        │   └── AndroidManifest.xml     # Adds: READ_PHONE_STATE, READ_CALL_LOG, SYSTEM_ALERT_WINDOW,
        │                               #        FOREGROUND_SERVICE, POST_NOTIFICATIONS, CallerIdService,
        │                               #        IncomingCallReceiver (static, PHONE_STATE intent-filter)
        │
        └── playstore/                   # Play Store flavor (empty — just uses main)
```

## Key Architecture Patterns

### ViewModel + MediatorLiveData
`ContactViewModel` uses `MediatorLiveData` for the contact list. It switches between `getContactsByGroup()` and `searchContacts()` sources depending on search query. Both `_currentGroupId` and `_searchQuery` are triggers. Repository and allGroups are initialized inline (NOT in init block) to avoid Kotlin initialization order issues.

### Dual Toolbar Multi-Select (Dailygraph Pattern)
`MainActivity` has two `MaterialToolbar` views:
- `toolbar` — normal mode: app title + 3-dot overflow menu
- `toolbarMultiSelect` — multi-select: X nav icon, "N selected" title, inflated `menu_multi_select`

`enterMultiSelectMode()` hides toolbar, shows toolbarMultiSelect, hides FAB. The adapter manages checkbox state via `selectedIds` set. Edit action works only when exactly 1 contact selected.

### Caller ID System (full flavor only)
1. `IncomingCallReceiver` is declared as a **static manifest receiver** in `src/full/AndroidManifest.xml` with a `PHONE_STATE` intent-filter. There is no dynamic registration — `ShadowContactsApp` only does theme init.
2. The on/off toggle works via `CallerIdPreferences`. `IncomingCallReceiver.onReceive()` checks `CallerIdPreferences.isEnabled()` as its second line and returns early if disabled. `MainActivity.toggleCallerId()` / `enableCallerId()` only flip the preference and call `invalidateOptionsMenu()` — they do not register or unregister the receiver.
3. **Permission flow** when enabling Caller ID: `enableCallerId()` → READ_PHONE_STATE + READ_CALL_LOG → `checkNotificationAndOverlayPermissions()` → POST_NOTIFICATIONS (Android 13+) → `checkOverlayAndEnableCallerId()` → SYSTEM_ALERT_WINDOW → set preference enabled. Each step checks if already granted and skips if so.
4. `IncomingCallReceiver` catches `PHONE_STATE_CHANGED`, handles Android 10+ double-broadcast (first=null number, second=real). Starts `CallerIdService`.
5. `CallerIdService` queries Room on IO, normalizes numbers (last 10 digits), shows overlay via `WindowManager.TYPE_APPLICATION_OVERLAY` + notification. Tapping the notification opens `ContactDetailActivity` for the matched contact (passes `contact_id` extra). Overlay is draggable with Y position saved to SharedPreferences. Auto-dismiss after 30 seconds.

### Dialog Pattern
All dialogs use `android.app.AlertDialog` (NOT MaterialAlertDialogBuilder) with `R.style.IOSDialogTheme`. Button colors set AFTER `show()`. `enforceMinWidth()` sets 85% screen width.

### Phone Contact Picker
`PhoneContactPickerActivity` reads system contacts via `PhoneContactImporter.readPhoneContacts()`, shows them in RecyclerView with checkboxes. Features: search, Select All/Deselect All, "EXISTS" badge for duplicates, bottom "Import X Contacts" button.

### Display Name Format
`Contact.displayName()` returns `"Dr. John Smith, Jr."` — comma before suffix.

## Color System (iOS-inspired)

| Token | Light | Dark |
|---|---|---|
| accent | #007AFF | #0A84FF |
| background | #F2F2F7 | #000000 |
| surface | #FFFFFF | #1C1C1E |
| search_bg | #E5E5EA | #2C2C2E |
| text_primary | #000000 | #FFFFFF |
| text_secondary | #6C6C70 | #98989D |
| text_tertiary | #AEAEB2 | #636366 |
| divider | #E5E5EA | #38383A |
| danger | #FF3B30 | #FF453A |
| selection_bg | #D6EAFF | #1A3050 |
| action_btn_bg | #EBF4FF | #1A2A3A |

## Database Schema

### contacts table
| Column | Type | Notes |
|---|---|---|
| id | Long | Auto-generated PK |
| groupId | Long | FK to groups (default 1) |
| prefix | String | "Dr.", "Mr.", etc. |
| name | String | Full name (required) |
| suffix | String | "Jr.", "Sr.", etc. |
| phone | String | Phone number |
| description | String | Freeform notes |
| createdAt | Long | Epoch millis |
| updatedAt | Long | Epoch millis |

### groups table
| Column | Type | Notes |
|---|---|---|
| id | Long | Auto-generated PK |
| name | String | Group name |
| isDefault | Boolean | True for "All Contacts" |

Groups exist in schema but NOT in UI. All contacts use groupId=1. Legacy holdover.

## SharedPreferences Keys

All under `"shadow_contacts_prefs"`:
- `theme_mode` (Int): 0=Light, 1=Dark, 2=Auto
- `active_group_id` (Long): Active group (default 1)
- `caller_id_enabled` (Boolean): Caller ID toggle
- `overlay_y_position` (Int): Overlay Y position

## JSON Export Format

```json
{
  "appName": "ShadowContacts",
  "version": 1,
  "exportedAt": 1234567890,
  "groups": [{ "id": 1, "name": "All Contacts", "isDefault": true }],
  "contacts": [{
    "id": 1, "groupId": 1,
    "prefix": "Dr.", "name": "John Smith", "suffix": "Jr.",
    "phone": "+919876543210", "description": "Cardiologist",
    "createdAt": 1234567890, "updatedAt": 1234567890
  }]
}
```

## Permissions by Flavor

| Permission | main | full | playstore | Used By |
|---|---|---|---|---|
| READ_CONTACTS | ✅ | — | — | PhoneContactPickerActivity |
| READ_PHONE_STATE | — | ✅ | ❌ | IncomingCallReceiver |
| READ_CALL_LOG | — | ✅ | ❌ | IncomingCallReceiver (Android 10+) |
| SYSTEM_ALERT_WINDOW | — | ✅ | ❌ | CallerIdService overlay |
| FOREGROUND_SERVICE | — | ✅ | ❌ | CallerIdService |
| POST_NOTIFICATIONS | — | ✅ | ❌ | CallerIdService notification |

## Known Design Decisions

- **No Compose**: Entire UI is XML + ViewBinding. Matches Dailygraph.
- **No groups in UI**: Group system exists in DB but UI was simplified. All contacts go to groupId=1.
- **Legacy files**: GroupManagerActivity, GroupAdapter, spinner layouts — not reachable from UI. Safe to delete.
- **WhatsApp assumes +91**: If number doesn't start with "+", prepends "+91" (India). Make configurable for international use.
- **Overlay is dark-only**: Hardcoded #1C1C2E background regardless of theme. Intentional.
- **Number matching**: Normalizes to last 10 digits. Works for India, may need adjustment elsewhere.
- **android.app.AlertDialog**: Used over MaterialAlertDialogBuilder for better button color control.
- **Action buttons**: Call/SMS/WhatsApp icons sit in `bg_icon_circle.xml` (oval, search_bg color) for themed round containers.
- **Playstore caller ID promo**: Menu item stays visible but non-checkable. Tapping opens a dialog explaining the Play Store limitation with an "Open GitHub" button. This ensures users discover the full version.
- **GITHUB_URL constant**: Set to `https://github.com/amishounak/ShadowContacts` in `InstructionsActivity.GITHUB_URL`. Single source of truth — used by the full-flavor GitHub card, the playstore promo card, and the caller ID promo dialog in `MainActivity`.
- **Instructions page flavor-awareness**: `activity_instructions.xml` has five cards: `cardGitHub`, `cardCallerId`, `cardPermissions`, `cardRecommended` (all shown in full, hidden in playstore), and `cardFullVersion` (hidden in full, shown in playstore). `InstructionsActivity.setupFlavorVisibility()` toggles visibility. The `cardGitHub` card links to the GitHub repo and is visible only in the full flavor.

## Common Tasks

### Add a new field to Contact
1. Add column to `Contact.kt`
2. Increment DB version in `ContactDatabase.kt`, add migration
3. Update `ContactDao.kt` queries
4. Update `activity_contact_detail.xml`
5. Update `ContactDetailActivity.kt`
6. Update `ImportExportHelper.kt`
7. Update adapter if shown in list

### Add a new menu item
1. Add `<item>` to `menu_main.xml`
2. Handle in `onOptionsItemSelected()` in `MainActivity.kt`

### Change Caller ID overlay
- Layout: `overlay_caller_id.xml`
- Logic: `CallerIdService.kt` → `showOverlay()`
- Timeout: `OVERLAY_TIMEOUT_MS` (30000ms)
- Position: `getSavedY()` / `saveY()`

### Build for Play Store
```bash
./gradlew assemblePlaystoreRelease
# or for AAB:
./gradlew bundlePlaystoreRelease
```
The output APK/AAB will NOT contain any telephony permissions or CallerIdService.

### Build for GitHub / sideloading
```bash
./gradlew assembleFullRelease
```
The output APK contains all features including Caller ID.

### Sign a release build
Create a keystore, then add to `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("path/to/keystore.jks")
        storePassword = "..."
        keyAlias = "..."
        keyPassword = "..."
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

## Known Fixes Applied

### API 35 Edge-to-Edge (fixed 2026-03-29)
Android 15 (API 35) enforces edge-to-edge by default, causing the toolbar to overlap the status bar.
**Fix**: Added `<item name="android:windowOptOutEdgeToEdgeEnforcement">true</item>` to `Theme.ShadowContacts` in both `values/themes.xml` and `values-night/themes.xml`. Same fix as Dailygraph.

### Target SDK 35 (updated 2026-03-29)
Google Play requires `targetSdk >= 35` as of 2026. Updated `compileSdk` and `targetSdk` from 34 to 35 in `app/build.gradle.kts`.

### Release Signing (added 2026-03-29)
Added `signingConfigs` block in `app/build.gradle.kts` referencing `../release-keystore.jks` (alias: `shadow-contacts`). Keystore is in `.gitignore`.

## Testing Notes

- Caller ID requires a real phone call — cannot be tested in emulator
- For testing, add a contact with your own number, call from another phone
- Logcat filter: `ShadowCallerID` for receiver debug
- Chinese ROMs (MIUI, ColorOS): enable autostart + battery unrestricted manually
- **Build variant must be `fullDebug` to test Caller ID** — `playstoreDebug` won't have the permissions
- Import from Phone requires at least one contact in phone's address book
