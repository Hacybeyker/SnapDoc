#!/bin/bash
# Installs the code-quality pre-commit hook.
# Run once after cloning the repository:
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
    echo "ℹ️  formatAndAnalyze fixed the formatting — the commit includes the corrected files."
fi

if [ $FORMAT_EXIT -ne 0 ]; then
    echo "❌ Commit blocked — there are errors that can't be auto-fixed. Check the output above."
    exit 1
fi

exit 0
EOF

chmod +x "$HOOK_FILE"
echo "✅ pre-commit hook installed at $HOOK_FILE"
