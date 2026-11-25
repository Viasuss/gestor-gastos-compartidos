# B4 — INFORME TÉCNICO DE CONECTIVIDAD INTELIGENTE MEDIANTE TELEGRAM BOT API

**Proyecto:** NEXUS (anteriormente Partly)  
**Rama:** `feature/acelerado-b4-telegram` (desde `feature/acelerado-b3-inteligencia` @ `6feac284be077c04c2ae45d7e661d32d7a4a73b3`)  
**Fecha:** 2026-11-25

---

## 1. Resumen ejecutivo
Se implementó el canal de conectividad **Telegram Bot API** mediante **Long Polling** (`$0 COP`, sin dependencias pesadas), vinculación segura por códigos SHA-256 de un solo uso, comandos privados y grupales, inline keyboards (`Confirmar`, `Descartar`, `Seleccionar grupo`), reutilización atómica de `BorradorOperacionService` y `GastoService`, consultas deterministas con `DeudaService`, y la vista web `/integraciones`. Adicionalmente, se consolidó el control explícito de **TPM** en `ResourceManager`.

## 2. Consolidación preventiva (CP1) y arquitectura B4
- **`ResourceManager`:** Añadido control de ventana deslizante **TPM** (`nexus.ai.resource.tpm-limit=6000` con reserva `10%`).
- **Persistencia (`V4__telegram_conectividad.sql`):** `CuentaCanal`, `CodigoVinculacion`, `VinculacionChatGrupo`, `TelegramUpdateProcesado`.
- **Servicios (`service/telegram/`):** `TelegramBotClient` (`RestTemplate`), `TelegramPollingService` (`SmartLifecycle`, manejo de `429`/`401`/`403`/`409` y apagado limpio), `TelegramAccountLinkService` (SHA-256 + `SecureRandom`), `TelegramGroupBindingService`, `TelegramRateLimiter`, `TelegramUpdateRouter`.
- **Web:** `IntegracionesController` + `integraciones.html` (`/integraciones`, `/integraciones/telegram/codigo`, `/integraciones/telegram/desvincular`).

## 3. Pruebas y verificación
- **Suite ejecutada (`.\mvnw.cmd -o clean test` en entorno temporal):** **39 pruebas** (`39` aprobadas, `0` fallos, `0` errores, `0` omisiones).
- **Distinción de evidencia:** `TelegramBotClient` fue simulado con `@MockitoBean` en `TelegramIntegrationTest`; no se realizaron llamadas HTTP reales a `api.telegram.org` ni se inició polling en pruebas (`TELEGRAM_ENABLED=false`).
