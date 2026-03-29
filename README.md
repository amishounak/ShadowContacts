# Shadow Contacts

A private, offline contact book for Android. Your contacts stay on your device — never synced with Google, Samsung, or any cloud service.

Built with Kotlin, MVVM architecture, and Material 3 design with an iOS-inspired aesthetic.

## Download

| Version | Download |
|---|---|
| Full (with Caller ID) | [ShadowContacts-v1.0.0-full.apk](https://github.com/amishounak/ShadowContacts/releases/download/v1.0.0/ShadowContacts-v1.0.0-full.apk) |

> Enable **Install from unknown sources** in your Android settings before installing.

## Features

**Private Contact Book**
Store contacts with Prefix, Name, Suffix, Phone, and Description fields. Names display as "Dr. John Smith, Jr." — comma before suffix. All data is stored locally in Room (SQLite). No internet required.

**Caller ID Popup**
When enabled, a floating popup appears over your screen during incoming calls, showing the contact's full name and description. The popup is draggable — slide it vertically to your preferred position and it remembers the placement across calls. Auto-dismisses after 30 seconds. A persistent notification is also created so you can identify missed calls — tap it to jump straight to the contact's detail view.

**Quick Actions**
Three action buttons on every contact card — Call, SMS, and WhatsApp — each inside a themed circular background. Tap to call via dialer, send SMS, or open a WhatsApp chat directly without saving the contact in your phone's address book.

**Import from Phone (Contact Picker)**
Opens a full-screen picker showing all your phone contacts with:
- Multi-select with iOS-style oval checkboxes
- Select All / Deselect All buttons
- Live search to filter by name or phone
- "EXISTS" badge on contacts already in Shadow Contacts
- Bottom "Import X Contacts" button that imports only selected, skipping duplicates

**Import & Export (Backup)**
Export all contacts to a JSON backup file. Import from backup on a new device with automatic duplicate detection.

**Multi-Select**
Long-press to enter multi-select mode with a dedicated toolbar (Dailygraph-style). iOS-style oval checkboxes appear on each card. Select All, Edit (single), or Delete (bulk) via the multi-select toolbar. Action buttons hide during selection.

**Search**
Dailygraph-style CardView search bar with custom vector icons. Full-text search across name, phone, prefix, suffix, and description. Clear button appears dynamically.

**Appearance**
Light mode, dark mode, or system-auto. iOS-inspired color palette with true black dark theme. Rounded popup menus offset below the toolbar, rounded dialogs at 85% screen width, card-based layouts with 16dp corners, and Material 3 components throughout.

**Instructions**
Built-in guide (Menu → Instructions) explaining all permissions, their purposes, and recommended device settings for reliable background operation including unrestricted battery and autostart for Chinese OEM ROMs.

## Permissions

| Permission | Purpose | When Requested |
|---|---|---|
| READ_PHONE_STATE | Detect incoming calls for Caller ID | When enabling Caller ID |
| READ_CALL_LOG | Get caller's phone number (Android 10+) | When enabling Caller ID |
| SYSTEM_ALERT_WINDOW | Draw popup over dialer/lock screen | When enabling Caller ID |
| POST_NOTIFICATIONS | Show notification for missed calls | When enabling Caller ID |
| READ_CONTACTS | Read phone contacts for picker import | When using "Import from Phone" |

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM |
| Database | Room (SQLite) |
| Async | Kotlin Coroutines + LiveData |
| UI | Material 3 + ViewBinding |
| Serialization | Gson |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |
| AGP | 8.7.0 |
| Kotlin | 2.0.21 |
| Gradle | 8.9 |

## Project Structure

```
app/src/main/java/com/shadowcontacts/app/
├── ShadowContactsApp.kt                  # Application class, theme + receiver init
├── data/
│   ├── Contact.kt                         # Contact entity (prefix, name, suffix, phone, description)
│   ├── Group.kt                           # Group entity
│   ├── ContactDao.kt                      # Room DAO
│   └── ContactDatabase.kt                # Room database singleton
├── repository/
│   └── ContactRepository.kt              # Data access layer
├── viewmodel/
│   └── ContactViewModel.kt               # Business logic, LiveData, multi-select
├── ui/
│   ├── MainActivity.kt                   # Contact listing, search, multi-select, menus
│   ├── ContactDetailActivity.kt          # View/edit/create contact with Call/SMS/WhatsApp
│   ├── PhoneContactPickerActivity.kt     # Multi-select phone contact import picker
│   ├── InstructionsActivity.kt           # Permissions & settings guide
│   ├── ContactAdapter.kt                 # List adapter with section headers + checkboxes
│   ├── PhoneContactPickAdapter.kt        # Phone contact picker adapter
│   ├── GroupAdapter.kt                    # Group list adapter (legacy)
│   └── GroupManagerActivity.kt            # Group management (legacy)
├── callerid/
│   ├── IncomingCallReceiver.kt            # Phone state broadcast receiver
│   └── CallerIdService.kt                # Overlay + notification + drag + position memory
└── utils/
    ├── ThemeHelper.kt                     # Light/Dark/Auto theme management
    ├── IOSStyleDialog.kt                  # Rounded dialogs with min width enforcement
    ├── ImportExportHelper.kt              # JSON backup/restore
    ├── PhoneContactImporter.kt            # System contacts reader
    ├── CallerIdPreferences.kt             # Caller ID toggle persistence
    └── GroupPreferences.kt                # Active group persistence
```

## Design Principles

### iOS-Inspired Aesthetics
- 16dp rounded card corners, 14dp dialog corners
- iOS blue accent (#007AFF light / #0A84FF dark)
- Oval checkbox selectors for multi-select
- Rounded popup menus with subtle border, offset below toolbar
- Dialogs at 85% screen width with explicit accent-colored buttons
- Circular themed backgrounds on action icon buttons

### Dual Toolbar Multi-Select (Dailygraph Pattern)
- Normal toolbar swaps to multi-select toolbar on long-press
- X button to exit, "N selected" title, Select All / Edit / Delete actions
- Checkboxes animate in, action buttons hide
- FAB hides during selection

### Privacy First
- No cloud sync, no analytics, no tracking
- All data stored locally in Room database
- Phone contacts are read-only during import (never modified)
- No internet permission required for core functionality

## Build & Run

1. Open in Android Studio (Ladybug 2024.2.1 or later)
2. Sync Gradle
3. Select build variant: **Build → Select Build Variant**
   - `fullDebug` — all features including Caller ID (for development/GitHub/F-Droid)
   - `playstoreDebug` — no telephony permissions (for Play Store testing)
4. Build and run on device or emulator (API 26+)

### Build Flavors

| Flavor | Caller ID | Target Distribution |
|---|---|---|
| `full` | Yes — all telephony permissions | GitHub, F-Droid, sideloading |
| `playstore` | No — stripped for Play Store compliance | Google Play Store |

```bash
./gradlew assembleFullDebug          # Full with Caller ID
./gradlew assemblePlaystoreRelease   # Play Store release
```

## Configuration

**Package**: `com.shadowcontacts.app`
**Application ID**: `com.shadowcontacts.app`

## Recommended Device Settings

For reliable Caller ID popup:

1. **Battery**: Settings → Apps → Shadow Contacts → Battery → Unrestricted
2. **Autostart** (Xiaomi/Oppo/Vivo/Realme): Settings → Apps → Manage Apps → Shadow Contacts → Enable Autostart
3. **Display over other apps**: Enabled via app menu toggle

## License

[MIT License](LICENSE) — Copyright (c) 2026 Shounak Datta

---

**Shadow Contacts** — Your contacts, your device, your privacy.
