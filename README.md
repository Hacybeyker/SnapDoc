# SnapDoc

<!-- TODO: reemplaza TU_USUARIO con tu usuario u organización de GitHub -->
[![CI](https://github.com/TU_USUARIO/SnapDoc/actions/workflows/ci.yml/badge.svg)](https://github.com/TU_USUARIO/SnapDoc/actions/workflows/ci.yml)
[![Release](https://github.com/TU_USUARIO/SnapDoc/actions/workflows/release.yml/badge.svg)](https://github.com/TU_USUARIO/SnapDoc/actions/workflows/release.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-BOM_2026.06-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-minSdk_26-3DDC84?logo=android&logoColor=white)

> <!-- TODO: escribe aquí una descripción breve de tu proyecto -->
> *Descripción del proyecto*

---

## 📁 Estructura

```
.
├── app/src/main/java/com/hacybeyker/snapdoc/
│   ├── core/              # Compartido (design tokens en core/ui/theme/)
│   ├── navigation/        # NavKeys + AppNavHost (Navigation 3)
│   └── feature/<name>/    # Vertical Slices: domain / data / ui
├── AGENTS.md              # Fuente de verdad para agentes de IA
├── .agents/               # Skills e infraestructura de IA
└── gradle/
    └── libs.versions.toml # Catálogo centralizado de versiones
```

---

## ⚡ Ejecutar

```bash
./gradlew assembleDebug    # o Run desde Android Studio
```

---

## 🧪 Tests

```bash
./gradlew test    # Tests unitarios (JVM — sin emulador)
```

---

## 🎨 Calidad de código

ktlint + detekt + Android Lint están preconfigurados. Reglas en `.editorconfig`, `config/detekt/detekt.yml` y `lint.xml`.

```bash
# Antes de cada commit: formatea y verifica todo
./gradlew formatAndAnalyze

# Solo verificación (CI / pre-push)
./gradlew codeQuality
```

El proyecto incluye un **pre-commit hook** que ejecuta `formatAndAnalyze` automáticamente antes de cada commit. Instálalo una sola vez tras clonar:

```bash
chmod +x scripts/setup-quality-hook.sh && ./scripts/setup-quality-hook.sh
```

---

## 🤖 Desarrollo con IA

Este proyecto incluye infraestructura para agentes de IA (Claude Code, Copilot, Cursor, Junie, Antigravity…).

Comienza diciéndole a tu agente:

> *"Lee AGENTS.md y ayúdame a implementar mi primera feature."*

El agente encontrará las reglas de arquitectura (Vertical Slice + Clean + MVI), el estándar de código y la guía de testing en `.agents/skills/`.

---

## 📄 Changelog

Ver [CHANGELOG.md](./CHANGELOG.md) para el historial de cambios.
