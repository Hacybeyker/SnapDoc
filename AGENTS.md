# AGENTS.md — SnapDoc

> Este archivo sigue el estándar [agents.md](https://agents.md/) y es la **Fuente de Verdad** para agentes de IA en este proyecto **Android nativo con Jetpack Compose**.

---

## 🛠️ Tech Stack & Source of Truth

> [!IMPORTANT]
> No asumas versiones. Consulta **SIEMPRE** `gradle/libs.versions.toml` para dependencias y versiones de plugins.

| Categoría | Estándar |
|-----------|----------|
| **UI** | Jetpack Compose + Material Design 3 |
| **Arquitectura** | Vertical Slice Architecture (feature-first) + regla de dependencia Clean + MVI |
| **Navegación** | Navigation 3 con `NavKey` type-safe (`@Serializable`) |
| **DI** | Hilt (módulo Hilt por feature, `@Binds` a interfaces de domain) |
| **Persistencia Local** | Room para datos estructurados + DataStore para preferencias (agregar al catálogo cuando se necesiten) |
| **Concurrencia** | Kotlin Coroutines + Flow (`StateFlow` en ViewModels, dispatchers inyectados) |
| **Testing** | JUnit + Turbine + Fakes (unitarios JVM) / Screenshot Testing (UI) |
| **Principios** | SOLID + Patrones de Diseño (Repository, Factory, Observer, etc.) |
| **Calidad** | ktlint + detekt + Android Lint |

---

## 📁 Project Layout

```
app/src/main/java/com/hacybeyker/snapdoc/
├── core/                  # Compartido SOLO si ≥2 features lo necesitan (YAGNI)
│   └── ui/theme/          #   design tokens (Color, Type, Shape, Spacing, Theme)
├── navigation/            # NavKeys @Serializable + AppNavHost (Navigation 3)
└── feature/<name>/        # un Vertical Slice por capacidad de negocio
    ├── domain/            #   modelos, usecases, interfaces de repos. Kotlin PURO.
    ├── data/              #   sources + mappers + repos impl + módulo Hilt de la feature
    └── ui/                #   Screen/Content Compose, ViewModel MVI, UiState/Intents
```

- **Package base**: `com.hacybeyker.snapdoc`
- **Build**: `./gradlew assembleDebug` · **Tests**: `./gradlew test`
- **Calidad (obligatorio antes de commit)**: `./gradlew formatAndAnalyze` (ktlint + detekt + Android Lint)

---

## 🏗️ Project Architecture Rules

### 1. Vertical Slice + Dependencias Unidireccionales
Packaging **por feature**, nunca por capa técnica. Dentro de cada slice: `ui → domain ← data`.
- **Domain**: Kotlin puro. Prohibido depender de frameworks (Android, Compose, Room, Hilt — solo `javax.inject`).
- **ViewModels**: inyectar UseCases. **Prohibido** inyectar Repositorios directamente.
- **Una feature nunca importa internals de otra feature**: colaboración vía `core/` o contratos de domain.
- **Promueve a `core/` solo cuando ≥2 features lo necesitan** (YAGNI).

### 2. State management (MVI)
Cada pantalla es dirigida por un estado inmutable:
- **UiState**: `sealed interface` con estados explícitos (`Loading / Empty / Content / Error`), expuesto como un único `StateFlow`.
- **Intent**: `sealed interface` para acciones del usuario (entran por `onIntent()`).
- La UI **reacciona** a los Flows (SSOT); no refresca a mano ni mantiene caches paralelos.

### 3. Coding Standards
- **Composables**: PascalCase. Separar `Screen` (stateful) de `Content` (stateless). `@Preview` solo en Content.
- **Strings**: prohibido hardcodear. Usar `stringResource(R.string.*)`.
- **Estilos**: prohibido hardcodear colores/dp/sp en Composables. Usar los tokens de `core/ui/theme/` (`MaterialTheme.colorScheme/typography/shapes/spacing`).
- **Imports**: prohibidos los wildcards (`import x.*`) y las trailing commas (ktlint lo aplica en el build).
- **Dependencias**: siempre en `gradle/libs.versions.toml` con `version.ref`; solo versiones **estables**.

### 4. SOLID & Patrones de Diseño
- **SRP**: una clase, una responsabilidad (UseCases pequeños, un Mapper por transformación).
- **DIP**: las capas superiores dependen de abstracciones (interfaces de Repository en domain, `@Binds` en data).
- **OCP/ISP/LSP**: prefiere `sealed interface` y composición sobre herencia; repositorios pequeños por feature; los Fakes de test honran el contrato.
- Aplica patrones donde aporten claridad (Repository, Factory, Strategy, Observer vía `Flow`), nunca por moda.

---

## 🚀 AI Interaction Workflow

Cualquier agente de IA que trabaje en este proyecto **DEBE** seguir estas guías maestras:

1. **Arquitectura & Workflow**: [ARCHITECTURE_AND_WORKFLOW.md](.agents/skills/android-best-practices/references/ARCHITECTURE_AND_WORKFLOW.md)
2. **Guía de UI & Styling**: [UI_AND_STYLING_GUIDE.md](.agents/skills/android-best-practices/references/UI_AND_STYLING_GUIDE.md)
3. **Calidad & Testing**: [TESTING_STRATEGIES.md](.agents/skills/android-best-practices/references/TESTING_STRATEGIES.md)
4. **Seguridad Móvil**: [MOBILE_SECURITY_GUIDE.md](.agents/skills/android-best-practices/references/MOBILE_SECURITY_GUIDE.md)

Para implementar una **feature / issue / bug / enhancement / fix / refactor** de punta a punta, sigue el workflow de la skill [feature-implementation](.agents/skills/feature-implementation/SKILL.md) (fases: contexto → snapshot → implementación → DoD → reporte).

Para **revisar código implementado** (code review contra los estándares de este archivo), sigue el workflow de la skill [code-reviewer](.agents/skills/code-reviewer/SKILL.md) (fases: alcance → checklist → verificación automatizada → reporte por severidad).

---

## 📝 Workflow por cambio (resumen)

1. **Ubica la feature, no la capa** (capacidad nueva → `feature/<name>/` nuevo).
2. **Domain primero**: model + usecase + interfaz, con tests unitarios.
3. **Data del slice**: sources + mapper + repo impl + módulo Hilt.
4. **UI del slice**: UiState/Intents + ViewModel + Screen/Content; registra el NavKey en `AppNavHost`.
5. **Tests**: unitarios para la lógica nueva; screenshot test si hay UI visual relevante.
6. **Verifica**: `./gradlew formatAndAnalyze` y `./gradlew test` en verde.
7. **Documenta**: entrada en `CHANGELOG.md` bajo `[Unreleased]` (`Added/Fixed/Changed/Enhancement/Security`).

**Commits — agrupa por unidad funcional, no por archivo.** Típicamente por capa (`domain` / `data` / `ui`) o sub-objetivo; cada commit compila y pasa calidad + tests. Una feature/fix a la vez.

---

## 🚫 Prohibiciones Críticas (Hard Rules)
- ❌ **NO** omitas la capa de dominio (UseCases).
- ❌ **NO** expongas Entities/DTOs fuera de la capa de datos (mapear siempre a Domain).
- ❌ **NO** implementes lógica de negocio en Composables.
- ❌ **NO** uses `@Preview` en funciones de Screen (solo en Content con fakes).
- ❌ **NO** uses `fallbackToDestructiveMigration` en código real (migraciones Room versionadas).
- ❌ **NO** hardcodees secretos (API keys, tokens, passwords) ni guardes credenciales en texto plano. Ver [Guía de Seguridad](.agents/skills/android-best-practices/references/MOBILE_SECURITY_GUIDE.md).
- ❌ **NO** agregues dependencias `alpha/beta/rc/snapshot` al catálogo.

---
**Standard Android Config** — SnapDoc
