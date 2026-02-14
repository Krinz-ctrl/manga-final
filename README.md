# Manga View

A modern, privacy-focused manga reader for Android built with Jetpack Compose.

## 🚀 Features

*   **Import from Anywhere**: Easily import manga from folders or `.zip`/`.cbz` archives on your device.
*   **Secure Storage**: Imported manga is copied to private internal storage, so you can safely delete the original files without losing your library.
*   **Smart Thumbnails**: Automatically generates cover thumbnails from the first page of your manga.
*   **Immersive Reader**: Distraction-free reading experience. The page indicator and controls hide automatically when you scroll down to read.
*   **Persistent Progress**: Tracks where you left off.
*   **Modern UI**: Built with Material Design 3 and smooth animations.

## 📥 Download

Get the latest version of the app here:

[**Download Latest APK**](https://github.com/Krinz-ctrl/manga-final/releases/latest)

## 📖 How it Works

### 1. Import Your Manga
Tap the **+** button on the home screen to open the system file picker. You can select:
*   **Performance Mode (Folders)**: Select a folder containing image files (`.jpg`, `.png`, `.webp`). The app will copy these images into its private secure storage.
*   **Archive Mode (.zip/.cbz)**: Select a compressed archive. The app will encrypt and import it securely.

### 2. Private Storage
Once imported, your manga is safely stored within the app's private directory. You can **delete the original files** from your phone's storage to save space, and your library will remain intact.

### 3. Immersive Reading
*   **Smart Indicator**: A page number indicator appears at the top when you scroll up or tap the screen.
*   **Focus Mode**: As you read (scroll down), all UI elements—including the page indicator—automatically hide to provide a distraction-free experience.
*   **Progress Tracking**: The app automatically remembers the last page you read for every manga.

## 🛠️ Tech Stack

*   **Kotlin** & **Jetpack Compose**
*   **MVVM Architecture**
*   **Coroutines & Flow** for async operations
*   **Coil** for efficient image loading
*   **Encrypted Storage** for archives
*   **Scoped Storage** best practices

## 📸 Screenshots

*(Add screenshots here)*
