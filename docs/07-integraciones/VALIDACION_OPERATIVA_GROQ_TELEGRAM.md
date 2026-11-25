# Validación Operativa Groq y Telegram (Preflight B5)

**Bloque:** B5 — Identidad Visual, Experiencia de Usuario, PWA y Preflight Operativo  
**Rama:** `feature/acelerado-b5-experiencia-pwa`  
**Fecha:** 2026-11-25

---

## 1. Estado de Variables de Entorno en Preflight Local

Verificación realizada sin imprimir valores de secretos (`false` = no definida en la sesión actual):

| Variable | Estado en entorno local | Uso |
|---|---|---|
| `GROQ_API_KEY` | No configurada (`False`) | Credencial API Groq Free |
| `GROQ_ENABLED` | No configurada (`False`, default `false`) | Activación explícita de cliente Groq |
| `TELEGRAM_BOT_TOKEN` | No configurada (`False`) | Token del bot de Telegram |
| `TELEGRAM_ENABLED` | No configurada (`False`, default `false`) | Activación explícita del bot |
| `TELEGRAM_GROUPS_ENABLED` | Default endurecido a `false` | Soporte de grupos Telegram (desactivado por defecto) |

## 2. Comportamiento Seguro sin Credenciales (Modo Degradado)

- El núcleo financiero (autenticación, grupos, gastos, deudas, liquidaciones bilaterales, bandeja inteligente y PWA) opera al **100%** sin depender de Groq ni Telegram.
- Con `groq.enabled=false` y `telegram.enabled=false`, no se realizan llamadas salientes a proveedores externos.
- En el perfil `test`, `ExternalProvidersTestConfig` y `application-test.properties` garantizan aislamiento total offline.

## 3. Endurecimiento de Seguridad Aplicado en B5 (Consolidación B4)

1. **`TELEGRAM_GROUPS_ENABLED=false` por defecto:**
   - Actualizado en `application.properties`, `TelegramGroupBindingService.java` y `IntegracionesController.java`.
2. **Rate-limiting de vinculación Telegram:**
   - `TelegramRateLimiter.allowLinkingAttempt(telegramUserId)` y `recordLinkingFailure(telegramUserId)` limitan intentos fallidos de `/vincular` a **máximo 5 fallos cada 15 minutos (900 s)** por usuario de Telegram, integrado en `TelegramUpdateRouter.vincularYResponder(...)`.

## 4. Checklist para Activación en Demo / Entorno Controlado

Para habilitar Groq Free o Telegram en una demo controlada (fuera de esta sesión):
1. Definir `GROQ_ENABLED=true` y `GROQ_API_KEY=<clave>` en variables de entorno del sistema (nunca en código ni en `.properties` versionados).
2. Definir `TELEGRAM_ENABLED=true`, `TELEGRAM_BOT_TOKEN=<token>` y `TELEGRAM_WEBHOOK_SECRET=<secreto>` (si se usa webhook).
3. Mantener `TELEGRAM_GROUPS_ENABLED=false` salvo prueba explícita de chat grupal autorizada.
4. Verificar en `/integraciones` la generación de código temporal `NX-...` y en `/asistente` la creación de borradores pendientes de confirmación humana.
