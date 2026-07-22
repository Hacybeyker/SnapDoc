# Architecture & Feature Workflow — Android + Compose

Esta guía define cómo estructurar y construir funcionalidades siguiendo **Vertical Slice Architecture** (packaging feature-first) con la **regla de dependencia de Clean Architecture** dentro de cada slice.

## 🏗️ Clean Architecture Layers (por slice)

### 1. Domain Layer (El Cerebro)
Lógica de negocio pura. Sin conocimiento de Android, Room, Compose ni Hilt.
- **Models**: data classes inmutables.
- **Use Cases**: una clase por responsabilidad (función `invoke`).
- **Repository Interface**: define qué datos se necesitan, no cómo se obtienen.

```kotlin
// feature/user/domain/User.kt
data class User(val id: String, val name: String)

// feature/user/domain/usecase/GetUserUseCase.kt
class GetUserUseCase @Inject constructor(private val repository: UserRepository) {
    suspend operator fun invoke(): Result<User> = repository.getUser()
}
```

> `@Inject constructor` es la única anotación tolerada en domain (es `javax.inject`, no Android).

### 2. Data Layer (Los Músculos)
Implementación técnica y gestión de datos.
- **Repository Impl**: orquesta sources locales (Room/DataStore) y/o remotas (Retrofit/Ktor).
- **Entities/DTOs**: modelos propios de la capa; **nunca** se filtran fuera de `data`.
- **Mappers**: transforman Entity/DTO ↔ Domain Model (extension functions en `data`).
- **Módulo Hilt de la feature**: `@Binds` de la interfaz de domain a la implementación.

### 3. UI Layer (La Cara)
UI y orquestación de estado (MVI).
- **ViewModel**: expone un único `StateFlow<UiState>`; recibe **intents**.
- **Composables**: divididos en `Screen` (stateful, obtiene el ViewModel con `hiltViewModel()`) y `Content` (stateless, con `@Preview`).

---

## 🚀 Workflow de Implementación (por Vertical Slice)

**Cada cambio es un slice vertical**: elige la feature primero y construye solo las clases que esa feature necesita, de arriba hacia abajo por sus propias capas — nunca "todo el domain de la app, luego todo el data".

1.  **Ubica la feature, no la capa**: capacidad nueva → `feature/<name>/` nuevo. Cambio a una existente → su carpeta. Solo lo genuinamente compartido toca `core/`.
2.  **Modela el domain del slice**: model + usecase + interfaz de repositorio en Kotlin puro, cada uno con su test unitario.
3.  **Implementa el `data` del slice**: sources (Entity+DAO si hay Room) + mapper + repositorio impl + módulo Hilt de la feature.
4.  **Conecta la UI del slice**: define `UiState` e intents; el ViewModel orquesta los usecases y expone `StateFlow`; el Composable consume estado y emite intents.
5.  **Navegación**: agrega el `NavKey @Serializable` en `navigation/` y registra la pantalla en `AppNavHost`.
6.  **Tests + verificación**: tests unitarios de la lógica nueva; `./gradlew formatAndAnalyze` y `./gradlew test` en verde.

---

## 🧭 Navegación Type-Safe (Navigation 3)

Las rutas se definen como objetos/clases `@Serializable` que implementan `NavKey` en `navigation/` y se registran en un único `AppNavHost` con `NavDisplay`. Las pantallas **no** conocen el back stack: reciben lambdas de navegación.

```kotlin
// navigation/Detail.kt
@Serializable data class Detail(val id: String) : NavKey

// navigation/AppNavHost.kt
entryProvider = entryProvider {
    entry<Home> { HomeScreen(onOpenDetail = { id -> backStack.add(Detail(id)) }) }
    entry<Detail> { key -> DetailScreen(id = key.id, onBack = { backStack.removeLastOrNull() }) }
}
```

---

## 🧱 Principios SOLID en la Práctica

| Principio | Aplicación en este stack |
|-----------|--------------------------|
| **SRP** | Un UseCase = una acción de negocio. Un Mapper = una transformación. |
| **OCP** | Nuevos comportamientos vía nuevas implementaciones de interfaces, no modificando código existente. |
| **LSP** | Los Fakes de test deben cumplir el mismo contrato que las implementaciones reales. |
| **ISP** | Repositorios por feature; evita interfaces "Dios" con decenas de métodos. |
| **DIP** | Domain define las interfaces; Data las implementa; Hilt las conecta con `@Binds`. |

**Reglas VSA adicionales:**
- Una *feature* es una **capacidad de negocio**, nunca un widget suelto.
- **Promueve a `core/` solo cuando ≥2 features lo necesitan de verdad** (YAGNI). Duplicación barata > acoplamiento prematuro.
- Si hay Room: el `@Database` es inherentemente cross-feature → vive en `core/database/`; cada feature **contribuye** su `@Entity` + DAO. Migraciones versionadas, **nunca** `fallbackToDestructiveMigration` en código real.

---

## 🛡️ Error Handling Strategy
Usa `Result` de Kotlin para propagar errores del Repositorio al ViewModel; los flujos reactivos usan `catch` y estados `Error` explícitos.

```kotlin
// Repositorio
override suspend fun getData(): Result<Data> = runCatching { remoteSource.fetch() }

// ViewModel
viewModelScope.launch {
    useCase().onSuccess { /* Update State */ }.onFailure { /* Emit Error state */ }
}
```

---
**Tip**: Mantén los UseCases pequeños. Si un UseCase supera ~100 líneas, probablemente estés mezclando lógica de negocio con lógica de datos.
