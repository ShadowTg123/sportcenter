package pe.sportcenter.controlador;

import pe.sportcenter.dao.UsuarioDAO;
import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;
import pe.sportcenter.util.Seguridad;
import pe.sportcenter.util.Validaciones;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutenticacionControlador {

    private static final Logger log =
            LoggerFactory.getLogger(AutenticacionControlador.class);
    private final UsuarioDAO usuarioDAO;
    private Usuario usuarioActual;

    public AutenticacionControlador(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO);
    }

    public Usuario iniciarSesion(String usuario, char[] clave)
            throws SQLException, IOException, GeneralSecurityException {

        // Un intento nuevo elimina cualquier sesión anterior.
        usuarioActual = null;

        String usuarioValidado = Validaciones.validarUsuario(usuario);

        if (clave == null || clave.length == 0 || clave.length > 128) {
            throw new IllegalArgumentException(
                    "Ingrese una contraseña de entre 1 y 128 caracteres.");
        }

        Usuario encontrado = usuarioDAO.autenticar(usuarioValidado, clave);

        if (encontrado == null) {
            log.warn("Inicio de sesión rechazado para usuario={}",
                    usuarioValidado);

            throw new IllegalArgumentException(
                    "Usuario o contraseña incorrectos.");
        }

        usuarioActual = encontrado;

        log.info("Inicio de sesión correcto: idUsuario={}, rol={}",
                usuarioActual.getIdUsuario(),
                usuarioActual.getRol().valorBD());

        return usuarioActual;
    }

    public void registrarUsuario(
            String nombre, String usuario, char[] clave, Rol rol)
            throws SQLException, IOException, GeneralSecurityException {

        // El permiso se comprueba aquí, además de en la interfaz.
        if (usuarioActual == null
                || usuarioActual.getRol() != Rol.ADMINISTRADOR) {
            throw new SecurityException(
                    "Solo el administrador puede registrar usuarios.");
        }

        String nombreValidado = Validaciones.validarNombre(nombre);
        String usuarioValidado = Validaciones.validarUsuario(usuario);
        Validaciones.validarClave(clave);

        if (rol == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un rol.");
        }

        if (usuarioDAO.existeUsuario(usuarioValidado)) {
            throw new IllegalArgumentException(
                    "El nombre de usuario ya está registrado.");
        }

        String claveHash = Seguridad.generarHash(clave);

        usuarioDAO.registrar(
                nombreValidado,
                usuarioValidado,
                claveHash,
                rol
        );
        log.info("Usuario registrado: usuario={}, rol={}, administradorId={}",
                usuarioValidado,
                rol.valorBD(),
                usuarioActual.getIdUsuario());
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public void cerrarSesion() {
        if (usuarioActual != null) {
            log.info("Cierre de sesión: idUsuario={}",
                    usuarioActual.getIdUsuario());
        }

        usuarioActual = null;
    }
}