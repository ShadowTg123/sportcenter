package pe.sportcenter.vista;

import pe.sportcenter.controlador.AutenticacionControlador;
import pe.sportcenter.modelo.Usuario;

import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Formulario de inicio de sesión. Ejecuta la autenticación con SwingWorker para mantener la
 * interfaz disponible mientras se consulta MySQL y se verifica el hash.
 */
public class LoginVista extends JFrame {

    private final AutenticacionControlador controlador;

    private final JTextField campoUsuario = new JTextField(20);
    private final JPasswordField campoClave = new JPasswordField(20);
    private final JButton botonIngresar = new JButton("Ingresar");
    private final JLabel mensaje = new JLabel(" ");

    public LoginVista(AutenticacionControlador controlador) {
        this.controlador = controlador;

        setTitle("Sport Center - Iniciar sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel titulo = new JLabel("SPORT CENTER", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));

        panel.add(titulo);
        panel.add(new JLabel("Usuario:"));
        panel.add(campoUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(campoClave);
        panel.add(botonIngresar);
        panel.add(mensaje);

        setContentPane(panel);

        // Permite ingresar pulsando Enter.
        getRootPane().setDefaultButton(botonIngresar);
        botonIngresar.addActionListener(e -> iniciarSesion());

        pack();
        setLocationRelativeTo(null);
    }

    /** Autentica en segundo plano y abre el menú principal solo si obtiene un usuario válido. */
    private void iniciarSesion() {
        String usuario = campoUsuario.getText();
        char[] clave = campoClave.getPassword();

        campoClave.setText("");
        cambiarEstado(true);
        mensaje.setText("Verificando acceso...");

        // Consulta MySQL sin bloquear la ventana.
        SwingWorker<Usuario, Void> tarea =
                new SwingWorker<>() {

                    @Override
                    protected Usuario doInBackground() throws Exception {
                        try {
                            return controlador.iniciarSesion(usuario, clave);
                        } finally {
                            Arrays.fill(clave, '\0');
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            // Confirma que la autenticación terminó correctamente.
                            get();

                            // Abre el menú conservando la sesión del controlador.
                            PrincipalVista principal = new PrincipalVista(controlador);
                            principal.setVisible(true);

                            // Cierra la ventana de login.
                            LoginVista.this.dispose();

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            controlador.cerrarSesion();
                            mensaje.setText("La operación fue interrumpida.");

                        } catch (ExecutionException e) {
                            controlador.cerrarSesion();
                            Throwable causa = e.getCause();

                            if (causa instanceof IllegalArgumentException) {
                                mostrarError(causa.getMessage());
                            } else {
                                mostrarError(
                                        "No se pudo iniciar sesión. "
                                                + "Revise la conexión con la base de datos.");

                                // Diagnóstico temporal para el desarrollador.
                                causa.printStackTrace();
                            }

                        } finally {
                            cambiarEstado(false);
                        }
                    }
                };

        tarea.execute();
    }

    /** Habilita o deshabilita los controles mientras se procesa la operación. */
    private void cambiarEstado(boolean procesando) {
        botonIngresar.setEnabled(!procesando);
        campoUsuario.setEnabled(!procesando);
        campoClave.setEnabled(!procesando);
    }

    /** Presenta el mensaje de error al usuario mediante un diálogo. */
    private void mostrarError(String texto) {
        mensaje.setText("No se pudo ingresar.");

        JOptionPane.showMessageDialog(this, texto, "Inicio de sesión", JOptionPane.ERROR_MESSAGE);
    }
}
