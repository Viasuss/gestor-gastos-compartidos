# Backlog - Gestor de Gastos Compartidos

## Épica 1: Autenticación

| ID | Historia de usuario | Estado | Asignado |
|----|---|--------|----------|
| HU01 | Como usuario, quiero registrarme con nombre, correo y contraseña, para poder acceder a la aplicación. | listo  | Wilmer   |
| HU02 | Como usuario, quiero iniciar sesión con mi correo y contraseña, para acceder a mis grupos y gastos. | listo  | Wilmer   |
| HU03 | Como usuario, quiero cerrar sesión, para proteger mi cuenta en un computador compartido. | listo    | Wilmer   |

## Épica 2: Grupos

| ID | Historia de usuario                                                                                                        | Estado | Asignado |
|----|----------------------------------------------------------------------------------------------------------------------------|--------|---------|
| HU04 | Como usuario, quiero crear un grupo con un nombre, para organizar los gastos de un contexto específico (viaje, apto, etc.) | listo  | wilmer  |
| HU05 | Como usuario, quiero agregar a otros usuarios registrados a mi grupo, para que puedan participar en los gastos compartidos | listo  | Johan   |
| HU05.1 | Como usuario invitado, quiero ver mis invitaciones pendientes y poder aceptarlas o rechazarlas, para decidir si quiero unirme a un grupo. | Listo | Johan |
| HU06 | Como usuario, quiero ver la lista de grupos a los que pertenezco, para acceder rápido a cada uno.                          | listo  | wilmer  |

## Épica 3: Gastos

| ID | Historia de usuario | Estado | Asignado |
|----|---|---|---|
| HU07 | Como usuario, quiero agregar un gasto dentro de un grupo (descripción, monto, quién pagó), para registrar lo que se gastó. | Pendiente | Por asignar |
| HU08 | Como usuario, quiero elegir entre quiénes se divide un gasto (todos o algunos del grupo), para que el cálculo sea justo. | Pendiente | Por asignar |
| HU09 | Como usuario, quiero ver el historial de gastos de un grupo ordenado por fecha, para revisar en qué se ha gastado. | Pendiente | Por asignar |

## Épica 4: Cálculo de deudas

| ID | Historia de usuario | Estado | Asignado |
|----|---|---|---|
| HU10 | Como usuario, quiero ver cuánto le debo a cada persona del grupo (o cuánto me deben), para saber mi situación financiera dentro del grupo. | Pendiente | Por asignar |
 
---

## Fuera de alcance (MVP)

- Notificaciones automáticas por correo/push (las invitaciones sí se ven dentro de la web, pero no se notifican por fuera)
- Simplificación inteligente de deudas (algoritmo que minimiza transacciones)
- App nativa (solo web por ahora)
- Subida de fotos de recibos/comprobantes
- División de gastos por porcentajes/montos personalizados (fase 2)
---

## Historial de Sprints

### Sprint 1 

**Sprint Goal:** Tener el registro y login de usuarios funcionando de punta a punta, más el modelo de datos completo creado en la base de datos.

**Tareas:**
- [x] Definir las 5 entidades del modelo de datos (Usuario, Grupo, MiembroGrupo, Gasto, GastoParticipante) — **Johan**
- [x] Corregir observaciones de revisión de código en las entidades (nombres de clases/paquete, tipos de dato de monto y fecha) — **Johan**
- [x] Verificar que las tablas se crean correctamente en MySQL — **Johan**
- [x] HU01: Formulario de registro (vista + controlador) — **Wilmer**
- [x] HU02: Formulario de login (vista + controlador) — **Wilmer**
### Sprint 2 

- [x] Crear los Repository (JPA) de las 5 entidades-Johan
- [x] HU04: Backend de "crear grupo"-Johan
- [x] Revisar/corregir el hash de contraseña-wilmer
- [x] HU03: Botón/lógica de cerrar sesión-wilmer
- [x] Vista de "inicio" (home) después del login-wilmer
### sprint 3
- [x] HU05: Controlador para agregar miembro a un grupo — recibe el correo, busca el Usuario con UsuarioRepository, si existe lo guarda en MiembroGrupoRepository asociado al grupo; si no existe, mostrar un mensaje de error simple ("usuario no encontrado") - **johan**
- [x] Vista simple con un formulario (un campo de texto para el correo + botón) - **johan**
- [x] HU06: Controlador que liste los grupos donde el usuario logueado es miembro (usando la sesión, igual que ya hizo con el login) — muestra una vista simple con el nombre de cada grupo - **wilmer**
### Sprint 4
- [x] Entidad `EstadoMiembro` (enum: PENDIENTE, MIEMBRO, RECHAZO) — **Johan**
- [x] `InvitacionController`: ver invitaciones, aceptar, rechazar — **Johan**
- [x] Validación de seguridad: solo el usuario invitado puede aceptar/rechazar su propia invitación — **Johan**
- [x] Vista `invitaciones.html` — **Johan**
- [x] Diseño en Figma de las pantallas principales de la app (login, registro, inicio, mis grupos, detalle de grupo) — **Johan**










