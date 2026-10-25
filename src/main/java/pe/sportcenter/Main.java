package pe.sportcenter;

import pe.sportcenter.controlador.AutenticacionControlador;
import pe.sportcenter.dao.UsuarioDAO;
import pe.sportcenter.dao.UsuarioDAOImpl;
import pe.sportcenter.vista.LoginVista;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada de SportCenter. Construye el DAO, el controlador de autenticación y la ventana
 * de login en el hilo de eventos de Swing.
 */
public class Main {

    public static void main(String[] args) {

        // Las ventanas de Swing se crean en su hilo de interfaz.
        SwingUtilities.invokeLater(
                () -> {
                    UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

                    AutenticacionControlador controlador = new AutenticacionControlador(usuarioDAO);

                    LoginVista ventana = new LoginVista(controlador);
                    ventana.setVisible(true);
                });
    }
}