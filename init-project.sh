#!/bin/bash
# init-project.sh — Inicializador del Scaffolding Android + Jetpack Compose
#
# Convierte este scaffolding en TU proyecto: renombra el proyecto, el package,
# el applicationId y configura la infraestructura de IA.
#
# Uso interactivo:
#   ./init-project.sh
#
# Uso no interactivo (ideal para agentes de IA / CI):
#   ./init-project.sh --name MiApp --package com.empresa.miapp [--app-name "Mi App"] [--yes]
#
# Flags:
#   -n, --name      Nombre TÉCNICO del proyecto (PascalCase, SIN espacios). Ej: MiAppGenial
#                   → se usa como: rootProject.name de Gradle, nombre del theme Compose,
#                     nombre de la carpeta raíz, módulos del IDE.
#   -p, --package   Package base. Ej: com.empresa.miapp
#                   → se usa como: namespace de Kotlin y applicationId de Android.
#   -a, --app-name  Nombre VISIBLE de la app (puede tener espacios y acentos).
#                   Default: igual a --name. Ej: "Mi App Genial"
#                   → se muestra en el launcher (strings.xml/app_name).
#   -y, --yes       Responde "sí" a todas las confirmaciones (limpieza de archivos)
#   -h, --help      Muestra esta ayuda

set -e

# ── Valores actuales del scaffolding (NO modificar) ─────────────────────────
OLD_PROJECT_NAME="ScaffoldingAndroidCompose"
OLD_PACKAGE_NAME="com.hacybeyker.scaffoldingandroidcompose"
MODULE_NAME="app"

SCRIPT_NAME="$(basename "$0")"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# ── Helpers ──────────────────────────────────────────────────────────────────
sedi() {
    # sed -i portable (macOS requiere sufijo vacío)
    if [[ "$OSTYPE" == darwin* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

usage() {
    sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'
    exit 0
}

die() {
    echo "❌ Error: $1" >&2
    exit 1
}

# ── 1. Recopilar información ─────────────────────────────────────────────────
PROJECT_NAME=""
PACKAGE_NAME=""
APP_NAME=""
ASSUME_YES=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -n|--name)     PROJECT_NAME="$2"; shift 2 ;;
        -p|--package)  PACKAGE_NAME="$2"; shift 2 ;;
        -a|--app-name) APP_NAME="$2"; shift 2 ;;
        -y|--yes)      ASSUME_YES=true; shift ;;
        -h|--help)     usage ;;
        *)             die "Flag desconocida: $1 (usa --help)" ;;
    esac
done

echo "🚀 Scaffolding Android + Compose — Inicializador de Proyecto"
echo "------------------------------------------------------------"

if [[ -z "$PROJECT_NAME" ]]; then
    echo "📌 Nombre TÉCNICO (PascalCase, sin espacios)."
    echo "   Se usa en Gradle, el theme Compose, módulos del IDE y la carpeta raíz."
    read -r -p "   Ej. MiAppGenial: " PROJECT_NAME
fi
if [[ -z "$PACKAGE_NAME" ]]; then
    read -r -p "📦 Package base (ej. com.empresa.miapp): " PACKAGE_NAME
fi
if [[ -z "$APP_NAME" && "$ASSUME_YES" == false ]]; then
    echo "🏷️  Nombre VISIBLE en el launcher (puede tener espacios y acentos)."
    read -r -p "   [$PROJECT_NAME]: " APP_NAME
fi
APP_NAME=${APP_NAME:-$PROJECT_NAME}

# ── 2. Validaciones ──────────────────────────────────────────────────────────
[[ -n "$PROJECT_NAME" && -n "$PACKAGE_NAME" ]] || die "El nombre del proyecto y el package son obligatorios."

[[ "$PROJECT_NAME" =~ ^[A-Za-z][A-Za-z0-9_]*$ ]] \
    || die "Nombre de proyecto inválido: '$PROJECT_NAME'. Usa solo letras/números, sin espacios (ej. MiAppGenial)."

