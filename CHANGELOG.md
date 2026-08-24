# Changelog — SnapDoc

> All notable changes to this project are documented here.
> Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

### ✨ Added
- Document archive with full-text search as its own `feature/library` slice: every scan that yields readable text is kept in Room together with its OCR text and what the app understood from it, and the library screen searches the recognized text itself — so a receipt is found by typing the shop that printed it. Search runs over an external-content FTS4 index, which keeps the index out of the row rather than storing the page text twice
- A scan enters the archive the moment the camera produces it, not once someone opens it: capturing a photo or finishing a guided scan is enough. Reading it later fills in the text and the insight on the same entry, and the library says which scans have not been read yet
- A scan is identified by the pages it is made of, so reading the same scan again updates its entry instead of duplicating it — which is also how an insight upgraded by a newly downloaded model reaches the archive. The update preserves the entry's id and the time the scan was taken, so re-reading an old document does not jump it to the top of the archive
- On-device document understanding with Gemini Nano through ML Kit's Prompt API: the recognized text is classified (receipt, invoice, ID, note) and its merchant, date and total are pulled out, all without the document ever leaving the phone. Where the model is not available the app answers with text rules instead, and the screen always says which of the two produced the answer
- The model can be downloaded from the screen when the device supports it but has not fetched it yet, with progress shown; once it arrives the document is read again, which is the moment the answer visibly improves
- Live text detection in the viewfinder: `ImageAnalysis` with `STRATEGY_KEEP_ONLY_LATEST` reads frames with ML Kit while the preview is open and reports how much text is in view, so the document can be framed before the shot. Frames are analyzed one at a time on a dedicated thread and the pending frame is released only once the recognizer finishes, which is what keeps the pipeline from falling behind. The hint is debounced over three agreeing frames so a passing hand or a focus hunt does not make it flicker, and it can be switched off to save the cost of continuous inference
- On-device OCR with ML Kit Text Recognition v2 as its own `feature/ocr` slice: the recognized text is shown per page, is selectable, and can be copied to the clipboard; a document with no readable text is reported as empty rather than as a failure, and a recognition failure can be retried
- "Extract text" button on the camera screen, shown once a capture or a scan is on disk, which opens the recognized text for those pages
- Guided document scanning with ML Kit: edge detection, cropping, perspective correction and multi-page scans (up to 10 pages, gallery import allowed) run in the Play services scanner, and every returned page is copied into the app's own storage
- Live camera preview with CameraX and photo capture: the granted-permission branch now streams the viewfinder and saves each shot as a JPEG in the app's internal storage (`filesDir/scans/`)
- Capture feedback tells the two failure modes apart — the camera refusing to take the photo and the photo failing to be written to disk — and the camera errors are logged under the `SnapDocCamera` tag
- Camera permission flow as a full vertical slice (`feature/camera`): rationale and permanently-denied screens, automatic first request, and a shortcut to the system app settings
- "Scan" button on the Home screen that opens the camera permission flow
- `CAMERA` permission and camera hardware feature declared in the manifest

### 🐛 Fixed
- A multi-page import that failed halfway left the pages it had already written on disk, unreachable and taking up internal storage forever — nothing references a page on its own, only a whole document. The import is now all-or-nothing: a failure deletes what that run wrote, and a delete that itself fails no longer replaces the error that caused the rollback

### ♻️ Changed
- `DocumentInsight` promoted from `feature/ocr` to `core/document/`: the OCR slice produces one and the library slice stores one, which is the two-feature threshold `AGENTS.md` sets for promoting to `core/`
- `AGENTS.md` records one explicit exception to the "stable versions only" rule, for `com.google.mlkit:genai-*`: it has no stable release and neither does any other on-device Gemini Nano path, so the alternative was no generative AI at all. The rules fallback is what contains the risk
- The insight section moved out of `DocumentTextScreen.kt` into `DocumentInsightSection.kt`
- `CameraPreviewScreen.kt` split in two: the composable tree the user sees stays there, and everything that talks to CameraX, Play services or ML Kit moved to `CameraPreviewPlatform.kt`
- A capture and a scan now clear each other, so the status line and "Extract text" always describe the last thing produced; before, a photo taken after a scan still showed "N pages scanned"
- Camera permission screen re-checks the permission when it resumes, so granting it from the system settings no longer leaves the user stuck on the denied screen
- Permission rationale now reads the host activity from `LocalActivity` instead of casting the composition-local context, which could silently fail and skip the rationale step
- `CameraPermissionViewModel` depends on a use case instead of the repository, per the project's dependency rule
- Home screen restructured around a `Scaffold` with a stateless content composable
- Hilt Compose integration migrated from `hilt-navigation-compose` to `hilt-lifecycle-viewmodel-compose`, the artifact that provides `hiltViewModel()` under Navigation 3
- `androidx.lifecycle.ViewModel` marked stable once at the base class in `compose_stability.conf`, so every ViewModel is covered without per-class entries
- `AGENTS.md`: implementation classes may no longer use the `Impl` suffix; they are named after what backs them

### 🗑️ Removed
- Unused scaffolding colors (`purple_*`, `teal_*`, `black`, `white`) and the now-empty `res/values/colors.xml`

### 🏗️ Bootstrap
- Project bootstrapped from [ScaffoldingAndroidCompose](https://github.com/hacybeyker/ScaffoldingAndroidCompose)
