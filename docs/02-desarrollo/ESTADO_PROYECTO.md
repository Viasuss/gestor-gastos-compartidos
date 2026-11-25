# Estado del Proyecto — NEXUS (anteriormente Partly)

**Última actualización:** 2026-11-25 (Bloque `B5` — Identidad Visual NEXUS, Experiencia de Usuario, PWA y Preflight Operativo)  
**Rama base:** `main` @ `761bc67` / Integración colaborativa `integracion/nexus-b2-colaborativa` @ `7a0b18d` / B4 publicado `feature/acelerado-b4-telegram` @ `bcf4483` / Desarrollo actual `feature/acelerado-b5-experiencia-pwa`  
**Presupuesto:** `$0 COP`

---

## 1. Línea base comprobada
- **Código productivo:** Intacto desde `761bc67`.
- **Auditorías completadas:**
  - `F0.2.A` (Backend, núcleo financiero, BD y seguridad): **15 hallazgos** (`HA-001` a `HA-015`).
  - `F0.2.B` (Frontend, UX, accesibilidad, PWA e integraciones): **13 hallazgos** (`HB-001` a `HB-013`).
  - *Nota de origen:* Ambas auditorías (`F0.2.A` y `F0.2.B`) fueron ejecutadas inicialmente desde el computador del Desarrollador A en dos conversaciones independientes. El Desarrollador B humano inicia su intervención desde una copia independiente sincronizada mediante `F1.0`.
  - `F0.2.C` (Consolidación arquitectónica): **Aprobada con observaciones**; `F1.0` establece la línea base colaborativa en GitHub (`develop`, `feature/A-f1-1-entorno-pruebas`, `feature/B-f1-1-saneamiento-frontend`).
  - **Estado de hallazgos:** Ningún hallazgo se considera resuelto únicamente por haber sido documentado; todos permanecen pendientes hasta su verificación con código y pruebas.
- **Suite de pruebas:** **BLOQUEADA** para ejecución hasta implementar `F1.1.A` (entorno aislado sin conexión a MySQL real).

## 2. Veredicto de preparación
- **Estado comercial/producción:** **NO APTO.**
- **Estado para desarrollo paralelo en F1:** **LISTO** sobre las ramas publicadas en `F1.0`, respetando los límites específicos de cada subfase.
