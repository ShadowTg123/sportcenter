// : Paquete de esta clase: coincide con su ubicación en src/main/java y permite que otras clases la importen.
package pe.sportcenter.controlador;
// : Interfaz para registrar eventos de aplicación sin imprimir contraseñas.
import org.slf4j.Logger;
// : Fábrica que crea el objeto de registro de eventos (log).
import org.slf4j.LoggerFactory;
// : Contrato de la capa DAO para buscar/registrar usuarios en MySQL.
import pe.sportcenter.dao.UsuarioDAO;
// : Enumeración de permisos ADMINISTRADOR y RECEPCIONISTA.
import pe.sportcenter.modelo.Rol;
// : Modelo que representa la cuenta y rol autenticados.
import pe.sportcenter.modelo.Usuario;
// : Funciones para generar/verificar hash de contraseñas.
import pe.sportcenter.util.Seguridad;
// : Reglas para nombre, cuenta y contraseña.
import pe.sportcenter.util.Validaciones;

// : Excepción en operaciones de entrada/salida como configuración.
import java.io.IOException;
// : Excepción posible al generar o verificar hashes.
import java.security.GeneralSecurityException;
// : Excepción cuando falla consulta o actualización de MySQL.
import java.sql.SQLException;
// : Permite comprobar referencias nulas con requireNonNull.
import java.util.Objects;

/**
 * Gestiona el inicio y cierre de sesión y el registro de usuarios. Comprueba el permiso de
 * administrador antes de registrar nuevas cuentas.
 */
// : Declara la clase y su responsabilidad dentro del módulo SportCenter.
public class AutenticacionControlador {

    // : Logger compartido para registrar eventos de inicio, cierre o creación de usuarios.
    private static final Logger log = LoggerFactory.getLogger(AutenticacionControlador.class);
    // : Dependencia o atributo final: se asigna en el constructor y no se reemplaza después.
    private final UsuarioDAO usuarioDAO;
    // : Dato de sesión: usuario autenticado; null significa que no existe sesión.
    private Usuario usuarioActual;

