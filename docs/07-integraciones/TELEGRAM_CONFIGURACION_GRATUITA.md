# Guía de Configuración Gratuita de Telegram Bot API — NEXUS

**Presupuesto autorizado:** `$0 COP` (Long Polling sin dominio público ni webhooks de pago).

---

## 1. Variables de entorno

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `TELEGRAM_ENABLED` | `false` | Activa (`true`) o desactiva (`false`) el servicio de Long Polling. |
| `TELEGRAM_BOT_TOKEN` | *(vacía)* | Token del bot emitido por `@BotFather`. **Nunca** escribirlo en `application.properties` ni subirlo a Git. |
| `TELEGRAM_BOT_USERNAME` | *(vacía)* | Username del bot (sin `@` o con `@`) para generar el enlace profundo en `/integraciones`. |
| `TELEGRAM_POLL_TIMEOUT` | `25` | Timeout en segundos de `getUpdates` (HTTP read timeout = `pollTimeout + 10s`). |
| `TELEGRAM_MAX_CONCURRENT_UPDATES` | `2` | Concurrencia máxima configurada. |
| `TELEGRAM_GROUPS_ENABLED` | `true` | Habilita o deshabilita vinculación en chats grupales (`/vincular_grupo`). |

## 2. Activación local segura
1. Crear bot gratuito en Telegram con `@BotFather` (`/newbot`), manteniendo **Privacy Mode activado** (`/setprivacy -> Enable`).
2. Configurar variables de entorno en la sesión local (sin guardar en Git):
   ```powershell
   $env:TELEGRAM_ENABLED="true"
   $env:TELEGRAM_BOT_TOKEN="tu_token_de_botfather"
   $env:TELEGRAM_BOT_USERNAME="TuBotNEXUS"
   .\mvnw.cmd spring-boot:run
   ```
3. Entrar en NEXUS Web a `/integraciones`, pulsar **Vincular Telegram**, y enviar `/vincular <CODIGO>` al bot en chat privado.
