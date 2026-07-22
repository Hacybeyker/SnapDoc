# UI & Styling Guide — Jetpack Compose + Material 3

Este documento detalla el sistema de diseño basado en **Material Design 3 (M3)**, los design tokens del proyecto y los patrones de Compose recomendados.

## 🎨 Design Tokens (obligatorio para cualquier UI)

Los tokens viven en `core/ui/theme/` (`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Theme.kt`). Regla **no negociable**: en un `@Composable` nunca se hardcodea `Color(0x…)`, un `.dp` mágico, un `.sp` crudo ni `FontFamily.Default`. Siempre se consume:

| Token | Acceso |
|-------|--------|
| Colores | `MaterialTheme.colorScheme.*` |
| Tipografía | `MaterialTheme.typography.*` |
| Formas | `MaterialTheme.shapes.*` |
| Espaciado | `MaterialTheme.spacing.*` (escala 4dp definida en `Spacing.kt`) |
| Strings | `stringResource(R.string.*)` — prohibido hardcodear texto |

### Theming Dinámico & Dark Mode
El theme del proyecto soporta colores dinámicos en Android 12+ y provee paletas light/dark propias como fallback:

```kotlin
val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```

- **Toda pantalla/componente funciona en light y dark**, con `@Preview` para cada modo.
- Si el branding exige colores estables, apaga `dynamicColor` por defecto (decisión por proyecto).

### Componentes M3 Críticos
- `Scaffold`: esqueleto base (topBar, snackbarHost, insets).
- `Surface`: fondo primario con elevación tonal.
- `OutlinedTextField`: campo de texto estándar con estados de error.

---

## 🏗️ Compose Best Practices

### Screen vs Content
- `Screen` (stateful): obtiene el ViewModel (`hiltViewModel()`), colecta el estado con `collectAsStateWithLifecycle()` y delega.
- `Content` (stateless): recibe `UiState` + lambdas de intents. **Los `@Preview` y los tests apuntan siempre al Content** con estados fake.

### State Hoisting
El estado reside en el ancestro común más bajo. Estado efímero de UI (texto de un input aún no confirmado) puede vivir en el Composable con `rememberSaveable`; el estado de negocio siempre en el ViewModel.

### Side Effects Management
- `LaunchedEffect`: disparadores únicos (ej. navegación tras éxito, snackbars one-shot).
- `rememberCoroutineScope`: para llamadas desde clics del usuario.
- `collectAsStateWithLifecycle`: obligatorio al coleccionar flows del ViewModel (evita trabajo en background).

### Optimización (Lazy Lists)
- **Key**: usa siempre el parámetro `key` en `items` para evitar recomposiciones costosas.
- **DerivedStateOf**: para cálculos basados en estados que cambian rápido (ej. scroll position).
- **Stability**: modelos de UI inmutables (`data class` + `List` read-only). Clases externas estables se declaran en `compose_stability.conf`.

---

## 📱 Accesibilidad
- **Content Description**: obligatorio en iconos sin label textual (o `null` explícito si es decorativo).
- **Touch target**: mínimo **48dp** para cualquier elemento clickable.
- **Color**: nunca comuniques significado solo con color; acompaña con icono/signo/texto.
- **Insets**: respeta `innerPadding` del `Scaffold` y `Modifier.safeDrawingPadding()` donde aplique (edge-to-edge activado).
