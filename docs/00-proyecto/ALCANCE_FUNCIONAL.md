# Alcance Funcional Objetivo — Partly

Este documento resume los **8 módulos objetivo** del sistema y distingue explícitamente lo **implementado actualmente** (comprobado en `main` @ `761bc67`) de lo **pendiente por construir**.

---

| Módulo | Alcance objetivo | Estado actual (F0.2.C) |
|---|---|---|
| **1. Núcleo financiero** | Usuarios, grupos, membresías, invitaciones, invitados temporales, gastos, múltiples pagadores, división igualitaria, personalizada, proporcional y por productos, balances, simplificación de deudas, liquidaciones, confirmación bilateral e historial. | **Parcial:** Usuarios, grupos, invitaciones (`PENDIENTE`/`MIEMBRO`/`RECHAZO`), creación de gastos con múltiples pagadores, división igualitaria en pesos enteros, balances e historial básico. **Pendiente:** edición/eliminación, liquidaciones, confirmación bilateral, simplificación, división personalizada/proporcional/productos, invitados temporales. |
| **2. Inteligencia artificial** | Lenguaje natural, notas de voz, extracción financiera, clasificación, copiloto, detección de duplicados, interpretación contextual y arquitectura multiproveedor. | **No iniciado.** |
| **3. Bandeja inteligente** | Recepción de mensajes, imágenes, audios y documentos; borradores, validación, edición, confirmación, rechazo, deduplicación, persistencia y trazabilidad. | **No iniciado.** |
| **4. Automatizaciones** | Gastos recurrentes, vencimientos, recordatorios, reglas configurables, autopiloto del hogar, resúmenes semanales, cierres mensuales y seguimiento de obligaciones. | **No iniciado.** |
| **5. Conectividad** | PWA instalable, bot privado y de grupos en Telegram, vinculación segura, invitaciones por enlace/QR, Web Push, recepción de contenido compartido y comunicación gratuita vía WhatsApp. | **No iniciado** (WhatsApp oficial automatizado condicionado a elegibilidad y costos). |
| **6. Analítica financiera** | Estadísticas, comparación entre períodos, presupuestos, predicciones, detección de anomalías, metas de ahorro compartidas, historial de aportes, PDF y Excel. | **No iniciado.** |
| **7. Ecosistema de pagos** | Registro de transferencias externas, interpretación de comprobantes Nequi/Daviplata, asociación a deudas, solicitudes de pago y confirmaciones. | **No iniciado** (lectura de comprobante nunca equivale a verificación bancaria real). |
| **8. Seguridad y administración** | Autenticación, autorización por grupos, auditoría, privacidad, control de cuotas, `ResourceManager`, monitoreo, respaldos, recuperación, resiliencia y control de costos. | **Parcial:** Auth clásica (BCrypt) + Google Sign-In, sesiones básicas. **Pendiente:** CSRF, Spring Security, política contraseñas, mitigación pre-hijacking, auditoría, `ResourceManager`, respaldos. |
