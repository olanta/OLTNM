# Contributing to OLTNM

Thanks for helping improve OLTNM. This guide covers everything needed to go from
a clean machine to a running debug build, plus the conventions and recipes used
by the project.

OLTNM is a **native Android client** for OLTN/Mastodon. It talks directly to the
user's instance. There is no OLTN backend, no relay, no analytics and no
tracking.

## 1. Requirements

| Requirement | Version / notes |
| --- | --- |
| JDK | 17 or newer. Android Studio's bundled JDK works. |
| Android SDK | Platform 36 and build-tools 36.0.0. |
| Platform tools | `adb` for installing and launching. |
| Build tool | Gradle wrapper is included; no global Gradle needed. |
| `make` | Optional but recommended; all shortcuts are in the `Makefile`. |
| Device/emulator | API 26 or newer. The project is tested on API 35. |
| Network | Needed for the first Gradle sync and for the app itself. |

On Windows, run `make` from a shell where the recipe can invoke `cmd` (cmd,
PowerShell, Git Bash/MSYS2 or WSL). The Makefile uses `cmd /c gradlew.bat` on
Windows and detects Android Studio's SDK/JDK automatically.

## 2. First-time setup

### 2.1 Check your toolchain

```bash
make doctor
```

This prints `ANDROID_HOME`, `JAVA_HOME`, the Gradle version, `adb` and the list
of AVDs. If `java` or `adb` is missing, fix that before building.

If you are not using the Makefile:

```bash
./gradlew --version
adb version
```

### 2.2 SDK location

The Makefile looks for the Android Studio SDK at
`%LOCALAPPDATA%\Android\Sdk` (Windows) and uses `ANDROID_HOME` when set. For a
custom location, either export `ANDROID_HOME` or create `local.properties`
(ignored by git):

```properties
sdk.dir=C:/Users/you/AppData/Local/Android/Sdk
```

`JAVA_HOME` can point at Android Studio's JBR or any JDK 17+.

### 2.3 Install the SDK packages

```bash
make sdk             # platform-tools, platform 36, build-tools 36.0.0
make emulator-sdk    # emulator + system image, only if you need them
```

On a machine where Android Studio already created an AVD, `make emulator` starts
the first existing AVD. To create the standard `OLTNM` AVD:

```bash
make avd
```

### 2.4 Build and run

```bash
make build           # app/build/outputs/apk/debug/app-debug.apk
make test            # JVM unit tests
make run             # install + launch on a connected device/emulator
```

Or start an emulator first and use the one-shot target:

```bash
make emulator
make run-emu
```

`make devices` lists connected devices. `make validate` runs build + tests +
ktlint + Android lint without a device.

## 3. Commands reference

| Command | What it does |
| --- | --- |
| `make help` | List every target. |
| `make sdk` | Install the SDK 36 platform and build-tools. |
| `make emulator-sdk` | Install the emulator and system image. |
| `make avd` | Create the `OLTNM` AVD. |
| `make emulator` | Start the first existing AVD (or `AVD=<name>`). |
| `make run` | Install and launch on a connected device. |
| `make run-emu` | Start an emulator, wait, install and launch. |
| `make devices` | List connected devices. |
| `make build` | Debug APK. |
| `make build-release` | Minified release APK (unsigned by default). |
| `make test` | JVM unit tests. |
| `make validate` | Build + tests + ktlint + Android lint. |
| `make lint` | ktlint check + Android lint. |
| `make format` | Auto-format Kotlin with ktlint. |
| `make format-check` | Fail if Kotlin sources are not formatted. |
| `make emojis` | Regenerate Unicode emoji JSON from Emojibase. |
| `make clean` | Delete Gradle build outputs. |
| `make doctor` | Show SDK/Java/adb/AVD information. |

Raw Gradle works too:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:ktlintFormat
./gradlew :app:lintDebug
```

## 4. Project tour

```
app/src/main/java/app/oltnm/
  OltnApplication.kt      creates AppContainer
  MainActivity.kt         hosts Compose and handles oltnm://oauth
  data/api/               MastodonApi, pagination, translation, GIPHY, OkHttp
  data/model/             Serializable DTOs and app models
  data/store/             session, preferences, drafts, timelines, newsletter
  data/repo/              AuthRepository
  di/AppContainer.kt      manual dependency container
  ui/OltnApp.kt           theme, routes and bottom navigation
  ui/MoreMenuSheet.kt     OLTN-style "More" navigation sheet
  ui/components/          OLTN primitives, status/media/poll/state views
  ui/screens/             one ViewModel + one screen per feature
  ui/theme/               colours, typography, shapes, semantic tokens
  util/                   HTML text, relative time, threads, code/text images, errors
