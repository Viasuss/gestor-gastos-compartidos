# Guion de Sustentación Universitaria — NEXUS `v1.0.0` (5–8 minutos)

| Tiempo | Paso | Responsable | Contenido y demostración en vivo (`nexus_demo`) |
|---|---|---|---|
| 0:00 – 0:40 | 1. Problema y 2. Propuesta de valor | Wilmer | Desorden en cuentas compartidas; NEXUS aporta exactitud `BigDecimal`, confirmación bilateral y asistente IA/Telegram. |
| 0:40 – 1:15 | 3. Inicio de sesión y 4. Dashboard / grupos | Wilmer | Mostrar `/login` (CSRF, BCrypt), `/inicio` con KPIs reales y `/mis-grupos` (`Apartamento NEXUS Demo`, `Viaje de Amigos`, `Universidad`). |
| 1:15 – 2:10 | 5. Registro y distribución de gastos | Wilmer | Explicar soporte multi-pagador, participantes, `CASE_B`, `idempotencyKey` y suma cero. |
| 2:10 – 3:00 | 6. Balances y liquidaciones | Wilmer | Mostrar historial, solicitudes bilaterales `PENDIENTE`/`CONFIRMADA` y bloqueo pesimista `PESSIMISTIC_WRITE` + `READ_COMMITTED`. |
| 3:00 – 3:50 | 7. Bandeja inteligente | Johan | Abrir `/asistente`, mostrar etiquetas legibles (`Requiere aclaración`, `Interpretación local`, `Telegram`/`Web`) y confirmación humana obligatoria. |
| 3:50 – 5:15 | 8. Telegram, 9. Saldo `$130.000` y 10. `/deudas` | Johan | Mostrar `@NexusFinanzasBot`: `/grupos`, `/saldo` (`$130.000 COP` a favor, `$0` en contra) y `/deudas` diferenciado (`Demo B: $40.000`, `Demo C: $90.000`). *(Plan B offline: usar `LocalRulesProvider` en `/asistente`.)* |
| 5:15 – 6:15 | 11. Arquitectura y seguridad | Johan | Spring Boot 4 + Java 21 + Spring Security + Flyway `V1`–`V4` + `ResourceManager` + PWA (`STATIC_ALLOWLIST`). |
| 6:15 – 7:00 | 12. Pruebas y 13. Limitaciones / futuro | Ambos | **63 pruebas aisladas aprobadas (0 fallos)**, limitaciones transparentes (`TELEGRAM_GROUPS_ENABLED=false`, Groq completo por mocks) y trabajo futuro. |
