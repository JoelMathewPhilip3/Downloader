# Joel Video Downloader

Native Android test app that shows a floating download button when Brave Browser reports active media playback.

## What works

- Detects active Brave media sessions using Android notification/media-session access.
- Shows a movable overlay download button while Brave reports `STATE_PLAYING`.
- Accepts links through Brave's Share menu.
- Reads a copied URL only when you tap Paste or the floating button.
- Downloads MP4 at a selected maximum resolution.
- Downloads MP3 at a fixed 192 kbps.
- For MP3, asks yt-dlp/FFmpeg to embed the thumbnail and metadata.
- Saves files to `Downloads/JoelDownloader`.

## Important limitation

Android/Brave normally does not expose the active browser tab URL to another app. The overlay can detect playback, but you still need to use **Share** or **Copy link** in Brave. If no copied/shared link exists, the popup asks for one.

## Build in Android Studio

1. Install Android Studio with Android SDK 35 and JDK 17.
2. Open this project folder.
3. Allow Gradle sync to finish.
4. Run the `app` configuration on an Android 8.0+ device.

## Build on GitHub

1. Upload the complete project to a GitHub repository.
2. Open **Actions**.
3. Run **Build APK**, or push to `main`.
4. Download the `JoelVideoDownloader-debug` artifact.

## First-time setup on the phone

1. Open Joel Downloader.
2. Grant **Display over other apps**.
3. Grant **Notification access**.
4. Enable **Show button when Brave reports playback**.
5. Start a video in Brave.
6. In Brave, use **Share** → Joel Downloader, or **Copy link** and tap the floating button.

## Supported Brave packages

- Stable: `com.brave.browser`
- Beta: `com.brave.browser_beta`
- Nightly: `com.brave.browser_nightly`

## Troubleshooting

- **No floating button:** Confirm Brave has a media notification/Android media session, both permissions are enabled, and the switch is on.
- **Popup has the wrong link:** Clipboard access is intentionally only checked when you tap. Copy the current page link again.
- **YouTube extractor error:** yt-dlp changes frequently. Update the youtubedl-android dependency or add an engine update screen later.
- **Some sites fail:** DRM, authenticated streams, expiring URLs, and unsupported extractors are not bypassed.

## Legal use

Use only for material you own, public-domain media, or media you are authorized to download. This project does not bypass DRM or paid access controls.

## License

GPL-3.0 because the bundled youtubedl-android/FFmpeg components are GPL-licensed.

### Gradle wrapper note

The small binary `gradle-wrapper.jar` is intentionally not bundled in this generated archive. The included `gradlew`/`gradlew.bat` downloads the official Gradle 8.9 wrapper JAR from Gradle's tagged GitHub source on first command-line build. GitHub Actions uses the installed Gradle runner directly. Android Studio can also regenerate it with `gradle wrapper --gradle-version 8.9` if required.
