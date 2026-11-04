# O'ZKEY Android — MVP 0.1.0

Native Android keyboard prototype built with Kotlin and `InputMethodService`.

## Open in Android Studio
1. Extract this ZIP.
2. Open the `OZKEY_Android` folder in Android Studio.
3. Allow Gradle sync and install the app on an Android device.
4. Open O'ZKEY, tap **1. O'ZKEY ni yoqish**, enable it in Android settings.
5. Return to O'ZKEY and tap **2. Klaviaturani tanlash**.

## Current MVP
- Android system keyboard service
- Basic QWERTY layout
- Ğ, Ö, Ş, Ç keys
- Automatic conversion: `Gʻ/gʻ → Ğ/ğ`, `Oʻ/oʻ → Ö/ö`, `SH/sh → Ş/ş`, `CH/ch → Ç/ç` (also accepts common apostrophe variants)
- Shift, backspace, enter, space
- Local-only prototype; no network permission

## Important prototype limitations
- This is source code, not a prebuilt APK.
- The base G and O letters remain available; Ğ, Ö, Ş, Ç are added as separate one-tap keys. Numeric/symbol page, full language layouts, preferences, suggestions, themes, accessibility polish, and expanded testing are next milestones.
- The new Uzbek alphabet rules should be confirmed against the official standard before release.
- Always test with non-sensitive sample text first. Android may switch IMEs for password fields.
