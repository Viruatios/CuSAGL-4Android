# Simplified Automatic Genshin Lyre - CuSAGL-4Android / CuSAGL Mobile

英语版本为翻译，实际含义以中文版本为准。

The English version is a translation, and the actual meaning is based on the Chinese version.

Android port of [CuSimpAutoGenshinLyre](https://github.com/Viruatios/CuSimpAutoGenshinLyre).

---

## Usage

### Intended Use

This app is used to perform on tap-based lyre interfaces on Android devices, such as Genshin Impact's Windsong Lyre and similar in-game instruments.

### First-Time Setup

1. Make sure your device meets the following requirements:
    - Android 12 or later.
    - Genshin Impact or Genshin Impact Cloud is installed.
    - The game can stably maintain at least 30 FPS and can normally enter the in-game lyre interface.
2. Install and open the app.
3. Follow the prompts on the home screen to enable the overlay permission and the accessibility service:
   - The overlay permission is used to show the playback control panel above the game.
   - The accessibility service is used to send touch gestures to the in-game lyre key positions.
   - Optional: network access permission is used to check for and download updates.
   - Optional: install unknown apps permission is used to install updated APK versions from inside the app.

### Basic Flow

1. Import `.json` scores in "Score management", or use "Create score" to create a new score manually.
2. In "Playback configuration", choose options such as playback mode, single score or queue, repeat count, queue interval, and repeat interval.
3. Return to the home screen and use "Preload scores" according to the current configuration. Preloading parses scores into playback timeline caches to avoid stuttering caused by just-in-time calculation during actual performance.
4. When the score cache, overlay permission, and accessibility service are all ready, tap "Prepare playback".
5. Switch to the in-game lyre interface and control playback through the playback control panel:
   - "Start" and "Pause" control the playback state of the current score.
   - "Exit" ends the current playback session and closes the playback control panel.
   - "Previous" and "Next" switch between scores in the preloaded queue.

The playback control panel can be dragged while idle, paused, or stopped. Its position is locked during playback to prevent accidental touches from affecting the performance.

***Important: During performance, keep the game in the lyre interface. If you need to leave the lyre interface, "Pause" or "Exit" the performance first.***

### Score and Cache Storage

- Imported scores are saved in the app-private directory `filesDir/score_file`, and filenames are normalized to the form `####.score-name.json`.
- Preload caches are saved in the app-private directory `filesDir/cache`. When a score is overwritten or deleted, the corresponding cache is cleaned up as well.
- Playback configuration is saved in the app-private directory `filesDir/playback_config.json`.

---

## Development and Build Configuration

- Build with the Gradle Wrapper in this repository: run `gradlew.bat` on Windows, or `./gradlew` on other platforms.
- The current Android Gradle Plugin version is `9.2.1`, the Kotlin Compose plugin version is `2.2.10`, and the Compose BOM version is `2026.05.01`.
- A local Android SDK is required. The SDK path is provided by the local `local.properties` file.
- Recommended verification command: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

## Score Writing Guide

The Android edition uses the keyboard-score format from the original JS script, and strictly validates scores during import and manual creation. Score files must be UTF-8 encoded JSON.

### Basic JSON Structure

```json
{
  "name": "Sample Score Name",
  "author": "Transcriber",
  "instrument": "Windsong Lyre",
  "description": "Score description",
  "type": "keyboard",
  "bpm": 120,
  "time_signature": "4/4",
  "composer": "Composer",
  "arranger": "Arranger",
  "notes": "Q W / E R\n(AD) S / D F"
}
```

### Field Descriptions

- `name`: Required. The score name. It cannot be empty.
- `bpm`: Required. A positive integer representing beats per minute.
- `time_signature`: Required. Format: `N/D`, such as `4/4`, `3/4`, or `6/8`; the denominator must be a power of 2.
- `notes`: Required. The keyboard-score body. It cannot be empty, and it must parse into at least one valid note or rest unit.
- `type`: Recommended to keep as `keyboard`, indicating the keyboard-score format.
- `author`, `instrument`, `description`, `composer`, `arranger`: Optional fields used to display score information. The app uses default text when they are omitted.

### Key Range

Scores use 21 keyboard-score letters, corresponding to the three rows of in-game lyre keys:

```text
Q W E R T Y U
A S D F G H J
Z X C V B N M
```

Lowercase letters are treated as uppercase. Invalid content other than parentheses, spaces, and `/` does not produce valid key presses. When writing scores, it is recommended to use only the keys and rule symbols above to avoid misinterpretation.

### Score Reading Rules

1. Each line in `notes` (between two `\n` characters) is treated as one measure.
2. `/` is the stop symbol for one beat. Multiple `/` characters can split a line into multiple beats. The current parser counts both sides of `/` toward the number of beats in the measure, so keeping a trailing `/` at the end of a line produces an empty trailing beat. If that trailing empty beat is not needed, the final `/` can be omitted.
3. A space is the stop symbol for a basic unit. Basic units separated by spaces within the same beat divide that beat's duration evenly.
4. Two consecutive spaces produce one rest unit. A leading space at the beginning of a line can also represent a rest at the start of that beat.
5. A single letter represents one key, such as `Q`.
6. Multiple letters inside parentheses represent a chord, pressed simultaneously, such as `(QW)`.
7. Multiple note blocks written consecutively without spaces represent an arpeggio or consecutive key presses. They are further subdivided within the duration of that basic unit and played in order. For example, `QWE` is played as `Q W E`, and `(QW)E(RT)` is played as `(QW) E (RT)`.
8. Each basic unit must wait until the previous basic unit has ended before it starts. At runtime, a minimum key-up gap is also preserved to reduce cases where repeated key presses are swallowed by the game.

### Beats and Durations

- `bpm` determines the base counting speed.
- The denominator of `time_signature` determines the beat basis. For example, `4/4` uses a quarter note as one beat, while `6/8` is handled according to compound-meter rules.
- Within each beat separated by `/`, all basic units evenly divide that beat's duration.

Examples:

```text
Q W E R
```

With `4/4` and `bpm = 120`, this beat is split into 4 basic units, and `Q`, `W`, `E`, and `R` are played in order.

```text
Q W / E R
```

This line contains two beats: in the first beat, `Q` and `W` each take half a beat; in the second beat, `E` and `R` each take half a beat.

```text
Q  W
```

There are two spaces between `Q` and `W`, which means there is one rest unit after `Q`. This beat is split into three basic units: `Q`, a rest, and `W`.

```text
(QW)E(RT)
```

This is one consecutive-key basic unit. It contains the chord `(QW)`, the single key `E`, and the chord `(RT)`, which are further evenly divided and played within the duration of this basic unit.

### Minimal Usable Example

```json
{
  "name": "Twinkle Twinkle Fragment",
  "type": "keyboard",
  "bpm": 120,
  "time_signature": "4/4",
  "notes": "Q Q / G G\nH H / G"
}
```

---

## Third-Party Content Notice

- The app icon comes from the "Windsong Lyre" in Genshin Impact, designed by HoYoverse, with copyright owned by HoYoverse. This icon is used only for demonstration and testing purposes in this project and is not used for any commercial purpose.
- This project was ported and rewritten from CuSimpAutoGenshinLyre. The original project was a JS script running on BetterGI, and its development referenced AutoYuanQin by @提瓦特钓鱼玳师 and @半江残秋. After the code port was completed, the original JS script project was removed from this project.

---
