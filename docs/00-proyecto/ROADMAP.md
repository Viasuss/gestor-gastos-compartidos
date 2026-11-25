# Hoja de Ruta Oficial (Roadmap) — Partly

**Fases generales:** `F0` a `F15` según el Contexto Maestro V1.0.  
**Estado actual:** Fase `F0` (Auditoría y Consolidación) completada en `F0.2.C`. Pendiente autorización humana para iniciar `F1`.

---

## 1. Subdivisión oficial de la Fase F1 (Endurecimiento Base y Saneamiento)

> [!IMPORTANT]
> Durante `F1` **no se adelantarán funcionalidades** de IA, Telegram, OCR, PWA ni automatizaciones. El objetivo exclusivo es convertir el MVP actual en una base segura, probada y mantenible.

| Subfase | Título | Responsable principal | Dependencias |
|---|---|---|---|
| **F1.0** | Organización del repositorio y configuración inicial de ramas (`develop`, `feature/A-*`, `feature/B-*`) | Coordinado (A/B) | Aprobación humana de comandos Git |
| **F1.1.A** | Entorno aislado de pruebas (perfil `test` con H2/Testcontainers + pruebas base de `DeudaService`) | Desarrollador A | F1.0 |
| **F1.1.B** | Saneamiento frontend independiente (corregir XSS DOM `HB-002`, `</div>` sobrante `HB-006`, contraste y accesibilidad `HB-009`) | Desarrollador B | F1.0 |
| **F1.2.A** | Autenticación y seguridad backend (`spring-boot-starter-security`, CSRF, política de contraseñas, rate-limit, mitigación pre-hijacking Google OAuth, mapeo `GET /`) | Desarrollador A | F1.1.A |
| **F1.2.B** | Adaptación de formularios a CSRF, meta tags y mejoras de feedback auth (`alert()` -> UI) | Desarrollador B | F1.2.A |
| **F1.3.A** | Migraciones e integridad de base de datos (Flyway/Liquibase, desactivar `ddl-auto=update`, constraints `UNIQUE`/`NOT NULL`, precisión `BigDecimal`, `@Version`) | Desarrollador A | F1.1.A |
| **F1.3.B** | Componentes frontend reutilizables (fragmentos Thymeleaf, extracción JS a `static/js/`, limpieza CSS inline) | Desarrollador B | F1.1.B |
| **F1.4.A** | Parseo monetario estricto e idempotencia en creación de gastos | Desarrollador A | F1.2.A, F1.3.A |
| **F1.4.B** | Experiencia y validación de registro de gastos (edición de fecha `HB-005`, previsualización de cuota, prevención de doble envío, conservación de estado ante error) | Desarrollador B | F1.2.B, F1.4.A |
| **F1.5.A** | Refactorización a servicios de dominio y DTOs con `@Valid` + `@ControllerAdvice` | Desarrollador A | F1.3.A, F1.4.A |
| **F1.5.B** | Navegación, accesibilidad completa y adaptación responsive | Desarrollador B | F1.3.B |
| **F1.6** | Rendimiento (eliminación N+1 en `DeudaService` y controladores) y consistencia financiera | Desarrollador A | F1.5.A |
| **F1.7** | Integración continua, pruebas end-to-end y regresión completa | Ambos (A + B) | F1.5.B, F1.6 |
| **F1.8** | Auditoría independiente de cierre de Fase F1 | Ambos (A + B) | F1.7 |

---

## 2. Macro-fases posteriores (`F2` a `F15` — Visión resumida)
- **F2:** Completitud del núcleo financiero (edición/eliminación segura, liquidaciones bilaterales, simplificación de deudas, API `/api/v1/` base, auditoría financiera).
- **F3:** `ResourceManager` y Bandeja Inteligente (persistencia de borradores, cuotas, circuit breakers, fallback manual).
- **F4:** PWA instalable, Web Push (VAPID) y capacidades offline seguras para activos estáticos/borradores.
- **F5–F7:** Motor de IA ($0 COP: Groq Free / Ollama), transcripción (Whisper) y OCR (Tesseract.js / Tess4J) conectados a Bandeja Inteligente.
- **F8–F10:** Conectividad Telegram Bot API, compartición gratuita WhatsApp, gastos recurrentes y automatizaciones (`Spring Scheduler`).
- **F11–F13:** Presupuestos, metas de ahorro compartidas, analítica, detección de anomalías y exportación PDF/Excel.
- **F14–F15:** Endurecimiento final de producción, despliegue Oracle Cloud Always Free, identidad comercial definitiva y cierre.
