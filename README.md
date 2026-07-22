# SnapDoc

[![CI](https://github.com/hacybeyker/SnapDoc/actions/workflows/ci.yml/badge.svg)](https://github.com/hacybeyker/SnapDoc/actions/workflows/ci.yml)
[![Release](https://github.com/hacybeyker/SnapDoc/actions/workflows/release.yml/badge.svg)](https://github.com/hacybeyker/SnapDoc/actions/workflows/release.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-BOM_2026.06-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-minSdk_26-3DDC84?logo=android&logoColor=white)

> Document scanner that **understands** what it scans, without sending anything to the cloud. Detects edges,
> corrects perspective and extracts text (OCR) with **CameraX + ML Kit**, then classifies/summarizes each
> document with **on-device generative AI (Gemini Nano via ML Kit GenAI)** — zero data leaves the device.

---

## 📁 Structure

```
.
├── app/src/main/java/com/hacybeyker/snapdoc/
│   ├── core/              # Shared code (design tokens in core/ui/theme/)
│   ├── navigation/        # NavKeys + AppNavHost (Navigation 3)
│   └── feature/<name>/    # Vertical Slices: domain / data / ui
├── AGENTS.md              # Source of truth for AI agents
├── .agents/               # AI skills & infrastructure
└── gradle/
    └── libs.versions.toml # Centralized version catalog
```

---

## ⚡ Run

```bash
./gradlew assembleDebug    # or Run from Android Studio
```

---

## 🧪 Tests

```bash
./gradlew test    # Unit tests (JVM — no emulator)
```

---

## 🎨 Code quality

ktlint + detekt + Android Lint are preconfigured. Rules in `.editorconfig`, `config/detekt/detekt.yml` and `lint.xml`.

```bash
# Before every commit: format and verify everything
./gradlew formatAndAnalyze

# Verification only (CI / pre-push)
./gradlew codeQuality
```

The project includes a **pre-commit hook** that runs `formatAndAnalyze` automatically before every commit. Install it once after cloning:

```bash
chmod +x scripts/setup-quality-hook.sh && ./scripts/setup-quality-hook.sh
```

---

## 🤖 AI-assisted development

This project ships with infrastructure for AI agents (Claude Code, Copilot, Cursor, Junie, Antigravity…).

Start by telling your agent:

> *"Read AGENTS.md and help me implement my first feature."*

The agent will find the architecture rules (Vertical Slice + Clean + MVI), the coding standard and the testing guide in `.agents/skills/`.

---

## 📄 Changelog

See [CHANGELOG.md](./CHANGELOG.md) for the change history.
