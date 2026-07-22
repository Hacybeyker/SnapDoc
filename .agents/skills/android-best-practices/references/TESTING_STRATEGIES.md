# Testing Strategies — Android + Compose

Garantizamos la estabilidad del proyecto mediante tres niveles: tests unitarios JVM (`src/test`), tests de UI con Compose Test y **screenshot tests** para regresión visual.

## 🧪 Unit Testing (src/test — JVM, sin emulador)

### Regla de Oro: Fakes sobre Mocks
Prefiere **Fakes** (implementaciones reales pero controladas del contrato de domain) sobre mocks. Un fake que honra el contrato (LSP) produce tests que verifican comportamiento real, no interacciones.

### Estructura GIVEN / WHEN / THEN (Arrange–Act–Assert)
```kotlin
@Test
fun `submit name intent updates the greeting`() = runTest {
    // GIVEN
    val repository = FakeGreetingRepository(initialName = "Android")
    val viewModel = buildViewModel(repository)

    viewModel.uiState.test {
        skipItems(2) // Loading + Content inicial

        // WHEN
        viewModel.onIntent(HomeIntent.SubmitName(name = "Compose"))

        // THEN
        assertEquals("Compose", (awaitItem() as HomeUiState.Content).greeting.name)
    }
}
```

### Herramientas Imprescindibles
- **JUnit 4** + aserciones (`assertEquals`, `assertTrue`).
- **Turbine**: la mejor forma de testear `Flow` y `StateFlow`.
- **kotlinx-coroutines-test**: `runTest`, `StandardTestDispatcher`, `advanceUntilIdle`.
- **MainDispatcherRule** (en `core/test/`): swap de `Dispatchers.Main` para que `viewModelScope` funcione en JVM.

### Reglas
- Todo UseCase o lógica de ViewModel nueva **debe** llegar con su test.
- Un comportamiento por test; nombres que describen el caso.
- El domain se testea sin Android (Kotlin puro + fakes).

---

## 📱 UI Testing
- **Compose Test + Robolectric** (JVM, sin emulador) para lógica de UI de alto rendimiento.
- **Instrumented** (`src/androidTest`, `connectedAndroidTest`) solo para flujos que realmente requieren dispositivo.

```kotlin
@Test
fun shouldShowTitle_whenContentIsLoaded() = runComposeUiTest {
    setContent { HomeContent(uiState = fakeContent, onIntent = {}) }
    onNodeWithText("Hello, Android!").assertIsDisplayed()
}
```

---

## 📸 Screenshot Testing (Regresión Visual)

Cada pantalla relevante debe tener un screenshot test que la capture en sus estados clave (Loading, Content, Error, Empty). Verifica en `gradle/libs.versions.toml` qué herramienta usa el proyecto; las opciones estándar son:

- **Roborazzi** (recomendada): se ejecuta sobre Robolectric en JVM, sin emulador. Record con `recordRoborazziDebug`, verify con `verifyRoborazziDebug`.
- **Paparazzi**: alternativa JVM si el proyecto no usa Robolectric.

### Reglas
- Los tests se escriben **siempre contra el Composable `Content`** (stateless) con estados fake, nunca contra `Screen`.
- Cubre light/dark mode con datos deterministas y dimensiones fijas (render reproducible).
- Las golden images se versionan en el repo; cualquier diff visual debe ser intencional y revisado.

---

## ✅ Cobertura Recomendada
1. **ViewModels**: 100% de la lógica de flujo de estados.
2. **UseCases**: lógica de negocio crítica y validaciones.
3. **Mappers**: transformaciones Entity/DTO ↔ Domain.
4. **Screenshot tests**: estados clave de cada pantalla.

### Verificación antes de cerrar cualquier tarea
```bash
./gradlew formatAndAnalyze   # ktlintFormat → ktlintCheck + detekt + lint
./gradlew test               # tests unitarios JVM
```
