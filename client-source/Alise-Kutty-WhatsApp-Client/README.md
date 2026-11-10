# Alise×Kutty — WhatsApp Web Android Client

An unofficial Android WebView wrapper for `https://web.whatsapp.com/`, branded Alise×Kutty, with a local CSS theme extension manager.

## Included
- Branded Android app shell and supplied black/white manga-style icon
- WhatsApp Web login through the official WhatsApp Web site (QR linking)
- Local extension ZIP importer
- Enable/disable installed CSS extensions
- ZIP validation: only `manifest.json` and `style.css` are accepted; JavaScript extensions are intentionally not executed

## Build
1. Open this folder in Android Studio (or another Android Gradle IDE).
2. Install Android SDK 35 and a compatible JDK (17 recommended).
3. Sync Gradle, then Build > Build APK(s).
4. Install the generated debug APK on your Android device.

This ZIP contains the complete Android Studio source project, not a precompiled APK. The first Gradle sync needs internet access to download Android Gradle Plugin/Kotlin dependencies.

## Extension ZIP format
Create a ZIP containing exactly these two files at the ZIP root:

`manifest.json`
```json
{
  "name": "Midnight Dark Theme",
  "version": "1.0.0",
  "description": "A dark CSS theme",
  "entry": "style.css"
}
```

`style.css`
```css
/* Example only; WhatsApp Web may change its page structure at any time. */
body { color-scheme: dark; }
```

In the app, tap **Extensions → Import extension ZIP**. Imported styles can be enabled or disabled from the Extensions list.

## Important limitations and safety
- This is an independent WebView wrapper, not an official WhatsApp app or a modified WhatsApp APK. It uses WhatsApp Web, so WhatsApp controls login, availability, and compatibility.
- WhatsApp may change its Web interface or restrict WebView/desktop rendering; compatibility is not guaranteed.
- Use your own account and link the device using WhatsApp's supported Linked Devices flow. Never share QR codes or session files.
- Extensions are CSS-only by design. Do not import unknown files or give third-party apps access to your WhatsApp session.
- No message scraping, bulk messaging, auto-replies, account-token collection, or credential export is included.
