# Backlog Consolidado Priorizado — Partly (Fase F1)

**Fuente:** Consolidación F0.2.C de hallazgos `HA-001..HA-015` y `HB-001..HB-013`.  
**Nota:** Ningún hallazgo está resuelto todavía; todos permanecen en estado `PENDIENTE`.

---

| Orden | Subfase | Tarea | Hallazgos asociados | Responsable | Depende de |
|---|---|---|---|---|---|
| 1 | **F1.0** | Crear rama `develop`, registrar documentación F0, tratar `repomix-output.xml` y abrir ramas `feature/A-*` y `feature/B-*` | — | Coordinado (A/B) | Aprobación humana |
| 2 | **F1.1.A** | Configurar perfil `test` aislado (H2/Testcontainers) y suite unitaria de `DeudaService` | `HA-008`, `HB-012` | Dev A | F1.0 |
| 3 | **F1.1.B** | Saneamiento frontend independiente: corregir XSS DOM (`innerHTML` -> `textContent`), `</div>` extra en `inicio.html`, corregir contraste `--positive` y accesibilidad base | `HB-002`, `HB-006`, `HB-009` | Dev B | F1.0 |
| 4 | **F1.2.A** | Incorporar `spring-boot-starter-security` (CSRF, `SecurityFilterChain`), política de contraseñas, rate-limiting `/login`, mitigar Account Pre-hijacking Google/Registro, timeouts `RestTemplate`, mapear `GET /`, filtrar `MIEMBRO` en `/grupo/{id}` y exponer `googleVinculado` | `HA-001`, `HA-002`, `HA-005`, `HA-006`, `HA-011`, `HA-015`, `HB-001`, `HB-003`, `HB-007`, `HB-008` | Dev A | F1.1.A |
| 5 | **F1.2.B** | Adaptar formularios Thymeleaf con tokens `_csrf` y meta tags; reemplazar `alert()` de Google Auth por UI | `HB-001`, `HB-007` | Dev B | F1.2.A |
| 6 | **F1.3.A** | Incorporar Flyway/Liquibase, desactivar `ddl-auto=update`, constraints `UNIQUE`/`NOT NULL`, precisión `BigDecimal` y `@Version` | `HA-007`, `HA-012`, `HA-014` | Dev A | F1.1.A |
| 7 | **F1.3.B** | Crear fragmentos Thymeleaf (`head`, `navbar`, `toasts`), extraer JS inline a `static/js/`, migrar estilos inline a `partly.css`, eliminar SVG duplicado | `HB-010` | Dev B | F1.1.B |
| 8 | **F1.4.A** | Parseo monetario estricto (rechazar negativos/formatos ambiguos), `Idempotency-Key` en gastos, `RedirectAttributes` ante errores y `${mensaje}` en agregar usuario | `HA-003`, `HA-004`, `HB-004` | Dev A | F1.2.A, F1.3.A |
| 9 | **F1.4.B** | Edición de fecha en formulario de gasto, previsualización de cuota por participante, prevención de doble envío, conservación de estado del formulario ante error | `HB-004`, `HB-005`, `HB-011` | Dev B | F1.2.B, F1.4.A |
| 10 | **F1.5.A** | Extraer lógica de controladores a servicios de dominio, DTOs con `@Valid` y `@ControllerAdvice` | `HA-013` | Dev A | F1.3.A, F1.4.A |
| 11 | **F1.5.B** | Navegación persistente en subvistas, layout desktop/tablet y accesibilidad completa | `HB-009` | Dev B | F1.3.B |
| 12 | **F1.6** | Optimizar N+1 en `DeudaService`/`MiembroGrupoController`, eliminar doble cálculo y corregir sesgo de residuo por ID | `HA-009`, `HA-010`, `HA-014` | Dev A | F1.5.A |
| 13 | **F1.7** | Pruebas `@WebMvcTest`, integración completa y regresión en entorno aislado | `HA-008`, `HB-012` | Ambos (A + B) | F1.5.B, F1.6 |
| 14 | **F1.8** | Auditoría independiente de cierre de Fase F1 | Todos (`HA-*`, `HB-*` de F1) | Ambos (A + B) | F1.7 |

> **Backlog diferido (`F2`–`F15`):** Liquidaciones bilaterales, edición/eliminación de gastos, simplificación de deudas, API `/api/v1/`, Bandeja Inteligente, `ResourceManager`, IA/OCR/Voz, Automatizaciones y Analítica. Las capacidades de PWA, canales externos (Telegram/WhatsApp) y flujos colaborativos avanzados (`HB-013`) permanecen asignadas a `F10/F11` (y fases posteriores según roadmap).
