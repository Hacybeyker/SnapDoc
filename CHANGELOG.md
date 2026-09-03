# Changelog — SnapDoc

> All notable changes to this project are documented here.
> Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

_Nothing yet._

---

## [1.0.1] — 2026-09-03

A security release. The first one shipped with the scaffolding's backup rules, which quietly made
scanned documents eligible for cloud backup — the one thing this app promises never happens. Also
carries the findings SonarCloud's first analysis turned up, and turns R8 on for release builds.

### 🐛 Fixed
- Scanned pages and the text read off them were eligible for **cloud backup**. The backup rules were the
  scaffolding's samples — an empty `full-backup-content` and a commented-out `data-extraction-rules` — so
  Android's Auto Backup included `filesDir/scans` and the Room database, and someone's receipts and IDs
  could end up in their Google account. That is the one thing the app promises never happens. Cloud backup
  now excludes every domain that can hold document data; device-to-device transfer still carries all of it,
  because moving to a new phone is a handover between two devices the same person is holding
- Workflow actions are pinned by commit SHA instead of by tag, since a tag can be repointed by whoever owns
  the action and these workflows hold repository secrets
- `android:usesCleartextTraffic="false"`: the app has no network layer, and the manifest now says so

### ♻️ Changed
- **R8 is on for release builds**: code shrinking, resource shrinking, optimization and obfuscation, which takes the APK from 55.7 MB to 45.4 MB and the bundle from 30.7 MB to 25.0 MB. One keep rule of our own was needed, and it is not the obvious kind: enum names are *data* here — a document's kind and the engine that read it are stored in Room by name — so renaming those constants would make every row written by an earlier build stop matching, and the mapper would quietly downgrade the whole archive to `Unknown`/`Rules` instead of crashing. Everything else rides on the consumer rules that kotlinx-serialization, Room, Hilt and ML Kit GenAI already ship
- The deprecated `Icons.Filled` arrows, lists and send glyphs move to their `Icons.AutoMirrored` equivalents,
  which is not cosmetic: they now mirror correctly in a right-to-left locale
- `CameraPermissionRepository`, `DocumentTextRecognizer`, `PdfExporter` and `ScannedPageReader` are `fun
  interface`s — each has exactly one method, and saying so lets a test double be a lambda where a class adds nothing
- `RoomDocumentRepository` no longer wraps its suspend DAO calls in `withContext`: Room already runs them on
  its own query executor, so that was one dispatcher hop for nothing. The cold Flows keep their `flowOn`
- `LibraryContent` takes a `LibraryActions` holder instead of five separate callbacks — they always travelled
  together, and eight parameters is where a call site stops being readable

---

## [1.0.0] — 2026-09-03

First stable release: the whole pipeline, from the viewfinder to a searchable archive and a shareable
PDF, running entirely on the device.

