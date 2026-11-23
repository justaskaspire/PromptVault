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
