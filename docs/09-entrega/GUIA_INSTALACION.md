# Guía de Instalación y Despliegue Local — NEXUS

## 1. Prerrequisitos
- **Java 21** (JDK 21)
- **MySQL Server 8.0+**
- **Git**

## 2. Preparación de Base de Datos de Demostración (`nexus_demo`)
No usar la base habitual `gestor_gastos` ni el usuario `root` en ejecución demo:

```sql
CREATE DATABASE IF NOT EXISTS nexus_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'nexus_demo_user'@'localhost' IDENTIFIED BY '<CONTRASENA_SEGURA>';
GRANT ALL PRIVILEGES ON nexus_demo.* TO 'nexus_demo_user'@'localhost';
FLUSH PRIVILEGES;
```

## 3. Variables de Entorno
Configurar en la terminal o en Run Configuration de IntelliJ (nunca versionar secretos):
- `SPRING_PROFILES_ACTIVE=demo`
- `NEXUS_DEMO_DB_URL=jdbc:mysql://localhost:3306/nexus_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota`
- `NEXUS_DEMO_DB_USERNAME=nexus_demo_user`
- `NEXUS_DEMO_DB_PASSWORD=<tu_contraseña>`
- `NEXUS_DEMO_SEED_ENABLED=true` *(opcional, solo primer arranque demo)*
- `GROQ_ENABLED=false` *(o `true` con `GROQ_API_KEY`)*
- `TELEGRAM_ENABLED=false` *(o `true` con `TELEGRAM_BOT_TOKEN` y `TELEGRAM_BOT_USERNAME`)*
- `TELEGRAM_GROUPS_ENABLED=false`

## 4. Arranque
```powershell
.\mvnw.cmd spring-boot:run
```
Abrir `http://localhost:8080/login`.

## 5. Ejecución de Pruebas Aisladas
```powershell
.\mvnw.cmd -o test
```
