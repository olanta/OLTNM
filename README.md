# OLTNM

[![CI](https://github.com/olanta/OLTNM/actions/workflows/ci.yml/badge.svg)](https://github.com/olanta/OLTNM/actions/workflows/ci.yml)

OLTN Mobile is the native Android client for **OLTN**, a Mastodon/fediverse
client. OLTNM talks directly to the user's instance; there is no OLTN backend.

> Status: early development. The core client, editable timelines, composer and
> Newsletter are usable, but feature parity with the OLTN web client is still in
> progress.

## Features

- OAuth sign-in with instance discovery and a preview before the browser handoff.
- Editable timeline hub: Home, Local, Federated, remote instance local/federated
  timelines, hashtags, lists, bookmarks, favourites and mentions.
- Notifications, search and profiles.
- Composer: text, content warning, visibility, sensitive media, up to four
  attachments with alt text, polls, quote policy, scheduling, local drafts,
  automatic threads, fenced-code-to-image, whole-draft text-to-image,
  translation, Unicode/instance emojis and GIPHY.
- Newsletter with trending links and configurable news sources.
- Trending, Lists and Scheduled posts.
- Dark-first OLTN theme with light/system modes and five accent colours.
- Accessibility settings: font family (Space Grotesk, Atkinson Hyperlegible,
  system, serif or monospace), text size and text colour.
- English and Brazilian Portuguese, switchable in Settings → Appearance.
- No analytics, no tracking, no external scripts.

## Requirements

- JDK 17 or newer.
- Android SDK with platform 36 and build-tools 36.0.0, plus platform-tools.
- Android Studio (recommended) or the SDK command-line tools.
- `make` for the shortcuts in the `Makefile`.
- An Android device or emulator running API 26 or newer.

On Windows, the Makefile detects Android Studio's SDK and bundled JDK. If yours
are in a custom location, set `ANDROID_HOME` and `JAVA_HOME` (or `sdk.dir` in
`local.properties`). The Gradle wrapper is included; a global Gradle install is
not required.

## Quick start

```bash
make doctor      # show SDK, Java, AVD and adb versions
make sdk         # install platform 36 and build-tools 36.0.0 if missing
make build       # debug APK
make test        # JVM unit tests
```

To run on a device or emulator:

```bash
make run         # install and launch on the connected device
```

Or start an emulator and do everything in one command:

```bash
make emulator    # starts the first existing AVD (or AVD=<name>)
make run-emu     # start emulator, wait, install and launch
```

`make help` lists every target. `make validate` runs the build, unit tests,
ktlint and Android lint without a device.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
The latest signed release is `dist/OLTNM-1.2.0.apk` (SHA-256 alongside).
Sideload it, or run:

```bash
adb install -r dist/OLTNM-1.2.0.apk
```

The release package is `app.oltnm`, so it can be installed alongside the debug
build (`app.oltnm.debug`).

## Versioning

OLTNM follows [Semantic Versioning 2.0.0](https://semver.org/). `versionName`
is the SemVer source of truth; `versionCode` is a separate monotonically
increasing Android update counter. The current release is **1.2.0**
(`versionCode = 3`).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for requirements, setup, architecture,
conventions and step-by-step recipes.

## License

MIT. See [LICENSE](LICENSE).
