#!/bin/bash
# Instala el pre-commit hook de calidad de código.
# Ejecutar una sola vez tras clonar el repositorio:
#   chmod +x scripts/setup-quality-hook.sh && ./scripts/setup-quality-hook.sh

set -e

HOOK_DIR="$(git rev-parse --git-dir)/hooks"
HOOK_FILE="$HOOK_DIR/pre-commit"

cat > "$HOOK_FILE" <<'EOF'
#!/bin/bash

./gradlew formatAndAnalyze --quiet
FORMAT_EXIT=$?

FORMATTED=$(git diff --name-only)
if [ -n "$FORMATTED" ]; then
    echo "$FORMATTED" | xargs git add
    echo "ℹ️  formatAndAnalyze corrigió el formato — el commit incluye los archivos corregidos."
fi

if [ $FORMAT_EXIT -ne 0 ]; then
    echo "❌ Commit bloqueado — hay errores que no se pueden auto-corregir. Revisa la salida anterior."
    exit 1
fi

exit 0
EOF

chmod +x "$HOOK_FILE"
echo "✅ pre-commit hook instalado en $HOOK_FILE"
