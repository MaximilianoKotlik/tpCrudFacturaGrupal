# TP CRUD Facturación (grupal)

Sistema de facturación de ventas con emisión de comprobantes electrónicos contra ARCA/AFIP (ambiente de **homologación**).

- **Backend:** Java 21 + Spring Boot 3.3 (JPA/Hibernate, PostgreSQL, Swagger/OpenAPI), integración SOAP con WSAA y WSFEv1.
- **Frontend:** TypeScript + Vite (sin framework).

## Integrantes

- _Completar con nombre y legajo de cada integrante_

## Estructura

```
frontend/                         # cliente web (TypeScript + Vite)
tp_spring_grupal/tp_spring_grupal # API REST (Spring Boot + Gradle)
```

## Requisitos

- JDK 21
- PostgreSQL (base `facturacion_db`; el esquema `facturacion` se crea con `ddl-auto=update`)
- Node.js 20+
- Para autorizar comprobantes en ARCA: certificado de homologación (`.pfx`) asociado a un CUIT

## Cómo ejecutar

### 1. Backend

```bash
cd tp_spring_grupal/tp_spring_grupal
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Editar `application.properties` y completar los valores marcados como `CAMBIAR` (contraseña de PostgreSQL, CUIT y clave del certificado). El certificado va en `src/main/resources/certificado.pfx`. Ninguno de estos archivos se sube al repositorio.

```bash
./gradlew bootRun
```

La API queda en `http://localhost:8080/api` y la documentación Swagger en `http://localhost:8080/swagger-ui.html`. Al iniciar, `DataLoader` carga datos de ejemplo (clientes, artículos, listas de precio, etc.).

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrir la URL que muestra Vite (por defecto `http://localhost:5173`).

## Tests

```bash
cd tp_spring_grupal/tp_spring_grupal
./gradlew test
```

Los tests usan una base H2 en memoria y valores ficticios, así que no requieren PostgreSQL ni conexión con ARCA.
