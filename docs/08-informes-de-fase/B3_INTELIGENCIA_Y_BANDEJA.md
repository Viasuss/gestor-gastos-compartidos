# B3 — INFORME TÉCNICO DE INTEGRACIÓN COLABORATIVA, RESOURCEMANAGER, GROQ FREE Y BANDEJA INTELIGENTE

**Proyecto:** NEXUS (anteriormente Partly)  
**Rama:** `feature/acelerado-b3-inteligencia` (desde `integracion/nexus-b2-colaborativa` @ `7a0b18d120082c93c283d0074d31d22b65524b54`)  
**Fecha:** 2026-11-25

---

## 1. Integración colaborativa (CP0–CP2)
- **Respaldo B2:** Commit local `2119c76773d218aae57627909807afd8d1bfea36` (`feat: implement bilateral settlements and financial history`).
- **Recuperación de Wilmer (B):** `origin/feature/B-f1-1-saneamiento-frontend` en SHA `5f7f1cb03e9752578153f84f11aa5aaa85ad4093` (7 archivos esperados).
- **Conflicto resuelto:** Único conflicto en `crear-grupo.html` (bloque `mostrarToastDinamico`), resuelto semánticamente conservando la versión accesible y segura contra XSS de Wilmer. `agregar-gasto.html` auto-merged conservando `CASE_B`, `idempotencyKey` y colores de contraste accesible.
- **Publicación de integración:** `integracion/nexus-b2-colaborativa` (`7a0b18d120082c93c283d0074d31d22b65524b54`) verificada con `30/30` pruebas y publicada en `origin`.

## 2. Motor inteligente, ResourceManager y Bandeja (CP3–CP6)
- **`ResourceManager` + `ConsumoRecurso`:** Control de habilitación, RPM global/usuario en memoria, RPD y presupuesto de tokens persistidos en BD, reserva configurable (`10%`), concurrencia (`Semaphore`), y circuit breaker inmediato en `429`/`401`/`403` o umbral de fallos `5xx`/timeout.
- **`LocalRulesProvider` + `GroqProvider` + `InteligenciaFinancieraService`:** Orden determinista `LocalRules -> Groq (si habilitado) -> Aclaración / Manual`. `GroqProvider` usa `RestTemplate` con timeouts (`3s`/`8s`) y deserialización estricta (`FAIL_ON_UNKNOWN_PROPERTIES`).
- **`GastoService` + `BorradorOperacion`:** Lógica de creación de gastos extraída de `GastoController` hacia `GastoService` para reutilización atómica entre formulario manual y `BorradorOperacionService.confirmarBorrador`. Vista `bandeja.html` (`/asistente`, `/bandeja`) con sección **Asistente NEXUS**.
- **Migración:** `V3__borradores_y_recursos.sql` preparada para MySQL 8 (pendiente de ejecución en MySQL desechable).

## 3. Pruebas y estado de verificación
- **Suite ejecutada (`.\mvnw.cmd -o test`):** **36 pruebas** (`36` aprobadas, `0` fallos, `0` errores, `0` omisiones) en `54.111 s`.
- **Distinción de evidencia:**
  - *Implementado y probado en H2/Spring:* Seguridad, CSRF, Liquidaciones, `ResourceManager`, `LocalRulesProvider`, `BorradorOperacion`, `GastoService`, validación estricta JSON de `GroqProvider`.
  - *Simulado/Deshabilitado en pruebas:* `GoogleAuthService` (mock), `GroqProvider` (deshabilitado en `test`; sin llamadas HTTP reales a Groq).
  - *Pendiente:* Validación de Flyway `V1`–`V3` en MySQL 8 real, Telegram, OCR, Voz, PWA y rebranding visual completo (B5).