app/src/test/java/app/oltnm/
scripts/generate-emojis.mjs
```

Key ideas:

- `data/api/MastodonApi` is the only network boundary. Every call is `suspend`
  and runs on `Dispatchers.IO`. Non-2xx responses throw
  `MastodonApiException(statusCode, message)`.
- `api/Pagination.kt` reads `max_id` from the `Link: ... rel="next"` header.
- `api/JsonConfig.kt` and `api/HttpClient.kt` provide one `Json` and one shared
  `OkHttpClient`.
- DTOs live in `data/model`, use camelCase with `@SerialName` for Mastodon's
  snake_case, and keep defaults so partial payloads decode. `Status.original`
  unwraps a boost.
- `data/store/SessionStore` persists the session and pending OAuth client in
  private `SharedPreferences`.
- `data/store/PreferencesStore` holds theme mode, accent colour, app language,
  font family, text size and text colour.
- `data/store/DraftStore`, `TimelineStore` and `NewsletterStore` hold composer
  drafts, the timeline hub and news sources.
- `di/AppContainer` wires the stores, APIs and `AuthRepository` without Hilt.
- Screens use one `ViewModel` per screen, an immutable `UiState` and
  `StateFlow`, collected with `collectAsStateWithLifecycle`.
- `ui/factoryOf {}` builds ViewModels; there is no DI framework.
- Navigation is a small custom back stack (`Route` in `ui/OltnApp.kt`); there is
  no navigation-compose dependency. Bottom tabs are Home, Search, Compose,
  Notifications and More.
- The theme is OLTN's dark-first palette. Use `OltnTopBar`, `OltnGlassCard`,
  `OltnMark`, `OltnControls` and `LocalOltnSemantics` instead of ad-hoc colours.

## 5. OAuth flow

1. `AuthRepository.beginLogin(instance)` normalises the host, registers the app
   with `POST /api/v1/apps` (redirect `oltnm://oauth`), stores the pending client
   and returns the authorize URL.
2. The login screen opens the URL in the system browser.
3. The browser redirects to `oltnm://oauth?code=...`; `MainActivity` forwards
   the code through `util/OAuthBus`.
4. `AuthRepository.completeLogin(code)` exchanges the code at `/oauth/token`,
   calls `verify_credentials`, persists `Session`, and `OltnApp` switches to the
   main scaffold.

Never log tokens or client secrets. Sessions are in app-private prefs; stronger
protection (`EncryptedSharedPreferences` or Keystore) is a valid future change.

If sign-in returns to the app but never completes, check that the manifest
intent filter and `BuildConfig.OAUTH_REDIRECT_URI` both say `oltnm://oauth`.

## 6. Recipes

### 6.1 Add a screen

1. Create `ui/screens/foo/FooViewModel.kt` with an immutable `FooUiState`, a
   `MutableStateFlow` and `viewModelScope`:

   ```kotlin
   data class FooUiState(val loading: Boolean = true)
   class FooViewModel(private val api: MastodonApi) : ViewModel() {
       private val _state = MutableStateFlow(FooUiState())
       val state: StateFlow<FooUiState> = _state.asStateFlow()
   }
   ```

2. Create `ui/screens/foo/FooScreen.kt`. Collect state with
   `collectAsStateWithLifecycle()` and create the ViewModel with:

   ```kotlin
   val viewModel: FooViewModel = viewModel(
       key = "foo-${api.hashCode()}",
       factory = factoryOf { FooViewModel(api) },
   )
   ```

3. Add a `Route.Foo` entry in `ui/OltnApp.kt`, import the screen, handle it in
   the `when` and add an entry to the tab bar or `MoreMenuSheet`.
4. Add user-facing strings to **both** `values/strings.xml` and
   `values-pt-rBR/strings.xml`.
5. Run `make format` and `make validate`.

### 6.2 Add an API endpoint

1. Add a `@Serializable` DTO in `data/model/`. Give every field a default and
   use `@SerialName` for snake_case.
2. Add a `suspend` method to `MastodonApi` using `getJson`, `getRaw`, `postRaw`
   or `execute`. Network errors already become `MastodonApiException`.
3. Use pagination through `Page<T>` and `Pagination.nextMaxId` when the endpoint
   returns a `Link` header.
4. Add a unit test for any pure parsing/pagination logic.
5. Never hit a real instance in tests; use a fake `OkHttpClient` or add
   `MockWebServer` as a `testImplementation` dependency.

### 6.3 Add a timeline type

Timelines are driven by `TimelineSpec(type, host, value, label)` and persisted by
`TimelineStore`.

