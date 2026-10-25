package pe.sportcenter.dao;

import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

/**
 * Contrato de acceso a usuarios: autenticación, comprobación de existencia y registro. Las
 * implementaciones comunican fallos de configuración, SQL o seguridad al controlador.
 */
public interface UsuarioDAO {

    Usuario autenticar(String usuario, char[] clave)
            throws SQLException, IOException, GeneralSecurityException;

    boolean existeUsuario(String usuario) throws SQLException, IOException;

    void registrar(String nombre, String usuario, String claveHash, Rol rol)
            throws SQLException, IOException;
}