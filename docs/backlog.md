# Backlog - Gestor de Gastos Compartidos

## Épica 1: Autenticación

| ID | Historia de usuario | Estado | Asignado    |
|----|---|---|-------------|
| HU01 | Como usuario, quiero registrarme con nombre, correo y contraseña, para poder acceder a la aplicación. | En progreso | Wilmer      |
| HU02 | Como usuario, quiero iniciar sesión con mi correo y contraseña, para acceder a mis grupos y gastos. | Pendiente | Wilmer      |
| HU03 | Como usuario, quiero cerrar sesión, para proteger mi cuenta en un computador compartido. | Pendiente | Por asignar |

## Épica 2: Grupos

| ID | Historia de usuario | Estado | Asignado |
|----|---|---|---|
| HU04 | Como usuario, quiero crear un grupo con un nombre, para organizar los gastos de un contexto específico (viaje, apto, etc.) | Pendiente | Por asignar |
| HU05 | Como usuario, quiero agregar a otros usuarios registrados a mi grupo, para que puedan participar en los gastos compartidos. | Pendiente | Por asignar |
| HU06 | Como usuario, quiero ver la lista de grupos a los que pertenezco, para acceder rápido a cada uno. | Pendiente | Por asignar |

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

- Notificaciones automáticas (correo/push)
- Simplificación inteligente de deudas (algoritmo que minimiza transacciones)
- App nativa (solo web por ahora)
- Subida de fotos de recibos/comprobantes
- División de gastos por porcentajes/montos personalizados (fase 2)
---

## Historial de Sprints

### Sprint 1 (actual)

**Sprint Goal:** Tener el registro y login de usuarios funcionando de punta a punta, más el modelo de datos completo creado en la base de datos.

**Tareas:**
- [x] Definir las 5 entidades del modelo de datos (Usuario, Grupo, MiembroGrupo, Gasto, GastoParticipante) — **Johan**
- [ ] Corregir observaciones de revisión de código en las entidades (nombres de clases/paquete, tipos de dato de monto y fecha) — **Johan**
- [ ] Verificar que las tablas se crean correctamente en MySQL — **Johan**
- [ ] HU01: Formulario de registro (vista + controlador) — **Wilmer**
- [ ] HU02: Formulario de login (vista + controlador) — **Wilmer**
### Sprint 2 (próximo, no iniciado)

- Repositorios (JPA Repository) para las 5 entidades
- CRUD de grupos (HU04, HU05, HU06)
- Conectar registro/login con la base de datos real (guardar y validar contra la tabla Usuario)
---

*Última actualización: Sprint 1*
 













