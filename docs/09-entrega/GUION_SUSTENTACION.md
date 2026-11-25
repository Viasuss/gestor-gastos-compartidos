# Guion de Sustentación Universitaria — NEXUS (8–12 minutos)

| Tiempo | Sección | Responsable | Contenido clave |
|---|---|---|---|
| 0:00 – 0:45 | 1. Problema identificado | Wilmer | Desorden en cuentas compartidas (viajes, apartamento, universidad) y errores manuales en hojas de cálculo. |
| 0:45 – 1:30 | 2. Solución NEXUS & 3. Diferenciación | Johan | Asistente inteligente de finanzas compartidas con exactitud `BigDecimal`, confirmación bilateral de pagos y bandeja IA subordinada. |
| 1:30 – 2:30 | 4. Arquitectura | Johan | Spring Boot 4 + Java 21 + Spring Security + Flyway `V1`–`V4` + MySQL 8 + `ResourceManager` + PWA. |
| 2:30 – 3:30 | 5. Registro e inicio de sesión & 6. Creación de grupo | Wilmer | Login/Registro seguro (CSRF, BCrypt, rate-limit), creación de grupo, invitaciones y accesibilidad UI. |
| 3:30 – 4:45 | 7. Registro de gasto manual | Wilmer | Multi-pagador, participantes, `CASE_B`, `idempotencyKey` y suma cero. |
| 4:45 – 6:15 | 8. Interpretación natural & 9. Bandeja inteligente | Johan | Propuesta vía Groq Free / `LocalRulesProvider`, creación de borrador y confirmación humana obligatoria. |
| 6:15 – 7:30 | 10. Telegram | Johan | Vinculación `NX-...` con SHA-256, `/saldo`, `/deudas`, `/gasto` y sincronización web *(o Plan B offline con `LocalRulesProvider`)*. |
| 7:30 – 8:30 | 11. Liquidación bilateral | Wilmer | Registro de pago parcial/total, reserva de deuda, confirmación por receptor y bloqueo pesimista. |
| 8:30 – 9:15 | 12. PWA | Wilmer | Manifiesto, instalación en Chrome/Edge y `sw.js` con *allow-list* estática sin cachear datos financieros. |
| 9:15 – 10:15 | 13. Seguridad y `ResourceManager` & 14. Pruebas | Johan | Cuotas RPM/RPD/TPM, *circuit breaker*, aislamiento `test` (H2) y suite de 48 pruebas aprobadas. |
| 10:15 – 11:00 | 15. Trabajo futuro | Ambos | División personalizada por porcentajes, OCR/voz dentro de presupuesto `$0 COP` y notificaciones push. |
