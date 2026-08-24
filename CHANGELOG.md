# Changelog — SnapDoc

> All notable changes to this project are documented here.
> Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

### ✨ Added
- Guided document scanning with ML Kit: edge detection, cropping, perspective correction and multi-page scans (up to 10 pages, gallery import allowed) run in the Play services scanner, and every returned page is copied into the app's own storage
- Live camera preview with CameraX and photo capture: the granted-permission branch now streams the viewfinder and saves each shot as a JPEG in the app's internal storage (`filesDir/scans/`)
- Capture feedback tells the two failure modes apart — the camera refusing to take the photo and the photo failing to be written to disk — and the camera errors are logged under the `SnapDocCamera` tag
- Camera permission flow as a full vertical slice (`feature/camera`): rationale and permanently-denied screens, automatic first request, and a shortcut to the system app settings
- "Scan" button on the Home screen that opens the camera permission flow
- `CAMERA` permission and camera hardware feature declared in the manifest

### ♻️ Changed
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
