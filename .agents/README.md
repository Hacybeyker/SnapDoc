# 🤖 AI Agent Infrastructure — {{PROJECT_NAME}}

> **Configuración universal siguiendo el estándar de [Agent Skills](https://agentskills.io/home)**

## 📋 Resumen
Este directorio es el sistema operativo de cualquier IA que trabaje en este proyecto. Proporciona las reglas, el estilo de código y los flujos de trabajo necesarios para garantizar que el desarrollo en **Android nativo con Jetpack Compose** sea consistente y de alta calidad.

---

## 📦 Skills de IA Disponibles

| Skill | Activación | Función |
|-------|------------|---------|
| `android-best-practices` | *Automática* | **Guía Maestra.** Define arquitectura (VSA + Clean + MVI), capas y estándares técnicos. |
| `feature-implementation` | *Automática* | Workflow de punta a punta para features/bugs/fixes/enhancements con DoD y reporte HTML. |
| `code-reviewer` | *Automática* | Revisa el código implementado contra los estándares de `AGENTS.md` (VSA + Clean + MVI) y reporta hallazgos por severidad. |
| `git-commit` | `/commit` | Genera mensajes de commit semánticos y profesionales. |
| `skill-creator` | `/new-skill` | Permite a la IA crear nuevas reglas específicas para este proyecto. |
| `changelog-generator` | `/changelog` | Genera notas de lanzamiento automáticas. |
| `skill-linker` | *Automática* | Sincroniza las skills con todos los editores vía symlinks. |

---

## 🏗️ Arquitectura de la "Fuente de Verdad"

Para evitar la fragmentación entre IDEs (VS Code, Android Studio, Claude Code), utilizamos **Symlinks**.

```
{{PROJECT_ROOT}}/
├── .agents/skills/           # ✨ FUENTE DE VERDAD (Originales)
├── .github/copilot/skills/   # → (Symlink) para VS Code / Copilot
├── .jetbrains/agent/skills/  # → (Symlink) para Android Studio / IntelliJ
├── .claude/skills/           # → (Symlink) para Claude Code
├── .cursor/skills/           # → (Symlink) para Cursor
├── .junie/skills/            # → (Symlink) para Junie
├── .antigravity/skills/      # → (Symlink) para Antigravity
└── .agent/skills/            # → (Symlink) genérico (otros agentes)
```

> [!IMPORTANT]
> **Regla de Oro:** No modifiques los symlinks de los editores (`.github/`, `.jetbrains/`, `.claude/`, etc.). Los cambios se hacen **siempre** en `.agents/skills/` y se sincronizan con el script `sync-skills.sh`.

---

## 🚀 Setup para Proyectos Nuevos

1. **Clona el scaffolding** con el nombre de tu proyecto y ejecuta el inicializador desde la raíz:
   ```bash
   ./init-project.sh
   ```
   El script renombra el proyecto y el package, reemplaza los placeholders (`{{PROJECT_NAME}}`, `{{PACKAGE_NAME}}`, `{{MODULE_NAME}}`) y crea los symlinks automáticamente. Guía completa en `SETUP.md`.
2. **Solo quieres la infraestructura de IA en un proyecto existente**: copia `.agents/` y `AGENTS.md`, actualiza los placeholders manualmente y ejecuta `./.agents/scripts/sync-skills.sh`.
3. **Validación**: Antes de cada commit, pide a la IA ejecutar `./gradlew formatAndAnalyze` (ktlint + detekt + Android Lint, ya configurados en este scaffolding).

---

## 💡 Consejos de Colaboración
- **Usa la IA como Arquitecto**: Si un patrón en la guía `android-best-practices` causa fricción, pide a la IA: *"Propón una mejora para esta skill basada en el nuevo patrón que implementamos"*.
- **Mantén las Reglas Claras**: Una IA trabaja mejor con prohibiciones explícitas (ej: "No uses wildcard imports").

---
**Standard Agent Infrastructure** — {{PROJECT_NAME}}
