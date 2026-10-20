# Frontend changelog

Release notes for Android, iOS, and Web. Each version includes only platforms with user-visible changes.
History begins with the introduction of shared version tracking in `version.xcconfig`.

## 0.3.2

### Android, iOS, and Web

- Use cacheable game reads and start a background backend wake-up request to reduce loading delays.

## 0.3.1

### Web

- Add previous and next controls to horizontal game and giveaway lists.
- Add a favicon and improve browser startup.

## 0.3.0

### Android and iOS

- Update network loading for game covers and giveaway images while retaining disk caching.

### Web

- Add a Kotlin/Wasm browser app with the shared game and giveaway screens.
- Adapt layouts to the browser viewport and enable loading game covers and giveaway images.

## 0.2.1

### iOS

- Raise the minimum supported iOS version from 14.1 to 15.0.

## 0.2.0

### Android and iOS

- Load giveaway lists and details through the configured backend alongside game data.

## 0.1.0

### Android and iOS

- Begin shared Android/iOS version tracking at version `0.1.0`, build `1`.

### Android

- Remove the `-release` suffix from release version names.
