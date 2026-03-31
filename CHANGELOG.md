# Changelog — Shadow Contacts

All notable changes to Shadow Contacts are documented here.
Format: `[version] — date — description`

---

## [1.0.2] — 2026-04-01

### Fixed
- **WhatsApp button text wrapping** — "WhatsApp" was breaking into two lines ("What" / "sApp") on smaller screens due to equal `layout_weight` with no `maxLines` constraint. Fixed by adding `android:maxLines="1"` + `android:ellipsize="end"` + `android:textSize="13sp"` + `app:iconSize="18dp"` to all three action buttons in `activity_contact_detail.xml`.
- **WhatsApp icon tint on some devices** — `app:iconTint="@null"` in XML is ignored by `MaterialButton` on certain Android versions and OEM ROMs (MIUI, ColorOS, etc.), causing the green WhatsApp icon to appear grey/blue. Fixed by setting `binding.btnWhatsApp.iconTint = null` programmatically in `ContactDetailActivity.setupActionButtons()`.
- **WhatsApp icon tint in contact list** — Same OEM tinting issue on the `ImageButton` in `item_contact.xml`. Fixed by setting `waBtn.imageTintList = null` in `ContactAdapter.bind()`.

---

## [1.0.1] — 2026-03-30

### Changed
- Bumped `versionCode` 1 → 2, `versionName` "1.0" → "1.0.1"
- Updated `compileSdk` and `targetSdk` from 34 → 35 (Android 15, required by Google Play from 2026)
- Fixed status bar overlap caused by Android 15's enforced edge-to-edge mode
  (`android:windowOptOutEdgeToEdgeEnforcement = true` in both light/dark themes)
- Added release signing config in `app/build.gradle.kts` (keystore at `../release-keystore.jks`)

### Distribution
- Submitted to Google Play Store (closed testing / Alpha track) — package `com.shadowcontacts.app`
- Privacy policy published at `https://raw.githubusercontent.com/amishounak/ShadowContacts/master/PRIVACY_POLICY.md`
- GitHub release created with both `full` and `playstore` APK flavors

### Known Limitation
- **Signing conflict**: Users who have the Play Store version installed cannot directly install the full APK over it. Google Play App Signing uses a different key than the sideload APK. Users must export contacts → uninstall → install full APK → import contacts.

### Play Store Status (as of 2026-04-01)
- Track: Closed Testing (Alpha) — **Active**
- Release: 2 (1.0.1) — approved and live
- Countries: 177
- Testers: Shounak email list (5 emails as of 2026-04-01)
- 14-day countdown starts once 12+ testers opt in

---

## [1.0.0] — 2026-03-25

### Added
- Initial release
- Private offline contact book with Room (SQLite) storage
- Caller ID popup overlay (`full` flavor only) — floating card over dialer during incoming calls
- Draggable overlay with Y-position memory
- Missed call notification tapping into contact detail
- Quick action buttons: Call, SMS, WhatsApp on every contact card
- Import from phone contacts with multi-select picker and EXISTS badge
- JSON backup/export and import with duplicate detection
- Multi-select with long-press (Dailygraph-style dual toolbar)
- Search across name, phone, prefix, suffix, description
- Light / Dark / Auto theme
- iOS-inspired Material 3 design (accent #007AFF / #0A84FF)
- Two build flavors: `full` (GitHub/F-Droid) and `playstore` (Google Play)
- Play Store promo dialog in `playstore` flavor when Caller ID menu is tapped
- Instructions page with flavor-aware cards
- MIT License

### Distribution
- GitHub release with `full` and `playstore` APK flavors
