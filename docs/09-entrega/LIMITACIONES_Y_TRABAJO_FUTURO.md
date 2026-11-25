# Limitaciones Conocidas y Trabajo Futuro — NEXUS

## 1. Limitaciones Actuales (Alcance B6)
- **División de gastos:** Reparto equitativo entre participantes seleccionados; la división por porcentajes o montos exactos por persona queda para una fase posterior.
- **Integraciones en entorno automatizado:** En la terminal local de validación, las credenciales reales de MySQL (`DB_PASSWORD`), Groq (`GROQ_API_KEY`) y Telegram (`TELEGRAM_BOT_TOKEN`) no estaban exportadas; por ello la validación automatizada cubre H2 + mocks, quedando la corrida en vivo con `nexus_demo`, Groq real y Telegram real sujeta a ejecución desde la sesión del propietario.
- **Telegram grupal:** Deshabilitado por defecto (`TELEGRAM_GROUPS_ENABLED=false`) por seguridad y privacidad.
- **Instalación PWA en iOS:** Requiere acción manual en Safari (*Compartir → Añadir a pantalla de inicio*).

## 2. Trabajo Futuro
1. División personalizada (porcentajes, cuotas fijas y partes).
2. Simplificación óptima de deudas en grafos grupales.
3. Procesamiento OCR/voz de facturas bajo el mismo `ResourceManager` y presupuesto `$0 COP`.
4. Auditoría de despliegue HTTPS con `server.servlet.session.cookie.secure=true`.
