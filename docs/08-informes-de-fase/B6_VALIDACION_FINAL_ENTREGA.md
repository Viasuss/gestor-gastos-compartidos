# Informe de Fase — Bloque B6: Validación Operativa, Estabilización Final y Entrega Universitaria

**Proyecto:** NEXUS (anteriormente Partly)  
**Rama:** `feature/acelerado-b6-validacion-entrega`  
**Rama candidata:** `release/nexus-v1.0-rc1`  
**Presupuesto ejecutado:** `$0 COP`  
**Fecha:** 2026-11-25

---

## 1. Resumen Ejecutivo y Veredicto

- **Veredicto:** **APROBACIÓN PARCIAL (LISTO EN ENTORNO AISLADO / PENDIENTE VALIDACIÓN EN VIVO CON CREDENCIALES DEL PROPIETARIO).**
- **Qué quedó validado y corregido:**
  - Publicación de `feature/acelerado-b5-experiencia-pwa` (`29ea0f7ed2a6aa904be40fd5bab720296c2e191c`) en `origin`.
  - Bloqueo pesimista `PESSIMISTIC_WRITE` en `GrupoRepository.findByIdForUpdate` aplicado a `LiquidacionService.registrarSolicitud` y `confirmar`, eliminando la condición de carrera entre liquidaciones distintas del mismo grupo.
  - Sincronización atómica (`quotaLock`) en `ResourceManager.tryAcquire` entre verificación y reserva de cuotas RPM/RPD/TPM.
  - Perfil `demo` aislado (`application-demo.properties`) con Flyway `V1`–`V4`, `ddl-auto=validate`, esquema `nexus_demo` y `DemoDataSeeder` idempotente protegido contra ejecución sobre `gestor_gastos`.
  - Suite automatizada completa (H2 + MockMvc + mocks): **48 pruebas aprobadas, 0 fallos, 0 errores, 0 omisiones**.
  - Documentación de entrega completa en `README.md` y `docs/09-entrega/*` (incluyendo diagramas Mermaid y guion de sustentación de 8–12 min).
- **Qué requiere intervención del propietario (por ausencia de credenciales en terminal y necesidad de interacción humana):**
  1. Crear `nexus_demo` / `nexus_demo_user` en MySQL 8 local y ejecutar arranque `demo` para validar `V1`–`V4` + `ddl-auto=validate` en MySQL real.
  2. Ejecutar los 3 casos sintéticos reales de Groq y la secuencia real con el bot de Telegram desde su sesión configurada.
  3. Inspección visual en navegador real (`360px`–`1440px`), Google Sign-In real e instalación PWA en Chrome/Edge.
