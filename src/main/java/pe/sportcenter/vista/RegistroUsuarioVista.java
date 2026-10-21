package pe.sportcenter.vista;

import pe.sportcenter.controlador.AutenticacionControlador;
import pe.sportcenter.modelo.Rol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class RegistroUsuarioVista extends JDialog {

    private final AutenticacionControlador controlador;

    private final JTextField campoNombre = new JTextField(25);
    private final JTextField campoUsuario = new JTextField(25);
    private final JPasswordField campoClave = new JPasswordField(25);
    private final JPasswordField campoConfirmacion = new JPasswordField(25);
    private final JComboBox<Rol> campoRol = new JComboBox<>(Rol.values());

    private final JButton botonGuardar = new JButton("Guardar");
    private final JButton botonCancelar = new JButton("Cancelar");

    private boolean procesando;

    public RegistroUsuarioVista(
            JFrame propietario,
            AutenticacionControlador controlador) {

        super(propietario, "Sport Center - Registrar usuario", true);
        this.controlador = controlador;

        if (controlador.getUsuarioActual() == null
                || controlador.getUsuarioActual().getRol()
                != Rol.ADMINISTRADOR) {
            throw new SecurityException(
                    "Solo el administrador puede abrir este formulario.");
        }

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);

        JPanel formulario = new JPanel(new GridLayout(0, 1, 6, 6));
        formulario.setBorder(new EmptyBorder(20, 25, 20, 25));

        formulario.add(new JLabel("Nombre completo:"));
        formulario.add(campoNombre);

        formulario.add(new JLabel("Usuario (4 a 30 caracteres):"));
        formulario.add(campoUsuario);

        formulario.add(new JLabel("Contraseña (8 a 128, letras y números):"));
        formulario.add(campoClave);

        formulario.add(new JLabel("Confirmar contraseña:"));
        formulario.add(campoConfirmacion);

        formulario.add(new JLabel("Perfil:"));
        campoRol.setSelectedItem(Rol.RECEPCIONISTA);
        formulario.add(campoRol);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(botonCancelar);
        botones.add(botonGuardar);

        formulario.add(botones);
        setContentPane(formulario);

        botonGuardar.addActionListener(e -> guardarUsuario());
        botonCancelar.addActionListener(e -> cerrarFormulario());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarFormulario();
            }
        });

        getRootPane().setDefaultButton(botonGuardar);

        pack();
        setLocationRelativeTo(propietario);
    }

    private void guardarUsuario() {
        if (procesando) {
            return;
        }

        String nombre = campoNombre.getText();
        String usuario = campoUsuario.getText();
        Rol rol = (Rol) campoRol.getSelectedItem();

        char[] clave = campoClave.getPassword();
        char[] confirmacion = campoConfirmacion.getPassword();

        boolean coinciden = Arrays.equals(clave, confirmacion);
        Arrays.fill(confirmacion, '\0');

        if (!coinciden) {
            Arrays.fill(clave, '\0');
            limpiarClaves();
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        limpiarClaves();
        cambiarEstado(true);

        // El registro se ejecuta sin bloquear la interfaz.
        SwingWorker<Void, Void> tarea = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    controlador.registrarUsuario(
                            nombre, usuario, clave, rol);
                    return null;
                } finally {
                    Arrays.fill(clave, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    get();

                    JOptionPane.showMessageDialog(
                            RegistroUsuarioVista.this,
                            "Usuario registrado correctamente."
                    );

                    dispose();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError(
                            "La operación fue interrumpida. "
                                    + "Compruebe el registro antes de reintentar.");

                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();

                    if (causa instanceof IllegalArgumentException
                            || causa instanceof SecurityException) {

                        mostrarError(causa.getMessage());

                    } else if (causa instanceof SQLException sql
                            && sql.getErrorCode() == 1062) {

                        mostrarError(
                                "El nombre de usuario ya está registrado.");

                    } else {
                        mostrarError(
                                "No se pudo confirmar el registro. "
                                        + "Revise la conexión con MySQL.");
                        causa.printStackTrace();
                    }

                } finally {
                    cambiarEstado(false);
                }
            }
        };

        tarea.execute();
    }

    private void cambiarEstado(boolean ocupado) {
        procesando = ocupado;

        campoNombre.setEnabled(!ocupado);
        campoUsuario.setEnabled(!ocupado);
        campoClave.setEnabled(!ocupado);
        campoConfirmacion.setEnabled(!ocupado);
        campoRol.setEnabled(!ocupado);
        botonGuardar.setEnabled(!ocupado);
        botonCancelar.setEnabled(!ocupado);

        botonGuardar.setText(ocupado ? "Guardando..." : "Guardar");
    }

    private void limpiarClaves() {
        campoClave.setText("");
        campoConfirmacion.setText("");
    }

    private void cerrarFormulario() {
        if (!procesando) {
            limpiarClaves();
            dispose();
        }
    }

    private void mostrarError(String texto) {
        JOptionPane.showMessageDialog(
                this,
                texto,
                "Registro de usuario",
                JOptionPane.ERROR_MESSAGE
        );
    }
}