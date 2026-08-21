# Changelog — SnapDoc

> All notable changes to this project are documented here.
> Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

### ✨ Added
- Camera permission flow as a full vertical slice (`feature/camera`): rationale and permanently-denied screens, automatic first request, and a shortcut to the system app settings
- "Scan" button on the Home screen that opens the camera permission flow
- `CAMERA` permission and camera hardware feature declared in the manifest

### ♻️ Changed
- Home screen restructured around a `Scaffold` with a stateless content composable
- Hilt Compose integration migrated from `hilt-navigation-compose` to `hilt-lifecycle-viewmodel-compose`, the artifact that provides `hiltViewModel()` under Navigation 3
- `androidx.lifecycle.ViewModel` marked stable once at the base class in `compose_stability.conf`, so every ViewModel is covered without per-class entries
- `AGENTS.md`: implementation classes may no longer use the `Impl` suffix; they are named after what backs them

### 🗑️ Removed
- Unused scaffolding colors (`purple_*`, `teal_*`, `black`, `white`) and the now-empty `res/values/colors.xml`

### 🏗️ Bootstrap
- Project bootstrapped from [ScaffoldingAndroidCompose](https://github.com/hacybeyker/ScaffoldingAndroidCompose)
