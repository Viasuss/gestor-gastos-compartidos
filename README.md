# Gestión de gastos compartidos

## 1. Problema que resuelve

Cuando varias personas comparten gastos (arriendo, mercado, un viaje), se pierde la cuenta de quién pagó qué y quién le debe a quién, y terminan haciendo cálculos a mano en WhatsApp o Excel, lo cual genera errores y hasta peleas.

## 2. Usuarios objetivo

Grupos pequeños (2-6 personas) que comparten gastos de forma recurrente: roomies que pagan arriendo/servicios/mercado juntos, o grupos de amigos que viajan juntos y dividen hospedaje/comida/transporte.

## 3. Alcance del MVP (completado)

- Registro/login de usuario (con contraseñas cifradas mediante BCrypt)
- Crear un grupo
- Invitar a otros usuarios registrados a un grupo, mediante correo electrónico
- Sistema de invitaciones: el usuario invitado puede aceptar o rechazar antes de pertenecer al grupo
- Ver la lista de los grupos a los que se pertenece
- Agregar un gasto dentro de un grupo (descripción, monto, fecha)
- **Registrar uno o varios pagadores por gasto**, cada uno con su propio monto aportado (ampliación sobre el alcance original — ver sección 5)
- Elegir entre quiénes se divide un gasto (todos los miembros del grupo, o solo algunos)
- Ver el historial de gastos de un grupo, ordenado por fecha
- Cálculo automático de deudas, **desglosado por persona** (a quién le debo, o quién me debe, dentro de cada grupo)
- Balance general en la pantalla de inicio, sumando las deudas a través de todos los grupos

## 4. Fuera de alcance (por ahora)

- Notificaciones automáticas por fuera de la web (correo/push) — las invitaciones y demás actualizaciones solo se ven dentro de la aplicación
- Simplificación inteligente de deudas (un algoritmo que minimice el número de transacciones entre más de dos personas, por ejemplo convirtiendo "A le debe a B y B le debe a C" en "A le paga directo a C")
- Aplicación nativa (móvil o escritorio) — el proyecto es una aplicación web
- Subida de fotos de recibos o comprobantes de pago
- División de un gasto en montos o porcentajes desiguales entre las personas que participan — la división entre participantes sigue siendo siempre en partes iguales

## 5. Ampliación sobre el alcance original: múltiples pagadores por gasto

Durante el desarrollo se identificó un caso de uso real no contemplado en el MVP original: situaciones donde **más de una persona aporta dinero para un mismo gasto** (por ejemplo, dos personas que juntan plata para pagar el mercado). Esto es conceptualmente distinto a la división entre participantes (que sigue siendo en partes iguales y fuera de alcance personalizarla): aquí lo que cambia es *quién puso el dinero*, no *entre quiénes se reparte la deuda*.

Se implementó entonces la posibilidad de registrar varios pagadores por gasto, cada uno con el monto específico que aportó, validando que la suma de esos aportes coincida exactamente con el monto total del gasto antes de guardarlo.

## 6. Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot (Maven) |
| Seguridad | Spring Security Crypto (BCrypt) para cifrado de contraseñas |
| Base de datos | MySQL |
| Frontend | Thymeleaf (server-side rendering) |
| Diseño | Figma (mockups de las pantallas principales) |
| Control de versiones | Git + GitHub |

## 7. Modelo de datos

Ver diagrama entidad-relación completo en [`docs/DER.md`](docs/DER.md).

Entidades principales: `Usuario`, `Grupo`, `MiembroGrupo` (con estado: pendiente, miembro, rechazado), `Gasto`, `GastoParticipante`, `PagoGasto`.

## 8. Backlog y metodología

El proyecto se desarrolló con una metodología híbrida (Scrum + documentación previa), en sprints semanales. Ver el detalle completo de historias de usuario y el historial de sprints en [`docs/backlog.md`](docs/backlog.md).

## 9. Equipo

- Johan Viasus
- Wilmer Mantilla
