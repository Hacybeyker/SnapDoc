---
name: skill-linker
description: 'Automatically synchronizes AI Agent Skills across all supported editors (Android Studio, VS Code, Claude Code, Cursor, etc.). Use when a new skill is added, renamed, or when symlinks are broken. Ensures .agents/skills/ remains the single source of truth.'
license: MIT
---

# Skill Linker — AI Infrastructure Sync

Este skill automatiza la vinculación de reglas de IA entre diferentes editores de código para garantizar un comportamiento consistente.

## Workflow de Sincronización

Cuando se detecta un cambio en `.agents/skills/` (nueva skill, cambio de nombre o eliminación), el agente debe ejecutar el motor de sincronización.

### 1. Ejecución del Motor
Para sincronizar todos los editores, ejecuta el script maestro desde la raíz:

```bash
./.agents/scripts/sync-skills.sh
```

### 2. Editores Soportados
El motor creará automáticamente symlinks relativos en:
- `.jetbrains/agent/skills/` (Android Studio / IntelliJ)
- `.github/copilot/skills/` (VS Code)
- `.claude/skills/` (Claude CLI)
- `.cursor/skills/` (Cursor)
- `.junie/skills/` (Junie)
- `.antigravity/skills/` (Antigravity)

### 3. Verificación
El agente debe verificar que los accesos directos apunten correctamente a la "Fuente de Verdad":

```bash
# Ejemplo para VS Code
readlink .github/copilot/skills/[skill-name]
# Debería mostrar: ../../../.agents/skills/[skill-name]
```

## Protocolo de Creación de Nueva Skill

1. Crea la carpeta en `.agents/skills/[name]`.
2. Crea el archivo `SKILL.md` con su metadata.
3. Ejecuta `./.agents/scripts/sync-skills.sh`.
4. Verifica que la skill aparezca en la lista de `README.md`.

---
**Nota**: Mantener los symlinks sincronizados es vital para que la IA herede las reglas de arquitectura del proyecto sin importar qué editor use el desarrollador.
