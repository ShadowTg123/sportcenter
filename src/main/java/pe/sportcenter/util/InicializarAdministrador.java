package pe.sportcenter.util;

import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

import javax.swing.*;

/**
 * Herramienta de configuración inicial para crear el primer administrador. Se ejecuta por separado
 * de Main y comprueba que no existan usuarios registrados.
 */
public class InicializarAdministrador {

    public static void main(String[] args) {

        // Comprueba si ya existen usuarios antes de mostrar el formulario.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta =
                     conexion.prepareStatement("SELECT COUNT(*) FROM USUARIO");
             ResultSet resultado = consulta.executeQuery()) {

            resultado.next();

            if (resultado.getInt(1) > 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "Ya existen usuarios registrados. "
                                + "La configuración inicial no está disponible.");
                return;
            }

        } catch (Exception e) {
            mostrarError(e);
            return;
        }

        JTextField campoNombre = new JTextField();
        JTextField campoUsuario = new JTextField();
        JPasswordField campoClave = new JPasswordField();
        JPasswordField campoConfirmacion = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        panel.add(new JLabel("Nombre del administrador:"));
        panel.add(campoNombre);

        panel.add(new JLabel("Usuario:"));
        panel.add(campoUsuario);

        panel.add(new JLabel("Contraseña (8 a 128 caracteres, letras y números):"));
        panel.add(campoClave);

        panel.add(new JLabel("Confirmar contraseña:"));
        panel.add(campoConfirmacion);

        int opcion =
                JOptionPane.showConfirmDialog(
                        null,
                        panel,
                        "Sport Center - Configuración inicial",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);

        char[] clave = campoClave.getPassword();
        char[] confirmacion = campoConfirmacion.getPassword();

        try {
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            String nombre = Validaciones.validarNombre(campoNombre.getText());

            String usuario = Validaciones.validarUsuario(campoUsuario.getText());

            Validaciones.validarClave(clave);

            if (!Arrays.equals(clave, confirmacion)) {
                throw new IllegalArgumentException("Las contraseñas no coinciden.");
            }

            String hash = Seguridad.generarHash(clave);

            // Vuelve a comprobar que la tabla esté vacía al insertar.
            String sql =
                    """
                    INSERT INTO USUARIO (nombre, usuario, clave, rol)
                    SELECT ?, ?, ?, 'administrador'
                    WHERE NOT EXISTS (SELECT 1 FROM USUARIO)
                    """;

            try (Connection conexion = Conexion.getInstancia().abrirConexion();
                 PreparedStatement consulta = conexion.prepareStatement(sql)) {

                consulta.setString(1, nombre);
                consulta.setString(2, usuario);
                consulta.setString(3, hash);

                int filas = consulta.executeUpdate();

                if (filas == 1) {
                    JOptionPane.showMessageDialog(null, "Administrador creado correctamente.");
                } else {
                    JOptionPane.showMessageDialog(
                            null,
                            "No se creó el administrador porque "
                                    + "ya existen usuarios registrados.");
                }
            }

        } catch (Exception e) {
            mostrarError(e);

        } finally {
            // Limpia las contraseñas utilizadas en esta herramienta.
            Arrays.fill(clave, '\0');
            Arrays.fill(confirmacion, '\0');
            campoClave.setText("");
            campoConfirmacion.setText("");
        }
    }

    /** Presenta el mensaje de error al usuario mediante un diálogo. */
    private static void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(
                null, e.getMessage(), "Error de configuración", JOptionPane.ERROR_MESSAGE);
    }
}
