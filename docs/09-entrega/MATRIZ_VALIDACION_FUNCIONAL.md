# Matriz de Validación Funcional — NEXUS (Cierre B6.4)

| Área / Funcionalidad | Estado | Evidencia / Mecanismo de Verificación | Observaciones |
|---|---|---|---|
| Seguridad Spring Security, CSRF, BCrypt, acceso grupos | Verificado | H2 + MockMvc + MySQL `nexus_demo` | Operativo |
| Núcleo financiero (`DeudaService`, suma cero, multi-pagador) | Verificado | H2 + MySQL real `nexus_demo` | Casos A/B/C/D validados en vivo |
| Liquidaciones bilaterales, idempotencia y bloqueo pesimista | Verificado | H2 + MySQL real `nexus_demo` | `READ_COMMITTED` + `findByIdForUpdate` |
| Normalización monetaria `MoneyParser` (`60000.00` exacto) | Verificado | `B63AndB64FixesTest` + `BandejaWebConfirmTest` | Rechaza fracciones no nulas (`.50`) |
| Groq Free API | Parcialmente verificado | Real: Caso A incompleto (`GROQ`, `REQUIERE_ACLARACION`, `1577 ms`); Completo: Mocks | `openai/gpt-oss-20b` + `reasoning_format=hidden` |
| Telegram privado (`@NexusFinanzasBot`) | Verificado en vivo | T01–T09 reales + revinculación segura + polling estable | `OBS-TG-01..03` resueltos |
| Flujo financiero multicanal (M01) | Verificado en vivo | Borrador `#10` Telegram → Web `$60.000` → Saldo Telegram actualizado | Idempotencia multicanal verificada |
| Telegram grupal | Deshabilitado por defecto | `TELEGRAM_GROUPS_ENABLED=false` + pruebas simuladas | No habilitado en demo |
| MySQL 8 (`nexus_demo`), Flyway `V1`–`V4` y `ddl-auto=validate` | Verificado en vivo | Arranques reales en `nexus_demo` (`nexus_demo_user`) | `gestor_gastos` intacta |
| PWA (`manifest.webmanifest`, `sw.js`, `offline.html`) | Verificado (MockMvc + estática) | `B5ExperienceAndPwaTest` (`STATIC_ALLOWLIST` sin datos privados) | Instalación física en navegador pendiente de demo |
