# Backlog - Gestor de Gastos Compartidos

## Épica 1: Autenticación

| ID | Historia de usuario | Estado | Asignado |
|----|---|---|---|
| HU01 | Como usuario, quiero registrarme con nombre, correo y contraseña, para poder acceder a la aplicación. | Listo | Wilmer |
| HU02 | Como usuario, quiero iniciar sesión con mi correo y contraseña, para acceder a mis grupos y gastos. | Listo | Wilmer |
| HU03 | Como usuario, quiero cerrar sesión, para proteger mi cuenta en un computador compartido. | Listo | Wilmer |

## Épica 2: Grupos

| ID | Historia de usuario | Estado | Asignado |
|----|---|---|---|
| HU04 | Como usuario, quiero crear un grupo con un nombre, para organizar los gastos de un contexto específico (viaje, apto, etc.) | Listo | Wilmer |
| HU05 | Como usuario, quiero agregar a otros usuarios registrados a mi grupo, para que puedan participar en los gastos compartidos. | Listo | Johan |
| HU05.1 | Como usuario invitado, quiero ver mis invitaciones pendientes y poder aceptarlas o rechazarlas, para decidir si quiero unirme a un grupo. | Listo | Johan |
| HU06 | Como usuario, quiero ver la lista de grupos a los que pertenezco, para acceder rápido a cada uno. | Listo | Wilmer |

## Épica 3: Gastos

| ID | Historia de usuario | Estado | Asignado |
|----|---|---|---|
| HU07 | Como usuario, quiero agregar un gasto dentro de un grupo (descripción, monto, quién pagó), para registrar lo que se gastó. | Listo | Johan |
| HU07.1 | Como usuario, quiero poder registrar varios pagadores en un mismo gasto, cada uno con su propio monto, para reflejar cuando varias personas aportan a una misma compra. | Listo | Johan |
| HU08 | Como usuario, quiero elegir entre quiénes se divide un gasto (todos o algunos del grupo), para que el cálculo sea justo. | Listo | Wilmer |
| HU09 | Como usuario, quiero ver el historial de gastos de un grupo ordenado por fecha, para revisar en qué se ha gastado. | Listo | Wilmer |

## Épica 4: Cálculo de deudas

| ID | Historia de usuario | Estado | Asignado |
|----|---|---|---|
| HU10 | Como usuario, quiero ver cuánto le debo a cada persona del grupo (o cuánto me deben), desglosado por persona, para saber exactamente a quién pagarle o cobrarle. | Listo | Johan |

---

## 🎉 MVP completo

Las 4 épicas y todas las historias de usuario planeadas están implementadas y probadas. El flujo completo funciona de punta a punta: registro → login → crear grupo → invitar miembros → aceptar/rechazar invitación → agregar gasto (con uno o varios pagadores) → ver historial → ver desglose de deudas por persona.

---

## Fuera de alcance (MVP)

- Notificaciones automáticas por correo/push (las invitaciones sí se ven dentro de la web, pero no se notifican por fuera)
- Simplificación inteligente de deudas (algoritmo que minimiza transacciones entre más de 2 personas)
- App nativa (solo web por ahora)
- Subida de fotos de recibos/comprobantes
- División de un gasto en montos/porcentajes desiguales entre quienes participan (sigue dividiéndose en partes iguales; lo que sí se amplió fue poder registrar **varios pagadores**, que es un concepto distinto — ver HU07.1)

---

## Historial de Sprints

### Sprint 1

**Sprint Goal:** Tener el registro y login de usuarios funcionando de punta a punta, más el modelo de datos completo creado en la base de datos.

- [x] Definir las 5 entidades del modelo de datos (Usuario, Grupo, MiembroGrupo, Gasto, GastoParticipante) — **Johan**
- [x] Corregir observaciones de revisión de código en las entidades — **Johan**
- [x] Verificar que las tablas se crean correctamente en MySQL — **Johan**
- [x] HU01: Formulario de registro (vista + controlador) — **Wilmer**
- [x] HU02: Formulario de login (vista + controlador) — **Wilmer**

### Sprint 2

- [x] Crear los Repository (JPA) de las 5 entidades — **Johan**
- [x] HU04: Backend de "crear grupo" — **Johan**
- [x] Revisar/corregir el hash de contraseña (BCrypt) — **Wilmer**
- [x] HU03: Botón/lógica de cerrar sesión — **Wilmer**
- [x] Vista de "inicio" (home) después del login — **Wilmer**

### Sprint 3

- [x] HU05: Controlador para agregar miembro a un grupo (por correo) — **Johan**
- [x] Vista de formulario para agregar miembro — **Johan**
- [x] HU06: Controlador que lista los grupos del usuario logueado — **Wilmer**

### Sprint 4: Sistema de invitaciones a grupos

**Sprint Goal:** Que agregar a alguien a un grupo funcione como una invitación real (pendiente → aceptar/rechazar), no como una adición directa sin consentimiento.

- [x] Entidad `EstadoMiembro` (enum: PENDIENTE, MIEMBRO, RECHAZO) — **Johan**
- [x] `InvitacionController`: ver invitaciones, aceptar, rechazar — **Johan**
- [x] Validación de seguridad: solo el usuario invitado puede aceptar/rechazar su propia invitación — **Johan**
- [x] Vista `invitaciones.html` — **Johan**
- [x] Diseño en Figma de las pantallas principales de la app — **Johan**

### Sprint 5: Registro de gastos

- [x] `GastoController`: formulario y guardado de gastos — **Johan**
- [x] Vista `agregar-gasto.html` — **Wilmer**
- [x] HU09: historial de gastos integrado en `grupo.html` — **Wilmer**

### Sprint 6: Cálculo de deudas + múltiples pagadores


- [x] JavaScript: marcar automáticamente al pagador en "dividir entre" (editable) — **Johan**
- [x] `DeudaService`: cálculo de deudas desglosado por persona — **Johan**
- [x] Ampliación: soporte para varios pagadores por gasto, cada uno con su propio monto (`PagoGasto`) — **Johan**
- [x] Validación: la suma de los pagos debe coincidir con el monto total del gasto — **Johan**
- [x] Formato de moneda estilo "Nequi" ($ con puntos de miles) en el formulario — **Wilmer/Johan**
- [x] Corrección de bug: comparación de `Long` con `==` en vez de `.equals()` en `DeudaService` — **Johan**
- [x] Sección "Detalle de mis deudas" integrada en `grupo.html` — **Johan/Wilmer**
- [x] `BalanceGeneral`: suma de deudas a través de todos los grupos, para la pantalla de inicio — **Johan**

---
*Última actualización: Sprint 6 — MVP completo*