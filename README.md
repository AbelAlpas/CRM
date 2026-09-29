# CRM

Base técnica de un CRM modular construido con Java 21, Spring Boot, Spring MVC, Thymeleaf, Bootstrap 5, Spring Data JPA, Spring Security, PostgreSQL y Flyway.

## Requisitos

- Java 21 o superior
- PostgreSQL 15 o superior
- Maven 3.9 o usar `mvnw.cmd`

## Configuración local

Define estas variables de entorno antes de iniciar la aplicación:

```text
DB_HOST=localhost
DB_PORT=5432
DB_NAME=CRM
DB_USERNAME=postgres
DB_PASSWORD=tu_password_local
CRM_BOOTSTRAP_ADMIN_ENABLED=true
CRM_ADMIN_EMAIL=admin@example.com
CRM_ADMIN_PASSWORD=una_clave_segura
```

El bootstrap de administrador está deshabilitado por defecto. Actívalo sólo en desarrollo y usa una contraseña local segura. Las contraseñas se almacenan usando BCrypt.

## Ejecución

Desde el IDE, ejecuta `com.example.CRM.CrmApplication` con las variables anteriores. También puedes usar:

```powershell
mvn clean verify
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080/login`.

Flyway aplica automáticamente las migraciones de `src/main/resources/db/migration` y Hibernate valida el esquema existente.

## Estructura

Los módulos se organizan por funcionalidad: `dashboard`, `lead`, `client`, `opportunity`, `pipeline`, `activity`, `user`, `security` y `config`.

El CRUD de usuarios está disponible en `/users` y sólo puede ser utilizado por usuarios con rol `ADMIN`.

## Contribución

1. Crea una rama descriptiva desde `main`.
2. Ejecuta `mvn clean verify` antes de abrir un pull request.
3. No subas archivos `.env`, contraseñas ni datos reales de PostgreSQL.
