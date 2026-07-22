# 📖 SETUP — De Scaffolding a tu nuevo proyecto Android

Esta guía explica, paso a paso, cómo convertir **ScaffoldingAndroidCompose** en tu propio proyecto Android nativo con Jetpack Compose.

---

## 1. Requisitos previos

| Herramienta | Versión | Notas |
|-------------|---------|-------|
| JDK | 21 | Gradle lo descarga solo vía toolchain (`gradle/gradle-daemon-jvm.properties`) |
| Android Studio | Reciente | Con soporte para AGP 9 |
| Git | Cualquiera | Para clonar y versionar |

## 2. Obtener la plantilla

**Opción A — Clonar con el nombre final del proyecto (recomendado):**

```bash
git clone https://github.com/hacybeyker/ScaffoldingAndroidCompose.git MiAppGenial
cd MiAppGenial
```

**Opción B — Usar como GitHub Template:** pulsa **"Use this template"** en GitHub, crea tu repo y clónalo.

## 3. Ejecutar el inicializador

### Modo interactivo

```bash
./init-project.sh
```

El script pregunta:

| Pregunta | Ejemplo | Regla |
|----------|---------|-------|
| 📌 Nombre del Proyecto | `MiAppGenial` | PascalCase, sin espacios ni guiones |
| 📦 Package base | `com.empresa.miapp` | Minúsculas separadas por puntos |
| 🏷️ Nombre visible de la app | `Mi App Genial` | Libre (puede tener espacios). Default: el nombre del proyecto |

Al final pregunta por **una limpieza opcional** (recomendada):

- 🧹 **Eliminar archivos del scaffolding** (`SETUP.md` y el propio `init-project.sh`).

Y aplica **automáticamente** estas tareas (no necesitas hacer nada):

- 🗑️ **Limpia la feature de ejemplo** (`feature/home` con su domain/data/tests) y la reemplaza por una pantalla mínima.
- 📂 **Renombra la carpeta raíz** si no coincide con el nombre del proyecto.
- 🧠 **Limpia metadatos del IDE** (`.idea/*.iml`, `.idea/modules.xml`, `.idea/.name`).
- 🌱 **Historial de git inteligente**:
  - Si clonaste el scaffolding (Opción A): **squashea** el historial en un commit inicial y elimina el remoto del scaffolding.
  - Si vienes de GitHub Template (Opción B): **añade un commit** de personalización sobre el initial commit (push sin `--force`).
- 🪝 **Instala el pre-commit hook** de calidad (`formatAndAnalyze` antes de cada commit).

### Modo no interactivo (CI / agentes de IA)

```bash
./init-project.sh \
  --name MiAppGenial \
  --package com.empresa.miapp \
  --app-name "Mi App Genial" \
  --yes
```

Con `--yes` no se hacen preguntas: se confirma todo y se eliminan los archivos del scaffolding. El manejo de git sigue siendo automático.

## 4. ¿Qué modifica exactamente el script?

| Área | Archivos | Cambio |
|------|----------|--------|
| **Gradle** | `settings.gradle.kts` | `rootProject.name` |
| **Android** | `app/build.gradle.kts` | `namespace` y `applicationId` |
| **Android** | `app/src/main/res/values/strings.xml` | `app_name` (nombre visible) |
| **Android** | `app/src/main/res/values/themes.xml` | Nombre del theme XML |
| **Kotlin** | `app/src/**` | Declaraciones `package`/`import`, nombre del theme Compose y **carpetas movidas** al nuevo package |
| **Ejemplo** | `feature/home/**` (main + test) | Slice de ejemplo eliminado; `HomeScreen` mínimo restaurado |
| **Docs IA** | `AGENTS.md`, `.agents/**` | Placeholders `{{PROJECT_NAME}}`, `{{PACKAGE_NAME}}`, `{{MODULE_NAME}}`, `{{PACKAGE_PATH}}`, `{{PROJECT_ROOT}}` |
| **IDE/IA** | `.claude/`, `.cursor/`, `.github/copilot/`, `.jetbrains/`, `.junie/`, `.antigravity/`, `.agent/` | Symlinks hacia `.agents/skills/` (vía `sync-skills.sh`) |
| **IntelliJ/Android Studio** | `.idea/*.iml`, `.idea/modules.xml`, `.idea/.name` | Renombrados con el nuevo nombre del proyecto |
| **Carpeta raíz** | El directorio del proyecto | Renombrado automáticamente si no coincide con `--name` |
| **Git** | Historial local + remoto | Squash (Opción A) o commit-on-top (Opción B) — sin `--force` |
| **README / CHANGELOG** | `README.md`, `CHANGELOG.md` | Se regeneran con la información de tu proyecto |

## 5. Verificación post-setup

```bash
# 1. No deben quedar referencias al scaffolding en el código
#    (README.md y CHANGELOG.md conservan el link de atribución a la plantilla; es normal)
grep -ri "scaffoldingandroidcompose\|com.hacybeyker" --exclude-dir=.git --exclude-dir=build \
  --exclude=README.md --exclude=CHANGELOG.md . || echo "✅ Limpio"

# 2. El proyecto compila
./gradlew assembleDebug

# 3. Los tests pasan
./gradlew test

# 4. La calidad de código está en verde (ktlint + detekt + Android Lint)
./gradlew codeQuality
```

## 6. Empezar a desarrollar con IA

Abre tu agente de IA (Claude Code, Copilot, Cursor, Junie…) en la raíz del proyecto y dile:

> *"Lee AGENTS.md y ayúdame a implementar mi primera feature."*

El agente seguirá automáticamente:

- **Arquitectura**: Vertical Slice + Clean + MVI (`.agents/skills/android-best-practices/`).
- **Workflow**: features/bugs/fixes de punta a punta con DoD y reporte (`.agents/skills/feature-implementation/`).
- **Commits**: mensajes semánticos con `/commit` (`.agents/skills/git-commit/`).
- **Changelog**: notas de versión con `/changelog`.
- **Reglas duras**: sin lógica en Composables, sin Entities/DTOs fuera de data, sin secretos hardcodeados.

## 7. Pasos manuales opcionales

- **Icono de la app**: reemplaza los `mipmap` en `app/src/main/res/`.
- **Dependencias**: agrega Room, DataStore, Retrofit, etc. en `gradle/libs.versions.toml` (la IA sabe usarlo — pídeselo).
- **Repo nuevo en GitHub** (solo si vienes de un clone del scaffolding): `git remote add origin <url> && git push -u origin main`.
- **Firma de release**: configura `signingConfigs` y los secrets del workflow `release.yml` (ver comentarios en ese archivo).

## 8. Solución de problemas

| Problema | Solución |
|----------|----------|
| `Permission denied` al ejecutar el script | `chmod +x init-project.sh` |
| "Este scaffolding ya fue inicializado" | El script solo puede ejecutarse una vez; clona la plantilla de nuevo |
| Gradle no sincroniza tras renombrar | En Android Studio: **File → Invalidate Caches / Restart** |
| Symlinks rotos en Windows | Ejecuta el script desde **Git Bash** con permisos de symlink habilitados (Developer Mode) |
