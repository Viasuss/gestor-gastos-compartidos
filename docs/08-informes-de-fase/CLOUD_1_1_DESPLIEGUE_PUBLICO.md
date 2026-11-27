# Fase Cloud 1.1 — Despliegue Público e Integraciones en Render (NEXUS)

**Rama de despliegue:** `feature/deployment-cloud`  
**URL pública:** `https://nexus-finanzas.onrender.com`  
**Base de datos:** TiDB Cloud (compatible con MySQL, TLS)  
**Presupuesto externo:** `$0 COP`

---

## 1. Variables de entorno en Render

| Variable | Obligatoria | Valor / Ejemplo | Descripción |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Sí | `cloud` | Activa `application-cloud.properties`. |
| `NEXUS_CLOUD_DB_URL` | Sí | `jdbc:mysql://<host>:4000/<db>?useSSL=true&requireSSL=true&serverTimezone=UTC` | Cadena JDBC TLS de TiDB Cloud. |
| `NEXUS_CLOUD_DB_USERNAME` | Sí | `<usuario_tidb>` | Usuario de TiDB Cloud. |
| `NEXUS_CLOUD_DB_PASSWORD` | Sí | `<secreto_tidb>` | Contraseña configurada en Render Secret/Env. |
| `GROQ_ENABLED` | Opcional | `true` / `false` | Habilita proveedor Groq Free (`false` por defecto). |
| `GROQ_API_KEY` | Si `GROQ_ENABLED=true` | `<clave_groq>` | Clave privada configurada manualmente en Render. |
| `GROQ_MODEL` | Opcional | `openai/gpt-oss-20b` | Modelo validado con fallback local determinista. |
| `TELEGRAM_ENABLED` | Opcional | `true` / `false` | Habilita long polling de Telegram (`false` por defecto). |
| `TELEGRAM_BOT_TOKEN` | Si `TELEGRAM_ENABLED=true` | `<token_bot>` | Token de `@NexusFinanzasBot`. |
| `TELEGRAM_BOT_USERNAME` | Si `TELEGRAM_ENABLED=true` | `NexusFinanzasBot` | Nombre de usuario del bot para deep links. |
| `TELEGRAM_GROUPS_ENABLED` | Opcional | `false` | Mantener en `false` (solo chat privado). |
| `TELEGRAM_POLL_TIMEOUT` | Opcional | `25` | Timeout (segundos) de `getUpdates`. |
| `TELEGRAM_MAX_CONCURRENT_UPDATES` | Opcional | `2` | Máximo de hilos concurrentes por ciclo de polling. |
| `GOOGLE_CLIENT_ID` | Opcional | `<client_id>.apps.googleusercontent.com` | Habilita botón de Google Sign-In en `/login` y `/registro`. |

> **Seguridad:** Nunca versionar credenciales reales en `.env`, `application*.properties` ni en GitHub. `nexus.demo.seed-enabled=false` permanece fijo en `application-cloud.properties`.

---

## 2. Procedimiento para activar Groq y Telegram sin conflictos HTTP 409

1. **Detener cualquier instancia local de Telegram antes de activar la nube:**
   - Si ejecutabas NEXUS en tu PC (perfil `demo` o local con `TELEGRAM_ENABLED=true`), detén la aplicación Java local (`Ctrl+C` o cierre de terminal/IDE) para evitar dos consumidores simultáneos haciendo `getUpdates` contra `@NexusFinanzasBot` (lo cual produce `409 Conflict`).
2. **Configurar variables en Render Dashboard → Environment:**
   - Cargar `GROQ_ENABLED=true` y `GROQ_API_KEY` (conservando `GROQ_MODEL=openai/gpt-oss-20b`).
   - Cargar `TELEGRAM_ENABLED=true`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME=NexusFinanzasBot` y `TELEGRAM_GROUPS_ENABLED=false`.
3. **Guardar y reiniciar el servicio en Render** (`Save Changes` o `Manual Deploy → Deploy latest commit`).

---

## 3. Limitaciones del plan gratuito de Render

- **Suspensión por inactividad:** Render Free suspende la instancia web tras ~15 minutos sin tráfico HTTP entrante.
- **Disponibilidad del bot de Telegram:** mientras la instancia está dormida, el hilo de *long polling* no consulta `getUpdates`; los mensajes enviados durante el reposo se procesan al despertar la aplicación mediante tráfico HTTP (y dentro de la ventana de retención de Telegram).
- **Cold start:** la primera petición tras reposo puede tardar ~30–60 segundos mientras arranca la JVM y Spring Boot.

---

## 4. Rollback seguro

Si fuera necesario revertir a la versión previa en `feature/deployment-cloud` (`b949b06`):
```bash
git checkout feature/deployment-cloud
git revert -m 1 <commit_integracion>
git push origin feature/deployment-cloud
```
O seleccionar un deploy previo desde el historial de Render Dashboard.
