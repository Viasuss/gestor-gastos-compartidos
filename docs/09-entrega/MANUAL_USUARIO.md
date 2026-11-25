# Manual de Usuario — NEXUS

## 1. Registro e Inicio de Sesión
1. Ingresa a `/registro` o `/login`.
2. Regístrate con nombre, correo y contraseña de al menos 8 caracteres (o utiliza Google Sign-In si está habilitado).
3. Al iniciar sesión llegarás al panel `/inicio`, donde verás tu balance general (**Te deben** / **Debes**), indicadores operativos (borradores pendientes, liquidaciones, invitaciones y estado de Telegram) y tus grupos destacados.

## 2. Gestión de Grupos e Invitaciones
1. En **Crear grupo** (`/grupo/crear`), asigna un nombre y añade miembros por correo electrónico registrado.
2. Los invitados aceptan o rechazan desde **Invitaciones** (`/invitaciones`).
3. En **Mis grupos** (`/mis-grupos`) accedes al detalle de cada grupo (`/grupo/{id}`).

## 3. Registro de Gastos Compartidos
1. Dentro de un grupo, pulsa **Agregar gasto**.
2. Ingresa descripción, fecha, quiénes pagaron y el monto aportado por cada pagador, y marca los participantes entre quienes se reparte el gasto.
3. NEXUS calcula automáticamente los saldos netos de cada integrante.

## 4. Asistente Inteligente (Bandeja `/asistente`)
1. Escribe un gasto en lenguaje natural (por ejemplo: *"Pagué 120 mil pesos de internet para los tres integrantes"*).
2. NEXUS crea un **borrador pendiente** sin modificar aún tus saldos.
3. Revisa el borrador y pulsa **Confirmar** (o **Descartar**).

## 5. Liquidaciones Bilaterales
1. Cuando debas dinero a un compañero en un grupo, entra a **Pagar / Liquidar**.
2. Registra el monto (parcial o total) y método de pago (`EFECTIVO`, `NEQUI`, `DAVIPLATA`, `TRANSFERENCIA_BANCARIA`, `OTRO`).
3. El receptor confirma o rechaza el pago desde el detalle del grupo; solo al confirmarse se ajusta la deuda pendiente.

## 6. Conexión con Telegram (`/integraciones`)
1. Pulsa **Generar código de vinculación** para obtener un código temporal `NX-...`.
2. Envía `/vincular NX-...` al bot autorizado de NEXUS en Telegram.
3. Usa `/grupos`, `/saldo`, `/deudas`, `/gasto` y `/pendientes` desde Telegram; los borradores quedan sincronizados con tu bandeja web.

## 7. Instalación como PWA
En Chrome o Edge, pulsa el ícono **Instalar NEXUS** en la barra de direcciones (o *Añadir a pantalla de inicio* en móvil).
