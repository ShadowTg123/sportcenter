# Sport Center

Sistema de escritorio para la gestión del alquiler de canchas deportivas.

## Avance 1: Logeo

Funcionalidades implementadas:

- Inicio y cierre de sesión.
- Perfiles de administrador y recepcionista.
- Registro de usuarios exclusivo del administrador.
- Validación de los datos ingresados.
- Protección de contraseñas mediante PBKDF2 con sal aleatoria.
- Registro de eventos mediante Logback.

## Gestión de canchas

- Registro, consulta y modificación de canchas.
- Validación del nombre, deporte y tarifa por hora.
- Eliminación lógica mediante el estado inactiva.
- Eliminación física únicamente cuando la cancha no tiene alquileres asociados.
- Administrador: acceso a todas las operaciones.
- Recepcionista: consulta de canchas activas, sin permisos de modificación.

## Preparación de la base de datos

Ejecutar en MySQL Workbench, en este orden:

1. sql/01_logeo.sql
2. sql/02_crud_canchas.sql

## Tecnologías

- Java 22, según la configuración actual del pom.xml.
- Java Swing.
- Maven.
- MySQL 8 y JDBC.
- Logback.

## Organización

- modelo: clases que representan los datos y roles.
- vista: ventanas y formularios.
- controlador: validaciones de flujo y autorización.
- dao: operaciones con la base de datos.
- util: conexión, validaciones y seguridad.

## Preparación y ejecución

1. Abrir el proyecto en IntelliJ IDEA con JDK 22.
2. Recargar Maven para descargar las dependencias.
3. Iniciar MySQL Server.
4. Ejecutar sql/01_logeo.sql en MySQL Workbench.
5. Copiar db.properties.example como db.properties en la raíz.
6. Configurar el usuario, contraseña y URL de MySQL en db.properties.
7. Establecer la raíz del proyecto como directorio de trabajo.
8. Si la tabla USUARIO está vacía, ejecutar
   pe.sportcenter.util.InicializarAdministrador para crear la primera cuenta.
9. Ejecutar pe.sportcenter.Main para abrir el sistema.

Las cuentas adicionales se crean desde el menú del administrador.

## Comprobaciones manuales

- Ejecutar PruebaSeguridad desde src/test/java.
- Comprobar el acceso con credenciales correctas e incorrectas.
- Registrar un recepcionista e iniciar sesión con esa cuenta.
- Verificar que el recepcionista no tenga la opción Registrar usuario.
- Comprobar el cierre de sesión y los eventos generados.

## Configuración y eventos

db.properties contiene las credenciales locales y está excluido de Git.

Los eventos se guardan en logs/sportcenter.log.
La carpeta logs también está excluida de Git.