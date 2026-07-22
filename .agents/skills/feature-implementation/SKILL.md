---
name: feature-implementation
description: 'Workflow completo de implementación Android. Úsalo cuando el usuario pida: crear una feature, resolver un bug/issue, aplicar un fix, hacer un enhancement, o refactorizar código. Cubre: (1) nuevas features como Vertical Slice con todas las capas (domain → data → ui), (2) bugfixes con análisis de causa raíz, (3) enhancements, (4) refactors. Siempre verifica el DoD completo (ktlint + detekt + lint + tests + compilación) y genera un reporte HTML. Triggers: "crea la feature X", "fix bug Y", "implementa Z", "enhance/mejora X", "refactoriza Y", "resuelve el issue #N".'
license: MIT
---

# Feature Implementation — Android Workflow

> **Regla de oro**: Lee `AGENTS.md` antes de escribir una sola línea de código. Toda implementación sigue las reglas allí definidas.

---

## Phase 0 — Context Loading

Carga siempre antes de implementar:

1. Lee `AGENTS.md` (reglas del proyecto, stack, hard rules)
2. Lee `.agents/skills/android-best-practices/SKILL.md`
3. Lee `.agents/skills/android-best-practices/references/ARCHITECTURE_AND_WORKFLOW.md`
4. Lee `.agents/skills/android-best-practices/references/TESTING_STRATEGIES.md`
5. **Si hay cambios de UI**: Lee `references/UI_AND_STYLING_GUIDE.md`
6. **Si hay seguridad (auth, storage, network)**: Lee `references/MOBILE_SECURITY_GUIDE.md`

Determina el tipo de tarea: `feature | bugfix | enhancement | refactor`

---

## Phase 1 — Snapshot (Before)

Antes de modificar cualquier archivo:

- Lee cada archivo que **vas a modificar** y anota su contenido completo como "estado anterior"
- Lista los archivos que **vas a crear** (no tienen estado anterior)
- Lista los archivos que **vas a eliminar** (solo su contenido anterior)

Este snapshot es obligatorio para el reporte HTML.

---

## Phase 2 — Implementation

### 2A. Feature / Enhancement

Cada feature es un **Vertical Slice**: elige la feature primero y construye solo lo que necesita, de arriba hacia abajo por sus propias capas.

```
feature/{{name}}/
├── domain/
│   ├── Model.kt            ← 1. Modelo de dominio (data class inmutable, Kotlin puro)
│   ├── Repository.kt       ← 2. Interfaz del repositorio (contrato)
│   └── usecase/UseCase.kt  ← 3. UseCase (función invoke, @Inject constructor)
├── data/
│   ├── source/             ← 4a. Entity+DAO (Room) / DTO (red) — siempre internos a data
│   ├── Mapper.kt           ← 4b. Mapper Entity/DTO → Domain (extension fun)
│   ├── RepositoryImpl.kt   ← 4c. Implementación del repositorio
│   └── DataModule.kt       ← 5. Módulo Hilt de la feature (@Binds)
└── ui/
    ├── UiState.kt          ← 6a. sealed interface UiState (Loading/Content/Error) + Intents
    ├── ViewModel.kt        ← 6b. @HiltViewModel: StateFlow<UiState> + onIntent()
    └── Screen.kt           ← 6c. Screen (stateful, hiltViewModel) + Content (stateless + @Preview)
```

**Reglas críticas de implementación**:
- El domain **no importa** Android/Compose/Hilt/Room (solo `javax.inject`)
- Entities/DTOs nunca se filtran fuera de `data` (mapear siempre a Domain)
- El ViewModel solo inyecta UseCases, nunca Repositorios directamente
- Usa `Result<T>` para propagar errores del Repositorio al ViewModel
- Registra la pantalla en `navigation/AppNavHost.kt` con su `NavKey @Serializable`
- Una feature nunca importa internals de otra feature (colabora vía `core/` o contratos de domain)
- Strings desde `res/values/strings.xml`; estilos desde los tokens de `core/ui/theme/`