[[ "$PACKAGE_NAME" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] \
    || die "Package inválido: '$PACKAGE_NAME'. Usa minúsculas separadas por puntos (ej. com.empresa.miapp)."

[[ "$PACKAGE_NAME" != "$OLD_PACKAGE_NAME" ]] \
    || die "El package no puede ser el mismo del scaffolding ($OLD_PACKAGE_NAME)."

grep -rq "$OLD_PACKAGE_NAME" settings.gradle.kts app 2>/dev/null \
    || die "Este scaffolding ya fue inicializado (no quedan referencias a $OLD_PACKAGE_NAME)."

OLD_PACKAGE_PATH=$(echo "$OLD_PACKAGE_NAME" | tr '.' '/')
NEW_PACKAGE_PATH=$(echo "$PACKAGE_NAME" | tr '.' '/')
OLD_PACKAGE_RE=${OLD_PACKAGE_NAME//./\\.}

echo ""
echo "⚙️  Se configurará el proyecto con:"
echo "   - Proyecto (técnico, sin espacios): $PROJECT_NAME"
echo "   - App visible (launcher):           $APP_NAME"
echo "   - Package:                          $PACKAGE_NAME"
echo "   - Root:                             $PROJECT_ROOT"
echo ""

if [[ "$ASSUME_YES" == false ]]; then
    read -r -p "¿Continuar? [Y/n]: " CONFIRM
    [[ -z "$CONFIRM" || "$CONFIRM" =~ ^[Yy] ]] || { echo "Cancelado."; exit 0; }
fi

# ── 3. Reemplazo en archivos de texto ────────────────────────────────────────
# Excluye binarios (-I), directorios generados y archivos que se sobrescriben
# íntegramente al final del script:
#   README.md    → regenerado en el paso 7  (queda en el proyecto nuevo)
#   CHANGELOG.md → regenerado en el paso 7b (queda en el proyecto nuevo)
#   SETUP.md     → eliminado en el paso 8   (era solo para el scaffolding)
#   $SCRIPT_NAME → eliminado en el paso 8   (era solo para el scaffolding)
replace_in_repo() {
    local pattern=$1
    local replacement=$2
    grep -rIl \
        --exclude-dir=.git --exclude-dir=.gradle --exclude-dir=.kotlin \
        --exclude-dir=.idea --exclude-dir=build \
        --exclude="$SCRIPT_NAME" --exclude="README.md" --exclude="SETUP.md" --exclude="CHANGELOG.md" \
        "$pattern" . 2>/dev/null | while read -r file; do
        sedi "s|$pattern|$replacement|g" "$file"
        echo "   ✏️  $file"
    done
}

echo "🏷️  Configurando nombre visible de la app (strings.xml)..."
sedi "s|<string name=\"app_name\">.*</string>|<string name=\"app_name\">$APP_NAME</string>|" \
    "app/src/main/res/values/strings.xml"

echo "📦 Renombrando package: $OLD_PACKAGE_NAME → $PACKAGE_NAME"
replace_in_repo "$OLD_PACKAGE_RE" "$PACKAGE_NAME"

echo "📛 Renombrando proyecto: $OLD_PROJECT_NAME → $PROJECT_NAME"
replace_in_repo "$OLD_PROJECT_NAME" "$PROJECT_NAME"

# ── 4. Mover directorios de código fuente al nuevo package ──────────────────
echo "📁 Moviendo directorios de código al nuevo package..."
find . -type d -path "*/java/$OLD_PACKAGE_PATH" \
    -not -path "./.git/*" -not -path "*/build/*" | while read -r old_dir; do
    base="${old_dir%/"$OLD_PACKAGE_PATH"}"
    new_dir="$base/$NEW_PACKAGE_PATH"
    [[ "$old_dir" == "$new_dir" ]] && continue
    mkdir -p "$new_dir"
    # Mueve todo el contenido (archivos y subcarpetas de features)
    find "$old_dir" -mindepth 1 -maxdepth 1 -exec mv {} "$new_dir"/ \;
    echo "   ✅ $new_dir"
done

# Elimina los directorios vacíos que dejó el package anterior
OLD_TOP_SEGMENT=$(echo "$OLD_PACKAGE_NAME" | cut -d. -f1)
find . -type d -path "*/java/$OLD_TOP_SEGMENT" -not -path "./.git/*" -not -path "*/build/*" \
    -exec find {} -depth -type d -empty -delete \; 2>/dev/null || true

# ── 4.5. Limpiar la feature de ejemplo (home/greeting) ───────────────────────
# La feature "home" del scaffolding demuestra la arquitectura (domain/data/ui).
# Se reemplaza por una pantalla mínima para que el proyecto arranque limpio.
echo "🗑️  Eliminando código de ejemplo del scaffolding..."
MAIN_SRC="app/src/main/java/$NEW_PACKAGE_PATH"
TEST_SRC="app/src/test/java/$NEW_PACKAGE_PATH"

rm -rf "$MAIN_SRC/feature/home/domain" "$MAIN_SRC/feature/home/data"
rm -f "$MAIN_SRC/feature/home/ui/HomeUiState.kt" \
      "$MAIN_SRC/feature/home/ui/HomeViewModel.kt"
rm -rf "$TEST_SRC/feature"

cat > "$MAIN_SRC/feature/home/ui/HomeScreen.kt" <<HOMEEOF
package $PACKAGE_NAME.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import $PACKAGE_NAME.R
import $PACKAGE_NAME.core.ui.theme.${PROJECT_NAME}Theme

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ${PROJECT_NAME}Theme {
        HomeScreen()
    }
}
HOMEEOF

# Deja solo app_name en strings.xml (los strings del ejemplo ya no existen)
cat > "app/src/main/res/values/strings.xml" <<STREOF
<resources>
    <string name="app_name">$APP_NAME</string>
</resources>
STREOF
echo "   ✅ Feature de ejemplo eliminada; HomeScreen mínimo restaurado."

# ── 4.6. Renombrar archivos .iml e índices de IntelliJ/Android Studio ───────
# Android Studio crea archivos .iml usando el rootProject.name. Si el usuario
# abrió el proyecto en el IDE antes de inicializarlo, esos archivos quedan con
# el nombre viejo. Los renombramos y actualizamos las referencias.
if [[ -d ".idea" ]]; then
    echo "🧠 Limpiando metadatos de IntelliJ/Android Studio (.idea/)..."
    find .idea -maxdepth 2 -name "${OLD_PROJECT_NAME}*.iml" 2>/dev/null | while read -r old_iml; do
        new_iml="${old_iml//$OLD_PROJECT_NAME/$PROJECT_NAME}"
        mv "$old_iml" "$new_iml"
        echo "   ✏️  $old_iml → $new_iml"
    done
    find . -maxdepth 1 -name "${OLD_PROJECT_NAME}*.iml" 2>/dev/null | while read -r old_iml; do
        new_iml="${old_iml//$OLD_PROJECT_NAME/$PROJECT_NAME}"
        mv "$old_iml" "$new_iml"
        echo "   ✏️  $old_iml → $new_iml"
    done
    grep -rIl "$OLD_PROJECT_NAME" .idea 2>/dev/null | while read -r idea_file; do
        sedi "s|$OLD_PROJECT_NAME|$PROJECT_NAME|g" "$idea_file"
    done
    if [[ -f ".idea/.name" ]]; then
        echo "$PROJECT_NAME" > ".idea/.name"
    fi
    echo "   ✅ Metadatos del IDE actualizados."
fi

# ── 5. Placeholders en la documentación de IA ────────────────────────────────
echo "📝 Configurando documentación de IA (AGENTS.md y .agents/)..."
replace_placeholders() {
    local file=$1
    sedi "s|{{PROJECT_NAME}}|$PROJECT_NAME|g" "$file"
    sedi "s|{{PACKAGE_NAME}}|$PACKAGE_NAME|g" "$file"
    sedi "s|{{MODULE_NAME}}|$MODULE_NAME|g" "$file"
    sedi "s|{{PACKAGE_PATH}}|$NEW_PACKAGE_PATH|g" "$file"
    sedi "s|{{PROJECT_ROOT}}|$PROJECT_NAME|g" "$file"
}

[[ -f "AGENTS.md" ]] && replace_placeholders "AGENTS.md"
find .agents -type f \( -name "*.md" -o -name "*.sh" \) | while read -r file; do
    replace_placeholders "$file"
done
echo "   ✅ Documentación actualizada."

# ── 6. Symlinks de skills para los IDEs ──────────────────────────────────────
echo "🔗 Sincronizando skills con los editores..."
if [[ -f ".agents/scripts/sync-skills.sh" ]]; then
    chmod +x .agents/scripts/sync-skills.sh
    ./.agents/scripts/sync-skills.sh
else
    echo "⚠️  No se encontró .agents/scripts/sync-skills.sh — omitido."
fi

# ── 7. README del nuevo proyecto ─────────────────────────────────────────────
echo "📄 Generando README.md del nuevo proyecto..."
# GITHUB_USER se puede pasar como variable de entorno; si no existe usa el placeholder.
GH_USER="${GITHUB_USER:-TU_USUARIO}"
cat > README.md <<EOF
# $APP_NAME

<!-- TODO: reemplaza TU_USUARIO con tu usuario u organización de GitHub -->
[![CI](https://github.com/$GH_USER/$PROJECT_NAME/actions/workflows/ci.yml/badge.svg)](https://github.com/$GH_USER/$PROJECT_NAME/actions/workflows/ci.yml)
[![Release](https://github.com/$GH_USER/$PROJECT_NAME/actions/workflows/release.yml/badge.svg)](https://github.com/$GH_USER/$PROJECT_NAME/actions/workflows/release.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-BOM_2026.06-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-minSdk_26-3DDC84?logo=android&logoColor=white)

> <!-- TODO: escribe aquí una descripción breve de tu proyecto -->
> *Descripción del proyecto*

---

## 📁 Estructura

\`\`\`
.
├── app/src/main/java/$NEW_PACKAGE_PATH/
│   ├── core/              # Compartido (design tokens en core/ui/theme/)
│   ├── navigation/        # NavKeys + AppNavHost (Navigation 3)
│   └── feature/<name>/    # Vertical Slices: domain / data / ui
├── AGENTS.md              # Fuente de verdad para agentes de IA
├── .agents/               # Skills e infraestructura de IA
└── gradle/
    └── libs.versions.toml # Catálogo centralizado de versiones
\`\`\`

---

## ⚡ Ejecutar

\`\`\`bash
./gradlew assembleDebug    # o Run desde Android Studio
\`\`\`

---

## 🧪 Tests

\`\`\`bash
./gradlew test    # Tests unitarios (JVM — sin emulador)
\`\`\`

---

## 🎨 Calidad de código

ktlint + detekt + Android Lint están preconfigurados. Reglas en \`.editorconfig\`, \`config/detekt/detekt.yml\` y \`lint.xml\`.

\`\`\`bash
# Antes de cada commit: formatea y verifica todo
./gradlew formatAndAnalyze

# Solo verificación (CI / pre-push)
./gradlew codeQuality
\`\`\`

El proyecto incluye un **pre-commit hook** que ejecuta \`formatAndAnalyze\` automáticamente antes de cada commit. Instálalo una sola vez tras clonar:

\`\`\`bash
chmod +x scripts/setup-quality-hook.sh && ./scripts/setup-quality-hook.sh
\`\`\`

---

## 🤖 Desarrollo con IA

Este proyecto incluye infraestructura para agentes de IA (Claude Code, Copilot, Cursor, Junie, Antigravity…).

Comienza diciéndole a tu agente:

> *"Lee AGENTS.md y ayúdame a implementar mi primera feature."*

El agente encontrará las reglas de arquitectura (Vertical Slice + Clean + MVI), el estándar de código y la guía de testing en \`.agents/skills/\`.

---

## 📄 Changelog

Ver [CHANGELOG.md](./CHANGELOG.md) para el historial de cambios.
EOF
echo "   ✅ README.md generado."

# ── 7b. CHANGELOG del nuevo proyecto ─────────────────────────────────────────
echo "📝 Generando CHANGELOG.md del nuevo proyecto..."
INIT_DATE=$(date +%Y-%m-%d)
cat > CHANGELOG.md <<EOF
# Changelog — $APP_NAME

> Todos los cambios notables de este proyecto están documentados aquí.
> Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).

---

## [Unreleased]

### ✨ Added
- Proyecto inicializado a partir de [ScaffoldingAndroidCompose](https://github.com/hacybeyker/ScaffoldingAndroidCompose)

---

<!-- Ejemplo de entrada:

## [1.0.0] — $INIT_DATE

### ✨ Added
- Feature X implementada como Vertical Slice (domain → data → ui)

### 🔧 Fixed
- Bug Y corregido en la feature Z

### ♻️ Changed
- Refactor de Z para mejorar legibilidad

### 🗑️ Removed
- Eliminado código obsoleto de A

-->

[Unreleased]: https://github.com/TU_ORG/$PROJECT_NAME/compare/v1.0.0...HEAD
EOF
echo "   ✅ CHANGELOG.md generado."

# ── 8. Limpieza de archivos del scaffolding ──────────────────────────────────
CLEANUP="n"
if [[ "$ASSUME_YES" == true ]]; then
    CLEANUP="y"
else
    read -r -p "🧹 ¿Eliminar archivos del scaffolding (SETUP.md y $SCRIPT_NAME)? [Y/n]: " CLEANUP
    CLEANUP=${CLEANUP:-y}
fi
if [[ "$CLEANUP" =~ ^[Yy] ]]; then
    rm -f SETUP.md
    rm -f -- "$SCRIPT_NAME"
    echo "   ✅ Archivos del scaffolding eliminados."
fi

# ── 9. Historial de git — automático según el origen ─────────────────────────
# Estrategia:
#   • Opción A (clone del scaffolding): squash a un único commit inicial y
#     elimina el remoto del scaffolding (el usuario añadirá el suyo después).
#   • Opción B (GitHub Template) o repo personalizado: añade un commit encima
#     del initial commit que GitHub ya creó. Funciona con `git push` normal.
if [[ -d ".git" ]]; then
    REMOTE_URL=$(git remote get-url origin 2>/dev/null || echo "")
    if [[ "$REMOTE_URL" == *"ScaffoldingAndroidCompose"* || "$REMOTE_URL" == *"scaffoldingandroidcompose"* ]]; then
        echo "🌱 Squash del historial del scaffolding (Opción A detectada)..."
        CURRENT_BRANCH=$(git symbolic-ref --short HEAD 2>/dev/null || echo "main")
        git remote remove origin 2>/dev/null || true
        git checkout --orphan _init_clean -q
        git add -A
        git commit -q -m "feat: initial commit from ScaffoldingAndroidCompose template ($PROJECT_NAME)"
        git branch -D "$CURRENT_BRANCH" 2>/dev/null || true
        git branch -m "$CURRENT_BRANCH"
        echo "   ✅ Historial squasheado en un commit inicial. Remoto del scaffolding eliminado."
    else
        echo "🌱 Añadiendo commit de personalización sobre el historial existente..."
        git add -A
        if git commit -q -m "feat: customize scaffolding for $PROJECT_NAME" 2>/dev/null; then
            echo "   ✅ Commit añadido. Puedes hacer 'git push' sin --force."
        else
            echo "   ℹ️  No hubo cambios para commitear."
        fi
    fi
fi

# ── 10. Instalar pre-commit hook ─────────────────────────────────────────────
# Se ejecuta después del posible git reset para que el hook quede en el .git
# recién creado. El script se mantiene en el repo para que futuros colaboradores
# que clonen el proyecto puedan instalarlo con: ./scripts/setup-quality-hook.sh
if [[ -d ".git" && -f "scripts/setup-quality-hook.sh" ]]; then
    chmod +x scripts/setup-quality-hook.sh
    ./scripts/setup-quality-hook.sh
fi

# ── 10b. Renombrar la carpeta raíz si no coincide con el nombre del proyecto ──
# Se hace AL FINAL para no romper rutas relativas durante el resto del script.
RENAMED_ROOT=""
CURRENT_FOLDER_NAME="$(basename "$PROJECT_ROOT")"
if [[ "$CURRENT_FOLDER_NAME" != "$PROJECT_NAME" ]]; then
    PARENT_DIR="$(dirname "$PROJECT_ROOT")"
    NEW_ROOT="$PARENT_DIR/$PROJECT_NAME"
    if [[ -e "$NEW_ROOT" ]]; then
        echo "⚠️  No se renombró la carpeta raíz: ya existe '$NEW_ROOT'."
        echo "    Hazlo manualmente cuando sea seguro."
    else
        # mv funciona aunque estemos dentro de la carpeta (el inode no cambia).
        mv "$PROJECT_ROOT" "$NEW_ROOT"
        RENAMED_ROOT="$NEW_ROOT"
        echo "📂 Carpeta raíz renombrada: $CURRENT_FOLDER_NAME → $PROJECT_NAME"
    fi
fi

# ── 11. Resumen final ────────────────────────────────────────────────────────
echo ""
echo "------------------------------------------------------------"
echo "🎉 ¡$PROJECT_NAME está listo!"
echo "------------------------------------------------------------"
if [[ -n "$RENAMED_ROOT" ]]; then
    echo "⚠️  Tu terminal sigue apuntando a la ruta vieja. Cambia con:"
    echo "      cd $RENAMED_ROOT"
    echo ""
fi
echo "🚀 Siguientes pasos:"
echo "   1. Abre el proyecto en Android Studio y sincroniza Gradle."
echo "   2. Verifica el build: ./gradlew assembleDebug"
echo "   3. Conecta tu repo en GitHub (si vienes de un clone del scaffolding):"
echo "      git remote add origin <tu-url> && git push -u origin main"
echo "   4. Nuevos colaboradores: ejecuta ./scripts/setup-quality-hook.sh una vez tras clonar."
echo "   5. Dile a tu IA: 'Lee AGENTS.md y ayúdame a implementar mi primera feature'."
echo "------------------------------------------------------------"
