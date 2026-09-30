# TPlayer for Android

A fully offline Android music player built with **Kotlin**, **Jetpack Compose**, **Media3 ExoPlayer**, **Room**, and **Hilt**.

## Features

- **Two source modes**
  - Default: Internal storage `/Music` via MediaStore
  - Custom folder: Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) with recursive scan
- **Metadata extraction**: Title, Artist, Album, Year, Track number, Composer + embedded album art (cached)
- **Browse by**: Artists, Years, Folders, All Tracks
- **Sorting**: Alphabetical (default), Track number, Year
- **Sequential playback** inside the chosen list (artist / album / folder / all)
- **Now Playing screen**
  - Scrolling title (falls back to filename)
  - Album artwork (generic placeholder when missing)
  - Circular finger gesture on artwork → seek forward/backward
  - Seek bar
  - Controls: Previous | Rewind | Play/Pause | Stop | Fast-forward | Next
  - Fast-forward / Rewind cycle speeds: 2× → 4× → 8× → 16× → normal
  - Pressing Play while at high speed returns to 1×
- Background playback via Media3 `MediaSessionService` (notification / lock-screen / Bluetooth)

## Requirements

- Android Studio Ladybug / Hedgehog or newer
- JDK 17
- minSdk 26, targetSdk 35

## How to open & run

1. Unzip / clone this project.
2. Open the project folder in Android Studio.
3. Let Gradle sync (first time may download dependencies).
4. Run on a device or emulator (API 26+).

**Permissions**: The app will request `READ_MEDIA_AUDIO` (Android 13+) or storage permission on older versions. For custom folders the system folder picker grants persistent access.

## Project structure

```
app/src/main/java/com/grok/tplayer/
├── data/
│   ├── db/           Room database + DAO
│   ├── model/        Track, AlbumArt, SourceFolder
│   ├── repository/   MusicRepository
│   └── scanner/      LibraryScanner + MetadataExtractor
├── di/               Hilt modules
├── player/           MusicService + PlayerController
├── ui/
│   ├── navigation/
│   ├── screens/
│   │   ├── source/   Source picker + scan progress
│   │   ├── browse/   Artists / Years / Folders / Track lists
│   │   └── player/   Now Playing (artwork + gestures + controls)
│   └── theme/
└── MainActivity.kt
```

## Notes & possible improvements

- **Multiple embedded images**: `MediaMetadataRetriever` only returns one picture. To support swiping between several APIC frames, integrate a TagLib Android binding (e.g. Kyant0/taglib) and store extra arts in the `album_arts` table.
- **True reverse playback**: ExoPlayer does not natively play backwards. The current Rewind button cycles speed; a production version can use a timer that repeatedly seeks backwards while the button is held or while in rewind mode.
- **Marquee title**: Add `Modifier.basicMarquee()` (Compose 1.7+) for long titles.
- **Equalizer / gapless / shuffle / repeat**: Easy to add on top of the existing Media3 player.
- **Android Auto / widgets**: MediaSession already provides the foundation.

## License

This starter is provided as-is for learning and further development.
