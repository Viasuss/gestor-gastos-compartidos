# Limitaciones Conocidas y Trabajo Futuro — NEXUS (`v1.0.0`)

## 1. Limitaciones Actuales (Entrega Universitaria `v1.0.0`)
1. **Validación real de Groq con gasto completo:** En vivo se acreditó la interpretación incompleta (`GROQ`, `REQUIERE_ACLARACION`, `1577 ms`); la interpretación completa con monto usando `GROQ` permanece cubierta mediante mocks (`GroqProviderAndTelemetryTest`) y pendiente de validación real adicional.
2. **Telegram grupal:** Permanece deshabilitado por defecto (`TELEGRAM_GROUPS_ENABLED=false`) por seguridad y privacidad; su cobertura actual es simulada.
3. **Instalación física de PWA:** Validada mediante `MockMvc` (`B5ExperienceAndPwaTest`) y análisis estático (`manifest.webmanifest`, `sw.js` con `STATIC_ALLOWLIST`, `offline.html`); la instalación física en dispositivo/navegador real no fue acreditada en esta sesión.
4. **Verificación directa en MySQL de los registros finales de M01:** Validada mediante respuestas operativas reales de NEXUS Web y capturas reales de Telegram confirmadas por el propietario; la consulta SQL directa de cierre sobre `nexus_demo` queda pendiente si no existe acceso autorizado desde consola.
5. **Normalización monetaria definitiva:** La corrección que evita enviar `.00` en `bandeja.html` y acepta `.00` exacto en `MoneyParser.parseCopAcceptingZeroDecimals` está cubierta por pruebas automatizadas (`BandejaWebConfirmTest`, `B63AndB64FixesTest`), pendiente de una nueva confirmación operativa sin edición manual del monto.
6. **División de gastos:** Reparto equitativo entre participantes seleccionados; la división personalizada queda para trabajo futuro.

## 2. Trabajo Futuro
1. División personalizada (porcentajes, cuotas fijas y partes).
2. Simplificación óptima de deudas en grafos grupales.
3. Procesamiento OCR/voz de facturas bajo `ResourceManager` y presupuesto `$0 COP`.
4. Auditoría de despliegue HTTPS con `server.servlet.session.cookie.secure=true`.