### ✨ Added
- MIT licence (`LICENSE`), so the terms the project has always been shared under are actually stated
- Screenshot tests with Roborazzi: 17 goldens covering Home, the archive, the reader and every camera and permission state, committed to the repository and rendered on the JVM so they run in CI without an emulator. They exist because the visual defects this project actually shipped — a scrim stopping short of the screen edge, a scanner frame drawn under the controls, a permission state with no way out — were all invisible to a green test suite. The camera ones render with no surface, which draws the whole chrome and none of the live feed
- Coverage gate with Kover: 90% of the code that can run on the JVM — domain, data and ViewModels — with the platform boundary excluded rather than counted and argued away. It sits at 93% today
- SonarCloud analysis in CI, fed by the reports the local gate already produces (Lint, detekt, ktlint and Kover) instead of a second, divergent analysis. The job fails when the Quality Gate does, and is skipped when no `SONAR_TOKEN` is present so a fork still gets a green build
- Gradle Build Scans published from every CI build, which is what makes a failure that only happens on the runner readable without reproducing it
- Tests for the last two ViewModels that had none: the library's search, delete and PDF export (including that a failed export is an effect and not a screen-wide error), and Home's recent-scans list. The Room mapping is covered too — a round trip through the row, the pages travelling as one column, and a kind or an engine written by a newer build reading back as a fallback instead of crashing on someone's own archive
- Export a scan to PDF and share it: every page is drawn onto an A4 sheet, scaled to fit and centred, and handed to any app through a `FileProvider` URI with a temporary read grant. The file is named after what the document is and when it was taken — `receipt_hardware-store_20260820_1330.pdf` — so it is recognizable in an inbox
- Document archive with full-text search as its own `feature/library` slice: every scan that yields readable text is kept in Room together with its OCR text and what the app understood from it, and the library screen searches the recognized text itself — so a receipt is found by typing the shop that printed it. Search runs over an external-content FTS4 index, which keeps the index out of the row rather than storing the page text twice
- A scan enters the archive the moment the camera produces it, not once someone opens it: capturing a photo or finishing a guided scan is enough. Reading it later fills in the text and the insight on the same entry, and the library says which scans have not been read yet
- A scan is identified by the pages it is made of, so reading the same scan again updates its entry instead of duplicating it — which is also how an insight upgraded by a newly downloaded model reaches the archive. The update preserves the entry's id and the time the scan was taken, so re-reading an old document does not jump it to the top of the archive
- On-device document understanding with Gemini Nano through ML Kit's Prompt API: the recognized text is classified (receipt, invoice, ID, note) and its merchant, date and total are pulled out, all without the document ever leaving the phone. Where the model is not available the app answers with text rules instead, and the screen always says which of the two produced the answer
- The model can be downloaded from the screen when the device supports it but has not fetched it yet, with progress shown; once it arrives the document is read again, which is the moment the answer visibly improves
- Live text detection in the viewfinder: `ImageAnalysis` with `STRATEGY_KEEP_ONLY_LATEST` reads frames with ML Kit while the preview is open and reports how much text is in view, so the document can be framed before the shot. Frames are analyzed one at a time on a dedicated thread and the pending frame is released only once the recognizer finishes, which is what keeps the pipeline from falling behind. The hint is debounced over three agreeing frames so a passing hand or a focus hunt does not make it flicker, and it can be switched off to save the cost of continuous inference
- On-device OCR with ML Kit Text Recognition v2 as its own `feature/ocr` slice: the recognized text is shown per page, is selectable, and can be copied to the clipboard; a document with no readable text is reported as empty rather than as a failure, and a recognition failure can be retried
- The camera hands over on its own: as soon as a photo or a guided scan is saved and filed, the app opens the recognized text for those pages and drops the camera from the back stack, so Back from the reader returns Home rather than a viewfinder aimed at a document that has already been scanned. The hand-off is emitted only after the archive write finishes, since leaving the screen ends the ViewModel that is doing the writing
- A back button on every camera state — over the viewfinder, on the two permission screens and on the "camera unavailable" dead end — where the only way out used to be the system gesture
- Guided document scanning with ML Kit: edge detection, cropping, perspective correction and multi-page scans (up to 10 pages, gallery import allowed) run in the Play services scanner, and every returned page is copied into the app's own storage
- Live camera preview with CameraX and photo capture: the granted-permission branch now streams the viewfinder and saves each shot as a JPEG in the app's internal storage (`filesDir/scans/`)
- Capture feedback tells the two failure modes apart — the camera refusing to take the photo and the photo failing to be written to disk — and the camera errors are logged under the `SnapDocCamera` tag
- Camera permission flow as a full vertical slice (`feature/camera`): rationale and permanently-denied screens, automatic first request, and a shortcut to the system app settings
- "Scan" button on the Home screen that opens the camera permission flow
- `CAMERA` permission and camera hardware feature declared in the manifest

### 🐛 Fixed
- Every screen was padded for the system bars twice: `MainActivity` wrapped the whole nav host in a `Scaffold` and each destination already owns one, so the gutters were doubled and the viewfinder was letterboxed between two black bars instead of filling the screen. The nav host no longer applies the padding, and the camera — the only destination without a `Scaffold` — insets its own bars
- The camera's scrims stopped short of the screen edges, because the insets were applied to the whole overlay column rather than inside each band: a strip of bare camera showed above the status bar and below the controls, leaving the panel looking unanchored
- The scanner frame was drawn at full screen size, so its two bottom brackets sat underneath the controls panel and the frame never looked closed. It is now sized to the camera area the bars leave free
- Sharing a PDF crashed with `Couldn't find meta-data for provider`: the FileProvider authority was built from a string whose `$` had been escaped away, so the literal text `${context.packageName}.fileprovider` was passed instead of the interpolated package name
- A multi-page import that failed halfway left the pages it had already written on disk, unreachable and taking up internal storage forever — nothing references a page on its own, only a whole document. The import is now all-or-nothing: a failure deletes what that run wrote, and a delete that itself fails no longer replaces the error that caused the rollback

