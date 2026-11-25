# B2 — INFORME TÉCNICO DE EVOLUCIÓN FINANCIERA Y LIQUIDACIONES

**Proyecto:** NEXUS (anteriormente Partly)  
**Rama:** `feature/acelerado-b2-liquidaciones` (desde `feature/acelerado-b1-seguridad` @ `a1b72c6ae0ecc4addce531407d702050eb81536c`)  
**Fecha:** 2026-11-24

---

## 1. Resumen ejecutivo
Se implementó el módulo completo de **liquidaciones internas y confirmación bilateral** (`Liquidacion`, `EstadoLiquidacion`, `MetodoPago`, `LiquidacionRepository`, `LiquidacionService`, `LiquidacionController`, `V2__liquidaciones.sql`, `liquidar.html` e integración en `DeudaService` y `grupo.html`). Todas las **30 pruebas** de la suite aislada pasaron (`0` fallos, `0` errores, `0` omisiones) en `48.965 s`.

## 2. Arquitectura y reglas implementadas
- **Entidad independiente:** `Liquidacion` (separada de `PagoGasto`) con `monto DECIMAL(19,2)`, `estado` (`PENDIENTE`, `CONFIRMADA`, `RECHAZADA`, `CANCELADA`), `metodoPago` (`EFECTIVO`, `NEQUI`, `DAVIPLATA`, `TRANSFERENCIA_BANCARIA`, `OTRO`), `idempotencyKey` único por grupo y `@Version`.
- **Máquina de estados (`LiquidacionService`):**
  - `PENDIENTE -> CONFIRMADA` (solo receptor; revalida deuda dentro de transacción).
  - `PENDIENTE -> RECHAZADA` (solo receptor).
  - `PENDIENTE -> CANCELADA` (solo pagador).
  - Reserva de pendientes: impide registrar solicitudes cuya suma exceda la deuda disponible.
- **Integración con `DeudaService`:** Aplica **exclusivamente** liquidaciones `CONFIRMADA` (una sola vez), conservando gastos, contribuciones e invariante de suma cero.
- **UI e historial:** Vista `liquidar.html` y sección de liquidaciones e historial en `grupo.html` con botones CSRF `Confirmar recepción`, `Rechazar` y `Cancelar solicitud`.
- **Funcionalidad diferida (CP5):** División personalizada por importes exactos aplazada para priorizar estabilidad del núcleo de liquidaciones.

## 3. Veredicto técnico
**BLOQUE B2 COMPLETADO Y VERIFICADO (30/30 pruebas en H2 aislado).** Listo para **Bloque B3 (Diferenciación Inteligente)** previa autorización.
