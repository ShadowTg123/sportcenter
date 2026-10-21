package pe.sportcenter.dao;

import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;
import pe.sportcenter.util.Conexion;
import pe.sportcenter.util.Seguridad;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public Usuario autenticar(String usuario, char[] clave)
            throws SQLException, IOException, GeneralSecurityException {

        String sql = """
                SELECT idUsuario, nombre, usuario, clave, rol
                FROM USUARIO
                WHERE usuario = ?
                """;

        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            consulta.setString(1, usuario);

            try (ResultSet resultado = consulta.executeQuery()) {

                if (resultado.next()) {
                    String claveHash = resultado.getString("clave");

                    if (Seguridad.verificarClave(clave, claveHash)) {
                        return new Usuario(
                                resultado.getInt("idUsuario"),
                                resultado.getString("nombre"),
                                resultado.getString("usuario"),
                                Rol.desdeBD(resultado.getString("rol"))
                        );
                    }
                }
            }
        }

        // Usuario inexistente o contraseña incorrecta.
        return null;
    }

    @Override
    public boolean existeUsuario(String usuario)
            throws SQLException, IOException {

        String sql = "SELECT 1 FROM USUARIO WHERE usuario = ?";

        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            consulta.setString(1, usuario);

            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next();
            }
        }
    }

    @Override
    public void registrar(
            String nombre, String usuario, String claveHash, Rol rol)
            throws SQLException, IOException {

        String sql = """
                INSERT INTO USUARIO (nombre, usuario, clave, rol)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            consulta.setString(1, nombre);
            consulta.setString(2, usuario);
            consulta.setString(3, claveHash);
            consulta.setString(4, rol.valorBD());

            consulta.executeUpdate();
        }
    }
}