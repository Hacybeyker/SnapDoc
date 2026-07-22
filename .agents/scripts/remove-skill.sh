#!/bin/bash
# remove-skill.sh — Eliminador seguro de Skill
# Elimina una skill de la fuente de verdad y limpia todos sus symlinks

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

if [ -z "$1" ]; then
    echo "❌ Error: Debes especificar el nombre de la skill."
    echo "Uso: ./remove-skill.sh <skill-name>"
    exit 1
fi

SKILL_NAME="$1"

# 1. Verificar existencia
if [ ! -d ".agents/skills/$SKILL_NAME" ]; then
    echo "❌ Error: La skill '$SKILL_NAME' no existe."
    exit 1
fi

echo "⚠️  Vas a eliminar la skill '$SKILL_NAME' permanentemente."
read -p "¿Estás seguro? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Operación cancelada."
    exit 1
fi

# 2. Eliminar de la fuente de verdad
rm -rf ".agents/skills/$SKILL_NAME"
echo "   ✅ Eliminado de .agents/skills/"

# 3. Limpiar symlinks en editores
EDITORS=(
    ".github/copilot/skills"
    ".jetbrains/agent/skills"
    ".claude/skills"
    ".cursor/skills"
    ".junie/skills"
    ".antigravity/skills"
    ".agent/skills"
)

for dir in "${EDITORS[@]}"; do
    if [ -L "$dir/$SKILL_NAME" ]; then
        rm "$dir/$SKILL_NAME"
        echo "   ✅ Symlink eliminado en $dir"
    fi
done

echo ""
echo "🎉 Skill '$SKILL_NAME' eliminada por completo."
echo "📝 No olvides removerla manualmente de .agents/README.md"
echo ""