1. Add a constant to `TimelineTypes` in `data/model/TimelineModels.kt`.
2. Handle the type in `TimelineViewModel.fetch` (`ui/screens/home`).
3. Add the type to `ADD_TYPES` in `ManageTimelinesScreen`.
4. Map its label in `ui/screens/home/TimelineLabels.kt`.
5. Add the required API method (for example a list or hashtag timeline).
6. Test pagination and empty/error states.

Built-in types: Home, Local, Federated, RemoteLocal, RemoteFederated, Hashtag,
List, Bookmarks, Favourites and Mentions. Remote timelines use an unauthenticated
`MastodonApi` for the remote host and hide write actions.

### 6.4 Add a composer option

The composer state is `ComposeUiState` in
`ui/screens/compose/ComposeViewModel.kt`.

1. Add the state field and mutation methods.
2. Include it in the `StatusRequest` built by `post()`.
3. Render the control in `ComposeScreen` using `OltnControls`/`OltnTopBar`.
4. Add strings in both locales.
5. Respect instance limits (`maxChars`, media slots, poll options) and Mastodon
   validation (for example, polls cannot coexist with media).
6. Add a test when the logic is pure.

Existing composer options: text, CW, visibility (including Pleroma/Akkoma
`local`), sensitive media, up to four attachments with alt text, polls, quote
policy, scheduling, drafts, threads, code-to-image, whole-draft text-to-image,
translation, emoji picker and GIPHY.

### 6.5 Add or change strings

`values/strings.xml` is the master dictionary (English). `values-pt-rBR/strings.xml`
must contain the same keys and the same `%1$s`/`%1$d` placeholders. Do not
hard-code user-facing text in composables.

The in-app language switch lives in Settings → Appearance → Language:

- `LanguageChoice` (`System`, `English`, `PortugueseBrazil`) is persisted in
  `PreferencesStore`.
- `MainActivity.attachBaseContext` wraps the Activity context with the chosen
  locale and sets the default locale; changing the language recreates the
  Activity.
- `values-pt-rBR` is the Brazilian Portuguese dictionary; the default `values`
  dictionary is English.
- To add a language: add `values-<tag>/strings.xml` with every key, add a
  `LanguageChoice` entry and its label, and test the switch in Settings.

Keep English as the default `values` dictionary so devices with unsupported
locales still fall back to English.

### 6.6 Generated files

- `app/src/main/res/raw/emojis_en.json` and `emojis_pt.json` are generated from
  Emojibase by `scripts/generate-emojis.mjs`.
- Run `make emojis`. The script reads the OLTN checkout's
  `node_modules/emojibase-data` by default; override with `EMOJIBASE_DIR`.
- Never hand-edit the generated JSON.

## 7. Accessibility and text-to-image

Settings → Appearance exposes:

- **Theme and accent** — dark/light/system and five accent colours.
- **Font** — Space Grotesk (default), Atkinson Hyperlegible (designed for low
  vision), system sans, serif and monospace. Bundled fonts are SIL OFL 1.1;
  see `licenses/`.
- **Text size** — Small, Default, Large and Extra large. The choice multiplies
  the system font scale, so system accessibility settings are still respected.
- **Text colour** — Default, High contrast, Cream, Yellow, Mint, Blue and Pink.
  Non-default choices override `onSurface`, `onBackground` and
  `onSurfaceVariant` in the theme.

Rules:

- Use `MaterialTheme.typography` and `MaterialTheme.colorScheme`; do not
  hard-code font sizes or text colours in components.
- Avoid fixed heights that clip scaled text. Check new UI at Extra large and
  with Atkinson Hyperlegible.
- Keep the system font scale respected: the chosen size is a multiplier, not a
  replacement.
- Keep contrast in mind for custom text colours; the high-contrast option is
  there for users who need it.

### Text-to-image

- Fenced code blocks are converted automatically on publish (`util/CodeImage`).
  The block becomes a dark PNG attachment and the code becomes its alt text.
- The composer toolbar also has **Text as image**: it renders the current draft
  with the selected font, text size, text colour and theme (`util/TextImage`),
  uploads the PNG as media, uses the draft as alt text and clears the draft only
  after a successful upload.
- Text-to-image needs a free media slot and cannot be combined with a poll.
## 8. Testing

- JVM tests live in `app/src/test/java/app/oltnm` and use JUnit 4.
- Current tests cover pure logic: instance normalisation, pagination, relative
  time, HTML text, thread splitting, code-block extraction, the text
  colour/size accessibility choices and the language tags.
- Add tests for formatting, parsing, pagination and publish-plan behavior.
- Network tests must use a fake client or `MockWebServer`; never call a real
  instance.
- Run `make test` or the full `make validate`.

Example:

```kotlin
@Test
fun extractsMaxIdFromNextLink() {
    assertEquals("100", Pagination.nextMaxId("<...max_id=100>; rel=\"next\""))
}
```

