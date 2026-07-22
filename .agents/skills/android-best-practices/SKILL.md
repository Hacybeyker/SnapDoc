---
name: android-best-practices
description: 'Android nativo + Jetpack Compose master skill. Vertical Slice Architecture + Clean Architecture + MVI, Hilt, Coroutines/Flow, Navigation 3 y Material 3. Cubre todas las capas, estrategias de testing y seguridad móvil.'
license: MIT
---

# Android + Jetpack Compose Master Skill — {{PROJECT_NAME}}

Esta es la guía definitiva para implementar funcionalidades en este proyecto. Cualquier implementación debe ser predecible, testeable y alineada con los estándares de producción de Google.

## 🏗️ Project Anatomy

```
{{MODULE_NAME}}/src/main/java/{{PACKAGE_PATH}}/
├── core/                      # Compartido SOLO si ≥2 features lo necesitan (YAGNI)
│   ├── di/                    #   módulos Hilt transversales
│   └── ui/theme/              #   design tokens (Color, Type, Shape, Spacing, Theme)
├── navigation/                # NavKeys @Serializable + AppNavHost (Navigation 3)
└── feature/                   # Vertical Slices (una por capacidad de negocio)
    └── {{feature_name}}/
        ├── domain/            # modelos, usecases, interfaces de repositorio. Kotlin PURO.
        ├── data/              # repositorio impl, sources (Room/DataStore/red), mappers, módulo Hilt
        └── ui/                # Screen (stateful) + Content (stateless), ViewModel MVI, UiState/Intents
```

**Dos reglas ortogonales y obligatorias:**
1. **Packaging = Vertical Slice** (feature-first): todo lo que una feature necesita vive junto bajo `feature/<name>/`.
2. **Regla de dependencia = Clean**: dentro de cada slice, `ui → domain ← data`. El domain no importa frameworks.

## 🛠️ Key Technical Patterns

### 1. The ViewModel Contract (MVI)
```kotlin
@HiltViewModel
class {{Feature}}ViewModel @Inject constructor(
    observeSomething: ObserveSomethingUseCase,
    private val doAction: DoActionUseCase
) : ViewModel() {

    val uiState: StateFlow<{{Feature}}UiState> = observeSomething()
        .map<Model, {{Feature}}UiState> { {{Feature}}UiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), {{Feature}}UiState.Loading)

    fun onIntent(intent: {{Feature}}Intent) { /* La lógica de negocio vive en los UseCases */ }
}
```
- **Un único `StateFlow<UiState>` inmutable por pantalla** con estados explícitos: `Loading / Empty / Content / Error`.
- Las interacciones entran como **intents** (`sealed interface`), no como métodos sueltos ad-hoc.
- **Inyecta** el `CoroutineDispatcher` cuando haya trabajo fuera del main (no hardcodees `Dispatchers.IO`).

### 2. Dependency Injection (Hilt)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class {{Feature}}DataModule {
    @Binds
    abstract fun bind{{Feature}}Repository(impl: Default{{Feature}}Repository): {{Feature}}Repository
}
```
- El módulo Hilt de una feature vive **dentro de esa feature** (`feature/<name>/data/`).
- ViewModels con `@HiltViewModel` + `@Inject`; se obtienen con `hiltViewModel()` en el `Screen`.
- Lo transversal (DB, Clock, dispatchers) se provee desde `core/di/` o `core/database/`.

### 3. SOLID & Patrones de Diseño
- **S**: UseCases y Mappers con una única responsabilidad.
- **O/L**: Modela jerarquías con `sealed interface` y composición, no herencia profunda.
- **I**: Interfaces de Repository pequeñas y específicas por feature.
- **D**: Domain define interfaces; Data las implementa; Hilt las conecta (`@Binds`).

Patrones recomendados: **Repository** (acceso a datos), **Factory** (builders de DB/HTTP), **Strategy** (variaciones de lógica), **Observer** (reactividad vía `Flow`/`StateFlow`).

## 📚 Deep Dive Guides
Para una comprensión profunda de cada área, consulta las guías de referencia:

1.  **[Architecture & Workflow](references/ARCHITECTURE_AND_WORKFLOW.md)**: Flujo de implementación por slice.
2.  **[UI & Styling Guide](references/UI_AND_STYLING_GUIDE.md)**: Compose Patterns, Material 3 y design tokens.
3.  **[Testing Strategies](references/TESTING_STRATEGIES.md)**: Tests unitarios con Fakes, Turbine y screenshot testing.
4.  **[Mobile Security Guide](references/MOBILE_SECURITY_GUIDE.md)**: Secretos, TLS/pinning, cifrado, R8 y hardening Android.

## ⛔ Hard Rules

Estas reglas no son negociables. Violarlas bloquea el merge:

- **Versiones estables obligatorias**: prohibido usar versiones `alpha`, `beta`, `rc` o `snapshot` en `libs.versions.toml`. Si la única versión disponible es pre-release, la librería no se incorpora (las excepciones existentes en el catálogo son deuda consciente y se migran al estabilizarse).
- **`libs.versions.toml` como única fuente de versiones**: ninguna dependencia declara su versión literal en un `build.gradle.kts`. Siempre `version.ref`.
- **Sin comentarios inline en `libs.versions.toml`**: las entradas del catálogo son autodescriptivas por su nombre.
- **Una feature nunca importa internals de otra feature**: la colaboración cruzada pasa por `core/` o contratos de domain.
- **Nada de lógica de negocio en Composables**: cálculos y validaciones viven en UseCases o el ViewModel.
- **Sin strings/colores/dp hardcodeados en UI**: strings desde `res/values/strings.xml`, estilos desde los tokens de `core/ui/theme/` (`MaterialTheme.spacing.*`, `colorScheme.*`, `typography.*`, `shapes.*`).

## ✅ Quality Checklist
- [ ] ¿El código pasa **ktlint**, **detekt** y **Android Lint**? (`./gradlew formatAndAnalyze`)
- [ ] ¿Se han incluido tests unitarios (JVM) usando Fakes que honran el contrato?
- [ ] ¿Las pantallas nuevas o modificadas tienen `@Preview` (light/dark) sobre el `Content` stateless?
- [ ] ¿El Domain es 100% independiente de Android/Compose/Hilt/Room?
- [ ] ¿Se respetan los principios SOLID (UseCases pequeños, dependencias hacia abstracciones)?
- [ ] ¿Toda dependencia nueva está en `libs.versions.toml` y es **estable**?
- [ ] ¿Se cumple el [Security Checklist](references/MOBILE_SECURITY_GUIDE.md) (sin secretos en duro, almacenamiento cifrado, R8)?

---
**Standard Android Skill** — Basado en Clean Architecture, Vertical Slices y patrones de industria.
