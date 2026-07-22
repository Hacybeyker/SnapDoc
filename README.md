# 🚀 ScaffoldingAndroidCompose

[![CI](https://github.com/hacybeyker/ScaffoldingAndroidCompose/actions/workflows/ci.yml/badge.svg)](https://github.com/hacybeyker/ScaffoldingAndroidCompose/actions/workflows/ci.yml)

> **Plantilla (scaffolding) para crear proyectos Android nativos con Jetpack Compose e infraestructura de IA lista para usar.**

Clona, ejecuta un script, y en menos de un minuto tienes un proyecto Android con tu nombre, tu package y reglas de arquitectura listas para que cualquier agente de IA (Claude Code, GitHub Copilot, Cursor, Junie, Antigravity…) trabaje con calidad profesional desde el primer prompt.

---

## ✨ ¿Qué incluye?

| Componente | Descripción |
|------------|-------------|
| **Jetpack Compose + Material 3** | UI declarativa con design tokens (Color, Type, Shape, Spacing) |
| **Arquitectura** | Vertical Slice (feature-first) + regla de dependencia Clean + MVI, con una feature de ejemplo completa |
| **Hilt + Navigation 3** | DI por feature (`@Binds`) y navegación type-safe (`NavKey @Serializable`) |
| **`init-project.sh`** | Script que renombra proyecto, package y applicationId en un solo paso |
| **`AGENTS.md`** | Fuente de verdad para agentes de IA (estándar [agents.md](https://agents.md/)) |
| **`.agents/`** | Skills de IA: android-best-practices, feature-implementation, code-reviewer, commits semánticos, changelog, creación de skills |
| **Symlinks multi-IDE** | Las skills se sincronizan automáticamente para Claude Code, Copilot, Cursor, JetBrains, Junie y Antigravity |
| **Catálogo de versiones** | `gradle/libs.versions.toml` centralizado (Kotlin 2.4, AGP 9, Compose BOM 2026.06) |
| **Calidad de código** | **ktlint + detekt + Android Lint** preconfigurados con tareas agregadas (`./gradlew formatAndAnalyze`) — reglas en `.editorconfig`, `config/detekt/detekt.yml` y `lint.xml` |
| **CI/CD (GitHub Actions)** | `ci.yml` (calidad + build/tests en cada push/PR) y `release.yml` (APK/AAB + GitHub Release al pushear un tag `v*`) + Dependabot |
| **Testing** | JUnit + Turbine + Fakes, con `MainDispatcherRule` y tests de ejemplo (UseCase + ViewModel) |

## 📋 Requisitos

- **JDK 21** (Gradle lo descarga automáticamente vía toolchain si no lo tienes)
- **Android Studio** (versión reciente)

## ⚡ Quick Start

```bash
# 1. Clona la plantilla con el nombre de tu nuevo proyecto
git clone https://github.com/hacybeyker/ScaffoldingAndroidCompose.git MiAppGenial
cd MiAppGenial

# 2. Ejecuta el inicializador (modo interactivo)
./init-project.sh
```

El script te preguntará el **nombre del proyecto**, el **package base** y el **nombre visible de la app**, y hará todo el resto: renombrar archivos, mover paquetes, limpiar la feature de ejemplo, configurar la documentación de IA, crear los symlinks, renombrar la carpeta raíz y dejar el historial de git limpio (squash automático si vienes del scaffolding, o commit encima si vienes de GitHub Template).

¿Prefieres no responder preguntas? Modo no interactivo (ideal para agentes de IA):

```bash
./init-project.sh --name MiAppGenial --package com.empresa.miapp --app-name "Mi App Genial" --yes
```

> 📖 **Guía completa paso a paso:** [SETUP.md](./SETUP.md)

## 🤖 Desarrollo con IA

Una vez inicializado, abre tu agente de IA favorito en la raíz del proyecto y dile:

> *"Lee AGENTS.md y ayúdame a implementar mi primera feature."*

El agente encontrará las reglas de arquitectura (Vertical Slice + Clean + MVI), los estándares de código, la estrategia de testing y la guía de seguridad móvil en `.agents/skills/`. La skill `feature-implementation` define el workflow completo para **features, issues, bugs, enhancements, fixes y refactors** (contexto → snapshot → implementación → DoD → reporte HTML).

## 🏗️ Estructura del proyecto

```
.
├── app/src/main/java/<package>/
│   ├── core/              # Compartido SOLO si ≥2 features lo necesitan
│   │   └── ui/theme/      # Design tokens (Color, Type, Shape, Spacing, Theme)
│   ├── navigation/        # NavKeys @Serializable + AppNavHost (Navigation 3)
│   └── feature/home/      # Feature de EJEMPLO (Vertical Slice completo)
│       ├── domain/        #   modelos + usecases + contrato del repo (Kotlin puro)
│       ├── data/          #   repo impl in-memory + módulo Hilt (@Binds)
│       └── ui/            #   Screen/Content + ViewModel MVI + UiState/Intents
├── AGENTS.md              # Fuente de verdad para agentes de IA
├── .agents/               # Skills e infraestructura de IA
│   ├── skills/            # android-best-practices, feature-implementation, code-reviewer, git-commit…
│   └── scripts/           # sync-skills.sh (symlinks multi-IDE)
├── init-project.sh        # ⚡ Inicializador del scaffolding
└── SETUP.md               # Guía detallada de inicialización
```

> La feature `home` es el ejemplo vivo de la arquitectura (con sus tests). `init-project.sh` la reemplaza por una pantalla mínima para que tu proyecto arranque limpio.

## 🔨 Comandos útiles

```bash
# Compilar la app
./gradlew assembleDebug

# Tests unitarios (JVM — sin emulador)
./gradlew test

# Calidad de código (ktlint + detekt + Android Lint)
./gradlew formatAndAnalyze     # formatea y verifica todo
./gradlew codeQuality          # solo verifica (ideal para CI)
```

## 📄 Licencia

Usa esta plantilla libremente para cualquier proyecto, personal o comercial.

---

Hecho con ❤️ para acelerar el desarrollo **Android + IA**. Si te sirve, ¡deja una ⭐ en el repo!
