---
name: code-reviewer
description: 'Revisión de código contra los estándares y reglas del proyecto (AGENTS.md). Úsalo cuando el usuario pida: revisar el código implementado, hacer code review, verificar que los cambios cumplen la arquitectura (VSA + Clean + MVI), o validar el código antes de un commit/PR. Cubre: (1) review del diff actual o de una branch, (2) review de archivos/features específicos, (3) verificación del DoD (ktlint + detekt + lint + tests). Triggers: "revisa el código", "haz code review", "verifica los estándares", "revisa la feature X", "¿cumple con las reglas?".'
license: MIT
---

# Code Reviewer — Android Standards Review

> **Regla de oro**: El veredicto se emite contra `AGENTS.md`. Toda observación debe citar la regla que se incumple, no una preferencia personal.

---

## Phase 0 — Scope & Context

1. **Determina el alcance del review**:
   - Sin argumentos → diff del working tree + staged (`git diff HEAD`); si está limpio, diff de la branch actual vs `main`
   - Feature/archivos indicados → todos los archivos de ese slice o los archivos señalados
2. **Carga las reglas**:
   - Lee `AGENTS.md` (stack, reglas de arquitectura, hard rules)
   - Lee `.agents/skills/android-best-practices/references/ARCHITECTURE_AND_WORKFLOW.md`
   - **Si hay UI (Composables/theme)**: lee `references/UI_AND_STYLING_GUIDE.md`
   - **Si hay tests o falta cobertura**: lee `references/TESTING_STRATEGIES.md`
   - **Si hay auth/storage/network**: lee `references/MOBILE_SECURITY_GUIDE.md`

---

## Phase 1 — Review Checklist

Revisa cada archivo del alcance contra estas categorías. Cita `archivo:línea` en cada hallazgo.

### 1. Arquitectura (VSA + regla de dependencia Clean)
- [ ] El código vive en su slice (`feature/<name>/`), no en capas técnicas globales
- [ ] Dirección de dependencias: `ui → domain ← data` (domain no importa Android/Compose/Room/Hilt; solo `javax.inject`)
- [ ] ViewModels inyectan **UseCases**, nunca Repositorios directamente
- [ ] Entities/DTOs no se filtran fuera de `data` (siempre mapeados a Domain)
- [ ] Ninguna feature importa internals de otra feature
- [ ] Código en `core/` solo si ≥2 features lo usan (YAGNI)

### 2. MVI / State Management
- [ ] Un único `StateFlow<UiState>` por pantalla, con `sealed interface` de estados explícitos
- [ ] Acciones de usuario vía `sealed interface` de Intents que entran por `onIntent()`
- [ ] La UI reacciona a Flows (SSOT); sin refresh manual ni caches paralelos
- [ ] Sin lógica de negocio en Composables

### 3. Coding Standards
- [ ] `Screen` (stateful) separado de `Content` (stateless); `@Preview` solo en Content
- [ ] Sin strings hardcodeados (usar `stringResource`), sin colores/dp/sp hardcodeados (usar tokens de `core/ui/theme/`)
- [ ] Sin wildcard imports ni trailing commas
- [ ] Dependencias nuevas en `libs.versions.toml` con `version.ref`, solo versiones estables (nunca alpha/beta/rc/snapshot)

### 4. Testing
- [ ] Lógica nueva (UseCases, ViewModels, Mappers) tiene tests unitarios con Fakes (no mocks)
- [ ] Los Fakes honran el contrato de la interfaz real
- [ ] Flows testeados con Turbine verificando la secuencia de estados

### 5. Seguridad (Hard Rules)
- [ ] Sin secretos hardcodeados (API keys, tokens, passwords) ni credenciales en texto plano
- [ ] Sin `fallbackToDestructiveMigration` (migraciones Room versionadas)

---

## Phase 2 — Automated Verification

Ejecuta y reporta el resultado (no corrijas fallos, solo repórtalos):

```bash
./gradlew formatAndAnalyze   # ktlint + detekt + Android Lint
./gradlew test               # tests unitarios JVM
```

Si el usuario pide un review "rápido" o solo de lectura, omite esta fase y márcala como SKIPPED.

---

## Phase 3 — Report

Presenta los hallazgos agrupados por severidad:

| Severidad | Criterio |
|-----------|----------|
| ❌ **Crítico** | Viola una Hard Rule o rompe la regla de dependencia — bloquea el merge |
| ⚠️ **Advertencia** | Incumple un estándar (naming, strings, tokens, tests faltantes) — corregir antes del commit |
| 💡 **Sugerencia** | Mejora opcional (claridad, patrón más idiomático) — a criterio del autor |

**Formato de cada hallazgo**: `archivo:línea` — qué está mal, qué regla incumple (sección de `AGENTS.md` o guía), y cómo corregirlo (breve).

**Veredicto final**:
- ✅ **APPROVED** — sin críticos ni advertencias, DoD en verde
- 🔄 **CHANGES REQUESTED** — lista priorizada de correcciones

```
⛔ Este skill NO modifica código — solo reporta. Si el usuario pide aplicar
   las correcciones, usa el workflow de feature-implementation.
```
