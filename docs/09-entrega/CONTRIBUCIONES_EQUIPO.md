# Contribuciones del Equipo — NEXUS

- **Johan Viasus (Desarrollador A):**
  - Entorno aislado de pruebas `test` (`F1.1.A`) y caracterización de `DeudaService`.
  - Bloque `B1`: Spring Security, CSRF, `LoginAttemptService`, `GrupoAccessService`, `MoneyInputParser`, mitigación *pre-hijacking* Google.
  - Bloque `B2`: Liquidaciones bilaterales (`Liquidacion`, `LiquidacionService`, `LiquidacionController`) y Flyway `V1`–`V2`.
  - Bloque `B3`: `ResourceManager`, `GroqProvider`, `LocalRulesProvider`, `BorradorOperacionService`, Bandeja Inteligente `/asistente` y Flyway `V3`.
  - Bloque `B4`: Conectividad Telegram Bot API, vinculación `NX-...`, `TelegramRateLimiter`, Flyway `V4`.
  - Bloque `B5` y `B6`: Identidad visual NEXUS, `nexus-layout.html`, PWA segura (`manifest.webmanifest`, `sw.js`), `DemoDataSeeder`, bloqueo pesimista en liquidaciones y documentación de entrega.

- **Wilmer (Desarrollador B):**
  - Saneamiento y evolución frontend en `integracion/nexus-b2-colaborativa` (`7a0b18d`):
  - Corrección XSS DOM (`textContent` en `crear-grupo.html`).
  - Validación `CASE_B` en `agregar-gasto.html`.
  - Mejoras de contraste y accesibilidad WCAG en plantillas Thymeleaf y estilos base.