### ♻️ Changed
- README rewritten around what the app became rather than what it started as: a capture is not a photo that waits to be processed, it is a document already saved, read and filed. Adds the flow and pipeline diagrams, the architecture and its dependency rule, the key decisions with their reasons, the full command list and the CI, Quality Gate, Build Scan and licence badges
- `AGENTS.md` kept in step with it: the testing standard now names the screenshot goldens and the coverage gate, and the commands section lists the gates a change has to pass
- Android Lint's last two actionable warnings are gone: the activity repeated the label the application already declares, and "Turn on on-device AI" reads better as "Enable on-device AI"
- App icon: the scaffolding robot is replaced by a document with the scan light crossing it, on amber rather than the product blue — a home screen is mostly blue and white icons, and the app you open to photograph something has to be findable in that grid. It is drawn entirely as vectors, including a monochrome layer for themed icons on Android 13+ where the beam and the text lines are cut out of the page instead of drawn on it, since the system supplies a single colour and no tones. The legacy bitmap densities are gone: `minSdk 26` means every launcher gets the adaptive icon
- README rewritten as the project's own: what the app does, the capture → OCR → AI → archive pipeline as a diagram, the on-device AI requirements together with what happens on a device without them, and an explicit note on what the JVM tests cannot cover and why
- Android Lint can now fail the build (`abortOnError = true`). The quality gate ran Lint but ignored its verdict, so a Lint error passed CI silently; warnings still do not fail anything
- Camera screen chrome reworked: live text detection is a real toggle button (filled when on, scrim-backed when off) instead of an `Info` glyph that read as help and whose off state was an invisible tint change on white; both capture buttons show progress while they work rather than only greying out; capture errors get their own container instead of white body text on the scrim, and the result action is gone entirely now that a finished capture leaves for the reader by itself; and the scanner caption moved above the primary button, where it can only describe that one
- The camera permission states are built like every other screen — a `Scaffold` and the app header — instead of a sentence and a button floating in the middle of the screen; a denied permission was a trap with no visible way back
- "Starting" and "camera unavailable" grew from a bare spinner and a lone sentence on black into states that say what is happening — a spinner over black reads as a crash
- `CameraPreviewScreen.kt` split again: the overlay chrome over the viewfinder moved to `CameraPreviewOverlay.kt`, leaving the screen with the state machine and the previews
- `DocumentInsight` promoted from `feature/ocr` to `core/document/`: the OCR slice produces one and the library slice stores one, which is the two-feature threshold `AGENTS.md` sets for promoting to `core/`
- `AGENTS.md` records one explicit exception to the "stable versions only" rule, for `com.google.mlkit:genai-*`: it has no stable release and neither does any other on-device Gemini Nano path, so the alternative was no generative AI at all. The rules fallback is what contains the risk
- The insight section moved out of `DocumentTextScreen.kt` into `DocumentInsightSection.kt`
- `CameraPreviewScreen.kt` split in two: the composable tree the user sees stays there, and everything that talks to CameraX, Play services or ML Kit moved to `CameraPreviewPlatform.kt`
- A capture and a scan now clear each other, so what the camera hands to the reader is always the last thing produced; before, a photo taken after a scan still showed "N pages scanned"
- Camera permission screen re-checks the permission when it resumes, so granting it from the system settings no longer leaves the user stuck on the denied screen
- Permission rationale now reads the host activity from `LocalActivity` instead of casting the composition-local context, which could silently fail and skip the rationale step
- `CameraPermissionViewModel` depends on a use case instead of the repository, per the project's dependency rule
- Home screen restructured around a `Scaffold` with a stateless content composable
- Hilt Compose integration migrated from `hilt-navigation-compose` to `hilt-lifecycle-viewmodel-compose`, the artifact that provides `hiltViewModel()` under Navigation 3
- `androidx.lifecycle.ViewModel` marked stable once at the base class in `compose_stability.conf`, so every ViewModel is covered without per-class entries
- `AGENTS.md`: implementation classes may no longer use the `Impl` suffix; they are named after what backs them

### 🗑️ Removed
- The scaffolding's sample instrumented test, which asserted that the package name is the package name, and the four dependencies that existed only for it. `ui-test-manifest` stays, and now says why in a comment: it is not for instrumented tests but for the screenshot goldens, which need its `ComponentActivity` to launch under Robolectric
- `androidx.room:room-testing` from the version catalog: declared, never used
- Unused scaffolding colors (`purple_*`, `teal_*`, `black`, `white`) and the now-empty `res/values/colors.xml`

### 🏗️ Bootstrap
- Project bootstrapped from [ScaffoldingAndroidCompose](https://github.com/hacybeyker/ScaffoldingAndroidCompose)

---

[Unreleased]: https://github.com/hacybeyker/SnapDoc/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/hacybeyker/SnapDoc/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/hacybeyker/SnapDoc/releases/tag/v1.0.0
