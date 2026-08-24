# AGENTS.md — SnapDoc

> This file follows the [agents.md](https://agents.md/) standard and is the **Source of Truth** for AI agents working on this **native Android + Jetpack Compose** project.

---

## 🛠️ Tech Stack & Source of Truth

> [!IMPORTANT]
> Do not assume versions. **ALWAYS** check `gradle/libs.versions.toml` for dependency and plugin versions.

| Category | Standard |
|-----------|----------|
| **UI** | Jetpack Compose + Material Design 3 |
| **Architecture** | Vertical Slice Architecture (feature-first) + Clean dependency rule + MVI |
| **Navigation** | Navigation 3 with type-safe `NavKey` (`@Serializable`) |
| **DI** | Hilt (one Hilt module per feature, `@Binds` to domain interfaces) |
| **Local persistence** | Room for structured data + DataStore for preferences (add to the catalog when needed) |
| **Concurrency** | Kotlin Coroutines + Flow (`StateFlow` in ViewModels, injected dispatchers) |
| **Testing** | JUnit + Turbine + Fakes (JVM unit tests) / Screenshot Testing (UI) |
| **Principles** | SOLID + Design Patterns (Repository, Factory, Observer, etc.) |
| **Quality** | ktlint + detekt + Android Lint |

---

## 📁 Project Layout

```
app/src/main/java/com/hacybeyker/snapdoc/
├── core/                  # Shared ONLY if ≥2 features need it (YAGNI)
│   └── ui/theme/          #   design tokens (Color, Type, Shape, Spacing, Theme)
├── navigation/            # NavKeys @Serializable + AppNavHost (Navigation 3)
└── feature/<name>/        # one Vertical Slice per business capability
    ├── domain/            #   models, usecases, repo interfaces. PURE Kotlin.
    ├── data/               #   sources + mappers + repo impls + feature's Hilt module
    └── ui/                #   Screen/Content Compose, MVI ViewModel, UiState/Intents
```

- **Base package**: `com.hacybeyker.snapdoc`
- **Build**: `./gradlew assembleDebug` · **Tests**: `./gradlew test`
- **Quality (mandatory before commit)**: `./gradlew formatAndAnalyze` (ktlint + detekt + Android Lint)

---

## 🏗️ Project Architecture Rules

### 1. Vertical Slice + Unidirectional Dependencies
Package **by feature**, never by technical layer. Inside each slice: `ui → domain ← data`.
- **Domain**: pure Kotlin. No dependency on frameworks allowed (Android, Compose, Room, Hilt — only `javax.inject`).
- **ViewModels**: inject UseCases. **Forbidden** to inject Repositories directly.
- **A feature never imports another feature's internals**: collaboration happens via `core/` or domain contracts.
- **Promote to `core/` only when ≥2 features need it** (YAGNI).

### 2. State management (MVI)
Every screen is driven by an immutable state:
- **UiState**: `sealed interface` with explicit states (`Loading / Empty / Content / Error`), exposed as a single `StateFlow`.
- **Intent**: `sealed interface` for user actions (enter through `onIntent()`).
- The UI **reacts** to the Flows (SSOT); it never refreshes manually or keeps parallel caches.

### 3. Coding Standards
- **Composables**: PascalCase. Separate `Screen` (stateful) from `Content` (stateless). `@Preview` only on Content.
- **Strings**: hardcoding is forbidden. Use `stringResource(R.string.*)`.
- **Styling**: hardcoding colors/dp/sp in Composables is forbidden. Use the tokens from `core/ui/theme/` (`MaterialTheme.colorScheme/typography/shapes/spacing`).
- **Imports**: wildcard imports (`import x.*`) and trailing commas are forbidden (enforced by ktlint at build time).
- **Dependencies**: always in `gradle/libs.versions.toml` with `version.ref`; **stable** versions only.
- **Naming**: never suffix a class with `Impl` (`XxxRepositoryImpl`) — it's a meaningless label that says nothing about the implementation. Name it after what backs it instead (`AndroidCameraPermissionRepository`, `RoomBookRepository`, `InMemoryCache...`); this also parallels test doubles named `Fake...`/`Stub...`.

### 4. SOLID & Design Patterns
- **SRP**: one class, one responsibility (small UseCases, one Mapper per transformation).
- **DIP**: upper layers depend on abstractions (Repository interfaces in domain, `@Binds` in data).
- **OCP/ISP/LSP**: prefer `sealed interface` and composition over inheritance; small repositories per feature; test Fakes honor the contract.
- Apply patterns where they add clarity (Repository, Factory, Strategy, Observer via `Flow`), never for fashion's sake.

---

## 🚀 AI Interaction Workflow

Any AI agent working on this project **MUST** follow these master guides:

1. **Architecture & Workflow**: [ARCHITECTURE_AND_WORKFLOW.md](.agents/skills/android-best-practices/references/ARCHITECTURE_AND_WORKFLOW.md)
2. **UI & Styling Guide**: [UI_AND_STYLING_GUIDE.md](.agents/skills/android-best-practices/references/UI_AND_STYLING_GUIDE.md)
3. **Quality & Testing**: [TESTING_STRATEGIES.md](.agents/skills/android-best-practices/references/TESTING_STRATEGIES.md)
4. **Mobile Security**: [MOBILE_SECURITY_GUIDE.md](.agents/skills/android-best-practices/references/MOBILE_SECURITY_GUIDE.md)

To implement a **feature / issue / bug / enhancement / fix / refactor** end to end, follow the [feature-implementation](.agents/skills/feature-implementation/SKILL.md) skill workflow (phases: context → snapshot → implementation → DoD → report).

To **review implemented code** (code review against this file's standards), follow the [code-reviewer](.agents/skills/code-reviewer/SKILL.md) skill workflow (phases: scope → checklist → automated verification → severity report).

---

## 📝 Workflow per change (summary)

1. **Locate the feature, not the layer** (new capability → new `feature/<name>/`).
2. **Domain first**: model + usecase + interface, with unit tests.
3. **Slice data layer**: sources + mapper + repo impl + Hilt module.
4. **Slice UI layer**: UiState/Intents + ViewModel + Screen/Content; register the NavKey in `AppNavHost`.
5. **Tests**: unit tests for new logic; screenshot test if there's relevant visual UI.
6. **Verify**: `./gradlew formatAndAnalyze` and `./gradlew test` passing.
7. **Document**: entry in `CHANGELOG.md` under `[Unreleased]` (`Added/Fixed/Changed/Enhancement/Security`).

**Commits — group by functional unit, not by file.** Typically by layer (`domain` / `data` / `ui`) or sub-goal; each commit builds and passes quality checks + tests. One feature/fix at a time.

---

## 🚫 Critical Prohibitions (Hard Rules)
- ❌ **DO NOT** skip the domain layer (UseCases).
- ❌ **DO NOT** expose Entities/DTOs outside the data layer (always map to Domain).
- ❌ **DO NOT** implement business logic in Composables.
- ❌ **DO NOT** use `@Preview` on Screen functions (only on Content with fakes).
- ❌ **DO NOT** use `fallbackToDestructiveMigration` in production code (versioned Room migrations).
- ❌ **DO NOT** hardcode secrets (API keys, tokens, passwords) or store credentials in plain text. See the [Security Guide](.agents/skills/android-best-practices/references/MOBILE_SECURITY_GUIDE.md).
- ❌ **DO NOT** add `alpha/beta/rc/snapshot` dependencies to the catalog. **One documented exception:** `com.google.mlkit:genai-*`, which has no stable release at all — every on-device Gemini Nano path is pre-release (AICore itself is `0.0.1-exp02`), so the choice is a beta artifact or no on-device generative AI. It is contained by design: `GenerateDocumentInsightUseCase` falls back to rules whenever the model is missing **or fails**, so the app behaves identically on a device where the dependency does nothing. Adding any other pre-release dependency still needs its own explicit decision.

---
**Standard Android Config** — SnapDoc
