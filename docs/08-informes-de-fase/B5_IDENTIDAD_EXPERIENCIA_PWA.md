# Informe de Fase — Bloque B5: Identidad Visual NEXUS, Experiencia de Usuario, PWA y Preflight Operativo

**Proyecto:** NEXUS (anteriormente Partly)  
**Rama:** `feature/acelerado-b5-experiencia-pwa`  
**Base B4 publicada:** `feature/acelerado-b4-telegram` @ `bcf4483e1ba6fab6101478c49a16cc4299d7353c`  
**Presupuesto ejecutado:** `$0 COP`  
**Fecha:** 2026-11-25

---

## 1. Alcance Ejecutado

1. **Consolidación de seguridad Telegram (CP1):**
   - `TELEGRAM_GROUPS_ENABLED` fijado a `false` por defecto en `application.properties`, `TelegramGroupBindingService.java` e `IntegracionesController.java`.
   - Rate-limiting de vinculación (`/vincular`) en `TelegramRateLimiter.java` (`allowLinkingAttempt` / `recordLinkingFailure`: máximo 5 intentos fallidos en ventana de 15 minutos) conectado en `TelegramUpdateRouter.java`.
2. **Preflight operativo (CP2):**
   - Verificada la ausencia segura de credenciales reales en entorno local; documentado en `docs/07-integraciones/VALIDACION_OPERATIVA_GROQ_TELEGRAM.md`.
3. **Identidad visual NEXUS y diseño unificado (CP3–CP5):**
   - Activos SVG: `nexus-isotipo.svg` y `nexus-logo-horizontal.svg`.
   - Sistema de diseño: `src/main/resources/static/css/nexus.css` con tokens semánticos (`--nexus-midnight`, `--nexus-emerald`, `--nexus-mint`, `--nexus-forest`, `--nexus-cloud`, `--nexus-slate`, `--nexus-danger`), navegación unificada desktop/móvil y rejilla KPI accesible.
   - Fragmentos Thymeleaf: `src/main/resources/templates/fragments/nexus-layout.html` (`head(pageTitle)` y `topnav(activePage)`).
   - Actualización de todas las vistas (`inicio.html`, `login.html`, `registro.html`, `mis-grupos.html`, `crear-grupo.html`, `invitaciones.html`, `grupo.html`, `agregar-gasto.html`, `liquidar.html`, `bandeja.html`, `integraciones.html`) con marca NEXUS, preservando tokens CSRF, mitigación XSS, validación `CASE_B`, `idempotencyKey` y accesibilidad.
   - Métricas reales en `UsuarioController.mostrarInicio(...)`: `borradoresPendientesCount`, `liquidacionesPendientesCount` y `telegramVinculado`.
4. **Capacidades PWA instalables y seguras (CP6):**
   - `manifest.webmanifest`, íconos `icon-192.png` e `icon-512.png`, `sw.js` (allow-list estática sin cachear vistas autenticadas ni endpoints financieros), `pwa-register.js`, `offline.html` y permisos en `SecurityConfig.java`.
5. **Pruebas y documentación (CP7):**
   - `B5ExperienceAndPwaTest.java` valida rate-limiting de vinculación, `groupsEnabled=false`, acceso público PWA, seguridad de `sw.js` y renderizado NEXUS en vistas públicas y autenticadas.
   - Documentación actualizada en `docs/07-integraciones/PWA_INSTALACION_Y_SEGURIDAD.md`, `docs/07-integraciones/VALIDACION_OPERATIVA_GROQ_TELEGRAM.md`, `docs/02-desarrollo/ESTADO_PROYECTO.md` y `docs/02-desarrollo/BACKLOG.md`.
