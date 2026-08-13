# Delivery Platform

API REST para una plataforma de domicilios, construida con **Spring Boot** siguiendo los principios de **Clean Architecture**. Proyecto de grado.

## 🚀 Características

- Arquitectura limpia con módulos independientes: **Autenticación**, **Pedidos**, **Catálogo** y **Notificaciones**
- Autenticación y autorización con **JWT**
- Máquina de estados completa para el ciclo de vida de un pedido (creado → confirmado → en camino → entregado, etc.)
- **39 pruebas unitarias** cubriendo la lógica de negocio
- Desplegado en producción sobre **Render** con base de datos **PostgreSQL**

## 🛠️ Stack técnico

- Java 17+ / Spring Boot
- PostgreSQL
- Spring Security + JWT
- JUnit / Mockito para pruebas
- Docker
- Despliegue: Render

## 📂 Arquitectura

El proyecto sigue Clean Architecture, separando:

- **Dominio**: entidades y reglas de negocio puras, sin dependencias externas
- **Aplicación**: casos de uso que orquestan la lógica del dominio
- **Infraestructura**: controladores REST, persistencia (JPA/PostgreSQL), seguridad

Esto permite que la lógica de negocio no dependa de frameworks ni de la base de datos, facilitando las pruebas y el mantenimiento.

## ▶️ Cómo ejecutar el proyecto localmente

```bash
# Clonar el repositorio
git clone https://github.com/carlospz28/delivery-plataform.git
cd delivery-plataform

# Configurar variables de entorno (base de datos, JWT secret, etc.)
# Ver application.properties / application.yml

# Ejecutar con Maven Wrapper
./mvnw spring-boot:run
```

El servicio quedará disponible en `http://localhost:8080`.

## 🧪 Ejecutar pruebas

```bash
./mvnw test
```

## 🌐 Demo en vivo

<!-- Si tienes la URL de Render, agrégala aquí, por ejemplo: -->
<!-- [Ver API desplegada](https://delivery-plataform.onrender.com) -->

## 👤 Autor

**Carlos Enrique Páez Durán**
Estudiante de Ingeniería de Sistemas y Computación — Universidad de Cundinamarca
[GitHub](https://github.com/carlospz28) · carlospaez2429@gmail.com
