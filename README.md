# PromptVault

A lightweight, private-by-default prompt library. Save useful AI prompts, organize them, search them, and copy them whenever you need them.

## Current features

- Create, edit, and delete prompts
- Organize prompts into categories
- Add searchable tags
- Mark prompts as favorites
- Search prompt titles, text, categories, and tags
- Sort by recently updated, recently added, or name
- Copy prompt text with one click
- Export and import a JSON backup
- Responsive layout for desktop and mobile
- Basic offline app-shell support through a service worker

## Run it

This is a static web app. Open `index.html` in a browser, or serve the repository with any static web server. For the installable/offline PWA features, serve it over HTTPS (GitHub Pages works) or localhost.

## Your data and privacy

PromptVault currently saves prompts in the browser's `localStorage`. There is no account, backend, or cloud sync in this first version. Your prompts stay in the browser profile on the device where you use the app. Clearing browser site data can erase them, so export a JSON backup regularly if your prompts matter.

## Built with

HTML, CSS, and vanilla JavaScript. No framework or paid API is required.


## Android APK

The Android app is a lightweight native WebView wrapper around the same PromptVault interface. Its prompts are stored separately from the browser website, inside the app's WebView storage. Use **Export backup** in the website and **Import backup** in the APK to move prompts between them.

A free GitHub Actions workflow builds a debug APK:
1. Open the repository's **Actions** tab.
2. Select **Build PromptVault APK**.
3. Run the workflow, or push a change under `android/`.
4. Open the completed run and download the `PromptVault-debug-apk` artifact.
5. Extract the ZIP to get `app-debug.apk`.

The APK is currently an unsigned debug build for personal testing, not a Play Store release.