### 2B. Bug Fix

1. Lee el código afectado y reproduce el bug mentalmente
2. Escribe un test que falle (reproduce el bug) — ANTES de fijar
3. Implementa el fix mínimo necesario
4. Verifica que el test ahora pase
5. Revisa si hay bugs relacionados en el mismo área

### 2C. Refactor

1. Verifica que existen tests para el código a refactorizar; si no, créalos primero
2. Aplica los cambios preservando el comportamiento observable
3. Confirma que todos los tests existentes siguen pasando

---

## Phase 3 — Definition of Done (OBLIGATORIO)

Ejecuta en orden. Si un paso falla: **corrige el error y vuelve a ejecutar ese paso y todos los siguientes**.

```bash
# 1. Formatear y verificar calidad (ktlint + detekt + Android Lint)
./gradlew formatAndAnalyze

# 2. Tests unitarios (JVM — sin emulador)
./gradlew test

# 3. Compilación Android (debug)
./gradlew assembleDebug
```

**DoD pasa cuando**: todos los pasos terminan con BUILD SUCCESSFUL.

---

## Phase 4 — HTML Report Generation

Al terminar el DoD, genera el reporte:

1. **Destino**: `reports/YYYY-MM-DD-{task-slug}.html`
   Ejemplo: `reports/2026-06-12-user-profile-feature.html`

2. **Agrega `reports/` al `.gitignore`** si no está ya presente

3. **Escribe el HTML** siguiendo exactamente la estructura de `assets/report-template.html`

4. **Contenido requerido** (ver template para el HTML exacto):

| Sección | Contenido |
|---------|-----------|
| **Header** | Nombre del proyecto, tipo de tarea (badge), fecha, descripción |
| **Resumen** | Qué se hizo, por qué, scope (feature name), archivos totales |
| **Archivos Nuevos** | Por cada archivo nuevo: ruta + contenido completo con syntax highlight |
| **Archivos Modificados** | Por cada archivo modificado: ruta + bloque ANTES + bloque DESPUÉS |
| **Archivos Eliminados** | Por cada archivo borrado: ruta + contenido previo |
| **DoD** | Cada paso con ícono ✅ PASS / ❌ FAIL / ⏭️ SKIPPED y output relevante |
| **Footer** | Proyecto, fecha, generado por Claude Code |

5. **Al final**, informa al usuario la ruta del reporte:
   ```
   📄 Reporte generado: reports/YYYY-MM-DD-{task-slug}.html
   ```

---

## Phase 5 — Final Handoff

```
⛔ NO hagas commit automático — el usuario debe revisar los cambios primero.
```

Presenta al usuario:
- Resumen de qué se implementó (2-3 líneas)
- Lista de archivos creados / modificados
- Resultado del DoD (PASS / FAIL / SKIPPED por paso)
- Ruta del reporte HTML
- Comando de commit sugerido (para que el usuario lo ejecute manualmente)

---

## Quick Reference — Checklist

- [ ] `AGENTS.md` y referencias de `android-best-practices` leídas
- [ ] Snapshot "before" capturado para todos los archivos a modificar
- [ ] Vertical Slice respetado (todo bajo `feature/<name>/`, `ui → domain ← data`)
- [ ] Entities/DTOs no se filtran fuera de `data`
- [ ] Tests escritos en `src/test` usando Fakes (no mocks)
- [ ] `@Preview` solo sobre el `Content` stateless
- [ ] Módulo Hilt de la feature creado (`@Binds`) y NavKey registrado en `AppNavHost`
- [ ] `CHANGELOG.md` actualizado bajo `[Unreleased]`
- [ ] `./gradlew formatAndAnalyze` — PASS
- [ ] `./gradlew test` — PASS
- [ ] `./gradlew assembleDebug` — PASS
- [ ] Reporte HTML generado en `reports/`
- [ ] **NO commit automático**
