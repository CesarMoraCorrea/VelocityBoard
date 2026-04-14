<div align="center">

# ⚡ VelocityBoard

### Tablero Kanban Reactivo en Tiempo Real

*Construido con Spring WebFlux · MongoDB Reactive · JWT Auth*

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/atlas)
[![JWT](https://img.shields.io/badge/JWT-Seguridad-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Railway](https://img.shields.io/badge/Railway-Desplegado-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)](https://railway.app/)

</div>

---

## 📋 Descripción del Proyecto

**VelocityBoard** es una aplicación de gestión de tareas estilo Kanban (inspirada en Trello) construida sobre una arquitectura **100% reactiva y no bloqueante** con Java. Permite a los equipos organizar su trabajo en tableros visuales con columnas de estado, actualizándose en **tiempo real** gracias a Server-Sent Events (SSE).

> Proyecto universitario desarrollado para demostrar los principios de programación reactiva con Spring WebFlux y autenticación segura con JWT.

---

## ✨ Características Principales

| Funcionalidad | Descripción |
|---|---|
| 🔐 **Autenticación JWT** | Registro e inicio de sesión seguros con tokens Bearer |
| ⚡ **Reactivo No-Bloqueante** | Basado en Spring WebFlux + Project Reactor |
| 🔴 **Tiempo Real (SSE)** | Las tarjetas aparecen en el tablero instantáneamente |
| 🗂️ **Tablero Kanban** | Columnas: *Por Hacer → En Progreso → Completadas* |
| 🌐 **API REST** | Endpoints documentados con OpenAPI / Swagger |
| ☁️ **MongoDB Atlas** | Persistencia en la nube con driver reactivo |
| 🚀 **Desplegado en Railway** | CI/CD automático desde GitHub |

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────┐
│                  CLIENTE                    │
│         index.html (Kanban UI)              │
│         swagger.html (API Docs)             │
└──────────────────┬──────────────────────────┘
                   │ HTTP / SSE
┌──────────────────▼──────────────────────────┐
│           SPRING WEBFLUX (Netty)            │
│                                             │
│  ┌─────────────┐    ┌────────────────────┐  │
│  │ AuthController│  │  TaskController    │  │
│  │ POST /login  │  │  POST /tasks        │  │
│  │ POST /register│  │  GET  /tasks/events │  │
│  └──────┬──────┘    └────────┬───────────┘  │
│         │                   │               │
│  ┌──────▼───────────────────▼───────────┐   │
│  │         Spring Security (Reactive)    │   │
│  │    JWT Filter · AuthManager · CSRF   │   │
│  └──────────────────┬────────────────────┘  │
│                     │                        │
│  ┌──────────────────▼────────────────────┐  │
│  │           Capa de Servicios            │  │
│  │    UserService · TaskService           │  │
│  └──────────────────┬────────────────────┘  │
└─────────────────────┼────────────────────────┘
                      │ Reactive Streams
┌─────────────────────▼────────────────────────┐
│              MongoDB Atlas                    │
│     Colecciones: users · tasks                │
└──────────────────────────────────────────────┘
```

---

## 🛠️ Stack Tecnológico

### Backend
- **Java 21** — Lenguaje principal
- **Spring Boot 4.0.3** — Framework base
- **Spring WebFlux** — Servidor reactivo (Netty, no Tomcat)
- **Spring Security (Reactive)** — Seguridad y autenticación
- **JJWT 0.12.5** — Generación y validación de tokens JWT
- **Project Reactor** — Tipos `Mono<T>` y `Flux<T>`

### Base de Datos
- **MongoDB Atlas** — Clúster en la nube (replica set)
- **Spring Data MongoDB Reactive** — Driver reactivo

### API & Documentación
- **OpenAPI / Springdoc** — Generación automática de docs
- **Swagger UI** — Interfaz para probar endpoints

### DevOps
- **Railway** — Plataforma de despliegue (PaaS)
- **GitHub** — Control de versiones (main / develop / feature)

---

## 🚀 Endpoints de la API

### 🔓 Autenticación (Público)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/auth/register` | Registrar nuevo usuario |
| `POST` | `/auth/login` | Iniciar sesión y obtener JWT |

#### Ejemplo - Registro
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"juan","email":"juan@gmail.com","password":"123456"}'
```

#### Ejemplo - Login
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"juan","password":"123456"}'
# Respuesta: {"token":"eyJhbGciOiJIUzI1NiJ9..."}
```

### 🔒 Tareas (Requiere JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/tasks` | Crear nueva tarea |
| `GET` | `/tasks/events` | Stream SSE de tareas en tiempo real |

#### Ejemplo - Crear Tarea
```bash
curl -X POST http://localhost:8081/tasks \
  -H "Authorization: Bearer <tu-token-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Implementar login","description":"Auth con JWT","status":"TODO"}'
```

---

## ⚙️ Instalación y Ejecución Local

### Prerrequisitos
- Java 21+
- Maven 3.9+
- Cuenta en MongoDB Atlas (gratis)

### Pasos

**1. Clonar el repositorio**
```bash
git clone https://github.com/CesarMoraCorrea/VelocityBoard.git
cd VelocityBoard
```

**2. Crear archivo de variables de entorno**
```bash
# Copia el ejemplo y edita con tus valores
cp .env.example .env
```

```env
MONGODB_URI=mongodb+srv://<usuario>:<password>@<cluster>.mongodb.net/velocityboard
JWT_SECRET=TuClaveSecretaMuyLarga256BitsMinimo
JWT_EXPIRATION=86400000
```

**3. Ejecutar la aplicación**
```bash
./mvnw spring-boot:run
```

**4. Acceder a la aplicación**
| URL | Descripción |
|-----|-------------|
| http://localhost:8081/index.html | 🏠 Tablero Kanban |
| http://localhost:8081/swagger.html | 📚 Documentación API |

---

## 🌿 Estrategia de Ramas (Git Flow)

```
main          ◄── Producción (Railway)
  │
  └── develop ◄── Integración de features
        │
        └── feature/inicio-registro-usuarios
        └── feature/tablero-kanban
        └── feature/...
```

---

## 👥 Equipo de Desarrollo

<div align="center">

| Integrante | Rol |
|---|---|
| **Juan Esteban Losada** | Desarrollador Backend |
| **Juan Diego Velásquez** | Desarrollador Backend |
| **Cesar Mora Correa** | Desarrollador Backend · DevOps |
| **Juan Camilo Aguilar** | Desarrollador Backend |

*Universidad — Programación Avanzada en Java · 2026*

</div>

---

## 📄 Licencia

Este proyecto fue desarrollado con fines académicos.

---

<div align="center">

Hecho con ☕ Java y ❤️ por el equipo VelocityBoard

</div>