## 9. Formatting and lint

- Kotlin official style, 4-space indent, no wildcard imports.
- Settings live in `.editorconfig`; ktlint uses `android_studio` style.
- `make format` auto-formats; `make format-check` only verifies.
- `make lint` runs ktlint and Android lint.
- Do not add comments that restate the code.

## 10. Versioning and releases

OLTNM follows **SemVer 2.0.0**. Use PATCH for fixes, MINOR for features and
MAJOR for breaking changes after `1.0.0`.

- `versionName` is the SemVer source of truth and is shown in Settings → About.
- `versionCode` is a separate monotonically increasing integer.
- Current baseline: `1.0.0` (`versionCode = 1`). Future releases follow SemVer:
  PATCH for fixes, MINOR for features, MAJOR for breaking changes.
- Run `make validate` and `make build-release` before tagging.
- Tag with `vX.Y.Z` and publish release notes on the tag/release.

Release signing reads `.release/keystore.properties` (ignored by git) or the
`OLTNM_KEYSTORE`, `OLTNM_KEYSTORE_PASSWORD`, `OLTNM_KEY_ALIAS` and
`OLTNM_KEY_PASSWORD` environment variables. Back up the keystore and password:
future updates must be signed with the same key. The first signed release is
`dist/OLTNM-1.0.0.apk` with its SHA-256 alongside.

Debug builds use `app.oltnm.debug`; the launcher activity is
`app.oltnm.debug/app.oltnm.MainActivity`. The release package is `app.oltnm`, so
it can be installed alongside the debug build.

## 11. Troubleshooting

| Symptom | Fix |
| --- | --- |
| `SDK location not found` | Run `make sdk`, set `ANDROID_HOME`, or add `sdk.dir` to `local.properties`. |
| `JAVA_HOME is not set` | Install JDK 17+ or point `JAVA_HOME` at Android Studio's JBR. |
| `make: command not found` | Install `make` or use `./gradlew` directly. On Windows, use Git Bash/MSYS2/WSL or a cmd/PowerShell make. |
| `no devices/emulators found` | Start a device (`make emulator` or Android Studio) and check `make devices`. |
| `make emojis` cannot find data | Set `EMOJIBASE_DIR` to an `emojibase-data` directory. |
| OAuth returns but sign-in never completes | Ensure `oltnm://oauth` matches in the manifest and `BuildConfig.OAUTH_REDIRECT_URI`. |
| App opens the wrong package | The debug app is `app.oltnm.debug`, not `app.oltnm`. |
| Stale state after changing session/preferences | Clear app data: `adb shell pm clear app.oltnm.debug`. |
| Build cache confusion | Run `make clean` and rebuild. |
| Release APK cannot be installed | Release builds are unsigned; sign them or use a debug build. |

Useful logs:

```bash
adb logcat | grep -i oltnm
adb logcat -d -t 500
```

## 12. Pull request checklist

- [ ] `make format` ran.
- [ ] `make validate` passes.
- [ ] New pure logic has unit tests.
- [ ] New strings exist in both English and pt-BR, with matching placeholders.
- [ ] No tokens, secrets, signing material or reference checkouts are staged.
- [ ] UI uses OLTN primitives and `LocalOltnSemantics`, not ad-hoc colours.
- [ ] Architecture/commands changed? Update `AGENTS.md` and this guide.
- [ ] UI change? Include screenshots or a short screen recording.
- [ ] Tested at Large/Extra large text and with Atkinson Hyperlegible.
- [ ] Keep the change focused; avoid unrelated refactors.
- [ ] Release-related change? Follow SemVer and bump `versionCode`.

## 13. Security and privacy

- Do not log tokens, client secrets or OAuth codes.
- Sessions are stored in app-private `SharedPreferences`; do not commit them.
- The app has no analytics or tracking. Normal use contacts the instance host.
  The optional composer translation and GIF features additionally contact
  Google's public gtx endpoint and GIPHY.
- `usesCleartextTraffic` is enabled for local development instances; keep it in
  mind when reviewing network changes.
- Only load remote content through Coil and `util/HtmlText`; never render raw
  remote HTML.

## 14. Where to start

Good first contributions are small and verifiable: a missing translation, an
accessibility audit (large text, Atkinson, high-contrast colour), a
layout polish, an empty/error state, a unit test for a pure helper, or a
documentation fix. For larger work, open a focused change with a clear before
and after.

## 15. License

By contributing, you agree that your changes are licensed under the project's
MIT license (see [LICENSE](LICENSE)). Bundled fonts (Space Grotesk and Atkinson
Hyperlegible) are under the SIL OFL 1.1; see `licenses/`.
