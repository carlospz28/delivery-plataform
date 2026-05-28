🍔 FoodExpress — Plataforma de Delivery de Comida

Una API que conecta clientes, restaurantes y repartidores en un solo sistema, al estilo Uber Eats o Rappi.


¿De qué trata este proyecto?
FoodExpress es el "cerebro" detrás de una app de domicilios. No es una aplicación con pantallas o botones visibles — es el motor que maneja toda la lógica: quién puede pedir, qué hay en el menú, cómo se procesa un pedido y quién lo lleva a tu puerta.
Imagínalo así: cuando abres Rappi y pides una hamburguesa, hay un sistema que verifica tu identidad, busca los restaurantes disponibles, guarda tu pedido, le avisa al restaurante, y luego coordina al repartidor. Este proyecto hace exactamente eso.

¿Cómo funciona el sistema?
El sistema reconoce tres tipos de usuarios, cada uno con acciones distintas:
🧑‍💼 Cliente

Puede explorar los restaurantes disponibles y sus menús.
Puede hacer un pedido y hacer seguimiento de su estado.

🍽️ Restaurante

Puede administrar sus platos: agregar, editar o eliminar del menú.
Recibe notificaciones cuando llega un pedido y puede marcarlo como "en preparación" o "listo".

🛵 Repartidor

Ve los pedidos que ya están listos para recoger.
Puede tomar un pedido y actualizar su estado hasta entregarlo.


¿Cómo está organizado por dentro?
El proyecto está construido de forma modular — como si fuera un conjunto de departamentos dentro de una empresa, cada uno con una responsabilidad clara:

Autenticación: Se encarga de que cada usuario inicie sesión de forma segura y solo pueda hacer lo que le corresponde según su rol.
Catálogo: Gestiona toda la información de los restaurantes y sus platos.
Pedidos: El corazón del sistema — registra, actualiza y coordina cada orden.
Notificaciones: Avisa a las partes involucradas cuando algo cambia en el estado de un pedido.

Esta separación hace que el proyecto sea fácil de mantener y escalar a futuro.

¿Qué tecnologías usa?
No es necesario conocerlas todas para entender el proyecto, pero aquí va un resumen sencillo:
Qué haceTecnologíaLenguaje principalJava 21Framework webSpring BootSeguridad y autenticaciónSpring Security + JWTBase de datosPostgreSQLComunicación con la base de datosSpring Data JPA / Hibernate
JWT (JSON Web Token) es como un "pase de acceso" digital: cuando un usuario inicia sesión, el sistema le entrega un token firmado que deberá presentar en cada solicitud para demostrar quién es.

¿Cómo correr el proyecto?
Necesitas tener instalado Java 21 y PostgreSQL. Luego:
1. Prepara la base de datos
Crea una base de datos en PostgreSQL llamada delivery_db corriendo en el puerto 5432.
2. Configura las variables de entorno
El sistema necesita dos datos sensibles que no se guardan en el código por seguridad:
DB_PASSWORD   → La contraseña de tu base de datos PostgreSQL
JWT_SECRET    → Una clave secreta para firmar los tokens de acceso
3. Inicia el servidor
bash./mvnw spring-boot:run
Al arrancar por primera vez, el sistema creará automáticamente todas las tablas necesarias en la base de datos. No tienes que hacerlo a mano.

¿Qué puedo explorar una vez que esté corriendo?
Una vez levantado el servidor, tendrás acceso a los endpoints REST de la API (normalmente en http://localhost:8080). Puedes probarlos con herramientas como Postman o Insomnia, o revisar la documentación generada automáticamente si está habilitada.

Estructura del proyecto (para desarrolladores)
Cada módulo del sistema sigue la misma organización interna:
modulo/
├── domain/model          → Las entidades (tablas de la base de datos)
├── application/dto       → Los datos que entran y salen por la API
├── application/service   → La lógica de negocio
├── infrastructure/
│   ├── persistence       → Consultas a la base de datos
│   └── controller        → Los endpoints HTTP expuestos