    // : Constructor: recibe un UsuarioDAO. Este DAO es la pieza que consulta y escribe en MySQL; se inyecta en lugar de crearla aquí.
    public AutenticacionControlador(UsuarioDAO usuarioDAO) {
        // : Obliga a recibir una dependencia no nula: falla pronto si está mal configurada.
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO);
    }

    /**
     * Limpia la sesión anterior, valida el usuario y solicita al DAO verificar las credenciales.
     */

    //En AutenticacionControlador, el método iniciarSesion() permite
    // verificar las credenciales del usuario, diferenciando los perfiles de administrador y recepcionista.

    // odjetivo1: iniciarSesion()
    // invalida la sesión previa; valida nombre de usuario y formato mínimo de clave; consulta al DAO y almacena el Usuario cuando se autentica.

    //Metodo iniciarSesion que devuelve el odjeto Usuario,
    //devuelve un objeto Usuario y recibe dos parámetros: usuario,
    // un String con el nombre de la cuenta, y clave, un char[], o sea un arreglo de caracteres.
    // Puede lanzar excepciones de base de datos, de configuración y de seguridad, que suben hasta la vista para mostrar un mensaje.”
    //JPasswordField componente
    public Usuario iniciarSesion(String usuario, char[] clave)
            throws SQLException, IOException, GeneralSecurityException {

        // : Se borra la sesión; en iniciarSesion impide que quede la cuenta anterior, en cerrarSesion termina el acceso.
        //Lo primero es poner usuarioActual en null. Así, si este intento falla, no se queda abierta la sesión anterior.”
        //A la variable usuarioActual se le asigna el valor null, significa que no hay ningún usuario conectado.
        usuarioActual = null;

        //declaramos una variable usuarioValidado, y lo guardamos en el metodo Validaciones.validarUsuario(usuario);
        //Se lo paso a Validaciones.validarUsuario, que comprueba que exista y que tenga entre 4 y 30 caracteres con formato permitido.
        // Devuelve el texto ya validado. Valido en el controlador y no solo en la pantalla.”
        String usuarioValidado = Validaciones.validarUsuario(usuario);

        // : En LOGIN la contraseña solo debe existir y no superar 128 caracteres; las 5 reglas fuertes se comprueban al crear cuentas
        if (clave == null || clave.length == 0 || clave.length > 128) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException(
                    "Ingrese una contraseña de entre 1 y 128 caracteres.");
        }

        // : Se envían usuario y contraseña al DAO; allí se consulta la cuenta y se verifica el hash.
        //Envío usuario y clave a usuarioDAO.autenticar. Ahí se consulta la tabla USUARIO y se compara
        // contra el hash guardado, nunca contra texto plano. Es el patrón DAO: el controlador no sabe de SQL.”
        Usuario encontrado = usuarioDAO.autenticar(usuarioValidado, clave);

        // : Si el DAO no devuelve usuario, significa que las credenciales no son válidas.
        //Registro un warn en el log con el usuario, sin contraseña, y lanzo una excepción con un mensaje genérico:
        // ‘Usuario o contraseña incorrectos’. No digo cuál de los dos falló, para no darle pistas a quien prueba cuentas
        if (encontrado == null) {
            // : Registra actividad técnica en el log sin exponer contraseñas.
            log.warn("Inicio de sesión rechazado para usuario={}", usuarioValidado);

            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        }

        // : Guarda la cuenta y rol autenticados para el resto de la aplicación.
        //Si es correcto, guarda la sesión. Guardo el usuario autenticado en usuarioActual, registro un info con el id y el rol, y lo devuelvo.
        // Ese usuarioActual es la sesión: el resto del sistema consulta su rol para decidir qué puede hacer un administrador y qué un recepcionista
        usuarioActual = encontrado;

        // : Registra actividad técnica en el log sin exponer contraseñas.

        log.info(
                "Inicio de sesión correcto: idUsuario={}, rol={}",
                usuarioActual.getIdUsuario(),
                usuarioActual.getRol().valorBD());

        // : Devuelve el objeto usuario autenticado al llamador; sirve para conocer el perfil.
        return usuarioActual;
    }

    /** Comprueba el rol administrador, valida los datos y guarda la contraseña como hash. */
    // : REGISTRO Y AUTORIZACIÓN: solo ADMINISTRADOR puede crear cuentas; comprueba nombre/usuario/clave/rol, evita repetidos y guarda hash.
    public void registrarUsuario(String nombre, String usuario, char[] clave, Rol rol)
            throws SQLException, IOException, GeneralSecurityException {

        // El permiso se comprueba aquí, además de en la interfaz.
        // : Control efectivo de permisos: incluso si alguien intentara saltarse la interfaz, no podrá registrar usuarios.
        if (usuarioActual == null || usuarioActual.getRol() != Rol.ADMINISTRADOR) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new SecurityException("Solo el administrador puede registrar usuarios.");
        }

        // : Guarda texto validado u obtenido de la consulta para las comprobaciones siguientes.
        String nombreValidado = Validaciones.validarNombre(nombre);
        // : Verifica que el nombre de cuenta esté presente y tenga formato permitido (4-30 caracteres).
        String usuarioValidado = Validaciones.validarUsuario(usuario);
        // : Aquí sí se exigen longitud >=8, minúscula, mayúscula, número y símbolo.
        Validaciones.validarClave(clave);

        // : Comprueba que el dato requerido no sea nulo antes de continuar.
        if (rol == null) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("Debe seleccionar un rol.");
        }

        // : No permite dos cuentas con el mismo nombre de usuario.
        if (usuarioDAO.existeUsuario(usuarioValidado)) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("El nombre de usuario ya está registrado.");
        }

        // : PBKDF2 convierte la contraseña en hash con sal: NO se almacena la contraseña original.
        String claveHash = Seguridad.generarHash(clave);

        // : El DAO ejecuta el INSERT del usuario con hash y rol en MySQL.
        usuarioDAO.registrar(nombreValidado, usuarioValidado, claveHash, rol);
        // : Registra actividad técnica en el log sin exponer contraseñas.
        log.info(
                "Usuario registrado: usuario={}, rol={}, administradorId={}",
                usuarioValidado,
                rol.valorBD(),
                usuarioActual.getIdUsuario());
    }

    // : Devuelve el usuario cuya sesión sigue abierta (o null si no hay sesión). Los otros controladores consultan este dato para autorizar.
    public Usuario getUsuarioActual() {
        // : Devuelve el objeto usuario autenticado al llamador; sirve para conocer el perfil.
        return usuarioActual;
    }

    /** Elimina la referencia al usuario autenticado. */
    // : Cierra la sesión: registra el evento si había usuario y borra la referencia usuarioActual.
    public void cerrarSesion() {
        // : Ejecuta esta acción solo cuando existe el dato opcional.
        if (usuarioActual != null) {
            // : Registra actividad técnica en el log sin exponer contraseñas.
            log.info("Cierre de sesión: idUsuario={}", usuarioActual.getIdUsuario());
        }

        // : Se borra la sesión; en iniciarSesion impide que quede la cuenta anterior, en cerrarSesion termina el acceso.
        usuarioActual = null;
    }
}
