# Guía de Configuración Gratuita de Groq — NEXUS

**Presupuesto autorizado:** `$0 COP`  
**Modelo candidato configurado por defecto:** `openai/gpt-oss-20b` (soporta JSON estructurado en Groq; límites gratuitos verificados de organización: `30 RPM`, `1.000 RPD`, `8.000 TPM`, `200.000 TPD`).

---

## 1. Variables de entorno

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `GROQ_ENABLED` | `false` | Activa (`true`) o desactiva (`false`) el adaptador `GroqProvider`. |
| `GROQ_API_KEY` | *(vacía)* | Clave de API de Groq (`console.groq.com`). **Nunca** escribirla en `application.properties` ni subirla a Git. |
| `GROQ_MODEL` | `openai/gpt-oss-20b` | Identificador del modelo en Groq. |

## 2. Activación y desactivación segura
- **Operación normal sin Groq (`$0 COP` garantizado):** Dejar `GROQ_ENABLED=false` y `GROQ_API_KEY` vacía. El sistema opera con `LocalRulesProvider` + registro manual al 100%.
- **Activación temporal en entorno local:**
  ```powershell
  $env:GROQ_ENABLED="true"
  $env:GROQ_API_KEY="tu_clave_sin_facturacion"
  $env:GROQ_MODEL="openai/gpt-oss-20b"
  .\mvnw.cmd spring-boot:run
  ```
- **Desactivación inmediata:** Fijar `GROQ_ENABLED=false` o limpiar `$env:GROQ_API_KEY=""`.
- **Protecciones integradas:** `ResourceManager` aplica límites conservadores (`20 RPM` global, `5 RPM` por usuario, `500 RPD`, reserva `10%`, concurrencia `2`) y abre el circuit breaker automáticamente ante HTTP `429`, `401` o `403`. *Nota:* `ResourceManager` controla cuotas en la app, pero no reemplaza los límites de facturación en la cuenta externa de Groq.
