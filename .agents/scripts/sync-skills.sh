#!/bin/bash
# sync-skills.sh — Motor de Sincronización de Agent Skills
# Garantiza que todos los editores tengan symlinks correctos hacia .agents/skills/

set -e

# Detectar la raíz del proyecto
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "🔄 Sincronizando Agent Skills..."
echo "------------------------------------------------------------"

# 1. Verificar fuente de verdad
if [ ! -d ".agents/skills" ]; then
    echo "❌ Error: La carpeta .agents/skills/ no existe."
    exit 1
fi

# 2. Configuración de Editores y Profundidades
# "ruta:profundidad"
EDITORS=(
    ".github/copilot/skills:3"
    ".jetbrains/agent/skills:3"
    ".claude/skills:2"
    ".cursor/skills:2"
    ".junie/skills:2"
    ".antigravity/skills:2"
    ".agent/skills:2"
)

# 3. Función de vinculación
sync_skill() {
    local skill_name=$1

    for entry in "${EDITORS[@]}"; do
        local dir="${entry%%:*}"
        local depth="${entry#*:}"

        mkdir -p "$dir"

        # Construir prefijo relativo
        local prefix=""
        for ((i=0; i<depth; i++)); do prefix="../$prefix"; done

        # Crear symlink (f = forzar, s = simbólico)
        (cd "$dir" && ln -sf "${prefix}.agents/skills/$skill_name" "$skill_name")
        echo "   ✅ $dir/$skill_name"
    done
}

# 4. Procesar todas las skills
for skill_path in .agents/skills/*; do
    if [ -d "$skill_path" ]; then
        SKILL_NAME=$(basename "$skill_path")
        echo "📦 Skill: $SKILL_NAME"
        sync_skill "$SKILL_NAME"
        echo ""
    fi
done

echo "🎉 Sincronización finalizada con éxito."
echo "------------------------------------------------------------"
