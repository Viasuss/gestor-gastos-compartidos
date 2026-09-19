# Diagrama Entidad-Relación (DER)

## Diagrama

```mermaid
erDiagram

    USUARIO ||--o{ GRUPO : crea
    USUARIO ||--o{ MIEMBROGRUPO : participa
    GRUPO ||--o{ MIEMBROGRUPO : incluye
    USUARIO ||--o{ GASTO : paga
    GRUPO ||--o{ GASTO : contiene
    GASTO ||--o{ GASTOPARTICIPANTE : divide
    USUARIO ||--o{ GASTOPARTICIPANTE : debe

    USUARIO {
        long id PK
        string name
        string email UK
        string password
    }

    GRUPO {
        long id PK
        string name
        LocalDate fecha_creacion
        long creador_id FK
    }

    MIEMBROGRUPO {
        long id PK
        long usuario_id FK
        long grupo_id FK
        EstadoMiembro estado
    }

    GASTO {
        long id PK
        string descripcion
        BigDecimal monto
        LocalDate fecha
        long usuario_id FK
        long grupo_id FK
    }

    GASTOPARTICIPANTE {
        long id PK
        long usuario_id FK
        long gasto_id FK
    }
```

## Estados de la relación MIEMBROGRUPO

El atributo `estado` permite controlar la situación del usuario dentro del grupo:

* `PENDIENTE`: el usuario recibió una invitación, pero todavía no la ha aceptado.
* `MIEMBRO`: el usuario aceptó la invitación y pertenece al grupo.
* `RECHAZO`: el usuario rechazó la invitación.

Estos estados permiten manejar el flujo de invitaciones sin crear una entidad adicional.
