# Mobile Security Guide — Android

Toda implementación debe asumir que **el APK será decompilado y el tráfico interceptado**. El objetivo: aunque alguien haga ingeniería inversa del binario, no debe obtener claves, secretos ni acceso a datos sensibles.

> [!IMPORTANT]
> La seguridad no es una feature opcional ni un paso final: se aplica en cada capa desde el primer commit. Ante la duda, sigue el estándar **[OWASP MASVS](https://mas.owasp.org/MASVS/)**.

---

## 🔑 1. Secretos y Claves (Regla #1: nada en duro)

- ❌ **PROHIBIDO** hardcodear API keys, tokens, passwords o URLs sensibles en código Kotlin, recursos o `strings.xml`. Los strings del binario se extraen en segundos con `strings`/`jadx`.
- ✅ Define los secretos en `local.properties` (fuera del repo, en `.gitignore`) e inyéctalos como `BuildConfig` fields:

```kotlin
// app/build.gradle.kts — lee de local.properties o variables de entorno (CI)
val apiKey = localProperties.getProperty("API_KEY") ?: System.getenv("API_KEY") ?: ""
buildConfigField("String", "API_KEY", "\"$apiKey\"")
```

- ⚠️ `BuildConfig` **dificulta** pero no oculta: el valor sigue dentro del binario. Para secretos realmente críticos:
  - **Mejor opción**: que el secreto **nunca viaje en la app**. Muévelo a tu backend (proxy de API) y protege el endpoint con autenticación del usuario.
  - Si debe estar en el dispositivo, guárdalo cifrado tras el primer uso (ver §3) o recupéralo en runtime desde el backend tras autenticar.
- ✅ Revisa antes de cada commit que no se filtren secretos (`git diff` + herramientas como `gitleaks` en CI).

---

## 🌐 2. Red: TLS y SSL Pinning

- ✅ **Solo HTTPS/TLS**. Prohibido `http://` en cualquier entorno, incluido debug contra servicios reales.
- ✅ Aplica **Certificate/Public-Key Pinning** en el cliente HTTP (OkHttp/Retrofit/Ktor-OkHttp) para mitigar MitM:

```kotlin
val pinner = CertificatePinner.Builder()
    .add("api.midominio.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .add("api.midominio.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=") // backup pin
    .build()

OkHttpClient.Builder().certificatePinner(pinner).build()
```

- ✅ Incluye siempre **al menos un pin de respaldo** (certificado futuro) para no bloquear la app al rotar certificados.
- ❌ Nunca deshabilites la validación TLS (`trustAll`) ni en builds de debug que lleguen a testers.
- ✅ Restringe el tráfico con `networkSecurityConfig` (`cleartextTrafficPermitted="false"`).

---

## 💾 3. Almacenamiento Seguro en el Dispositivo

| Tipo de dato | Dónde guardarlo |
|--------------|-----------------|
| Tokens, credenciales, secretos | **Android Keystore** (AES/GCM) — la clave nunca vive en el código |
| Preferencias no sensibles | DataStore (sin cifrar está bien) |
| Datos estructurados sensibles | Room + **SQLCipher** (passphrase generada y custodiada por Keystore) |

### Reglas
- ❌ Nunca guardes tokens/credenciales en DataStore o Room **en texto plano**.
- ✅ Cifrado simétrico: **AES-256-GCM** con claves generadas y custodiadas por el Keystore.
- ✅ Usa `setUserAuthenticationRequired(true)` en claves que protegen datos de alto valor (combinado con `androidx.biometric`).
- ✅ Excluye datos sensibles de los backups (`android:allowBackup="false"` o `dataExtractionRules`/`backup_rules`).
- ✅ Limpia los secretos al cerrar sesión.

---

## 🕵️ 4. Ofuscación y Endurecimiento del Binario (R8)

- ✅ Activa en todo build de release: minificación + shrink de recursos (con AGP 9, bloque `optimization { enable = true }` o `isMinifyEnabled = true` + `isShrinkResources = true` según DSL).
- ✅ Mantén las reglas keep mínimas: cada `-keep` innecesario es código sin ofuscar. Revisa el `mapping.txt` generado (súbelo a tu crash reporter, nunca al repo público).
- ✅ `kotlinx.serialization`/Room/Hilt traen sus reglas; solo agrega `-keep` propios cuando la reflexión lo exija de verdad.

---

## 🚫 5. Higiene de Código y Datos en Runtime

- ❌ **Logs**: prohibido loguear tokens, PII o cuerpos de request/response en release. Usa un logger desactivable por build type.
- ❌ No expongas datos sensibles en mensajes de error, analytics ni crash reports.
- ✅ Oculta contenido sensible en el app switcher y bloquea capturas en pantallas críticas (`FLAG_SECURE`).
- ✅ Valida y sanitiza **deep links**: nunca confíes en parámetros externos para saltar autenticación o navegar a pantallas privilegiadas.
- ✅ WebViews: deshabilita JavaScript si no se necesita; nunca cargues URLs arbitrarias ni expongas bridges (`addJavascriptInterface`) sin validación.
- ✅ `android:exported="false"` por defecto en Activities/Services/Receivers que no necesiten ser públicos.

---

## 🛡️ 6. Defensas Adicionales (según criticidad de la app)

Para apps con datos de alto valor (fintech, salud), considera además:

- **Root detection**: degradar funcionalidad o alertar en dispositivos comprometidos (ej. RootBeer). Es disuasión, no garantía.
- **Detección de debugger/hooking** (Frida, Xposed): chequeos en runtime para apps críticas.
- **Integridad de la app**: **Play Integrity API** para que el backend verifique que habla con una app legítima no modificada.
- **RASP / App shielding** comercial si el negocio lo justifica.

---

## ✅ Security Checklist (antes de cada release)

- [ ] ¿Cero secretos hardcodeados? (busca `apiKey`, `password`, `secret`, `token` en código y recursos)
- [ ] ¿SSL Pinning activo y con pin de respaldo en todos los clientes HTTP?
- [ ] ¿Tokens y credenciales en Keystore, nunca en texto plano?
- [ ] ¿R8/ofuscación habilitada en release y reglas keep justificadas?
- [ ] ¿Logging de red y logs de debug deshabilitados en release?
- [ ] ¿Deep links y WebViews validados?
- [ ] ¿Backups excluyen datos sensibles?
- [ ] ¿Se decompiló el release (jadx) para verificar que no se lee nada sensible?

---
**Referencia maestra**: [OWASP Mobile Application Security (MAS)](https://mas.owasp.org/) — MASVS para requisitos y MASTG para técnicas de verificación.
