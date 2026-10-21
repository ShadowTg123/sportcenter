package pe.sportcenter.vista;

import pe.sportcenter.controlador.AutenticacionControlador;
import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;
import pe.sportcenter.controlador.CanchaControlador;
import pe.sportcenter.dao.CanchaDAOImpl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;


public class PrincipalVista extends JFrame {

    private final AutenticacionControlador controlador;

    public PrincipalVista(AutenticacionControlador controlador) {
        this.controlador = controlador;

        Usuario usuario = controlador.getUsuarioActual();

        // No permite abrir esta ventana sin una sesión.
        if (usuario == null) {
            throw new SecurityException("Debe iniciar sesión.");
        }

        setTitle("Sport Center - Menú principal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 500);
        setMinimumSize(new Dimension(700, 400));
        setLocationRelativeTo(null);

        JPanel contenedor = new JPanel(new BorderLayout(20, 20));
        contenedor.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel encabezado = new JLabel(
                "SPORT CENTER | " + usuario.getNombre()
                        + " | Perfil: " + usuario.getRol().valorBD()
        );
        encabezado.setFont(new Font("SansSerif", Font.BOLD, 18));
        contenedor.add(encabezado, BorderLayout.NORTH);

        // Menú lateral.
        JPanel menu = new JPanel(new GridLayout(0, 1, 10, 10));

        JButton botonInicio = new JButton("Inicio");
        JButton botonCanchas = new JButton(
                usuario.getRol() == Rol.ADMINISTRADOR
                        ? "Gestionar canchas"
                        : "Consultar canchas"
        );
        JButton botonCerrar = new JButton("Cerrar sesión");

        menu.add(botonInicio);

        // Esta opción solo se muestra al administrador.
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            JButton botonUsuarios = new JButton("Registrar usuario");
            menu.add(botonUsuarios);

            botonUsuarios.addActionListener(e ->
                    new RegistroUsuarioVista(this, controlador).setVisible(true)
            );
        }

        menu.add(botonCanchas);
        menu.add(botonCerrar);

        JPanel lateral = new JPanel(new BorderLayout());
        lateral.add(menu, BorderLayout.NORTH);
        contenedor.add(lateral, BorderLayout.WEST);

        JLabel bienvenida = new JLabel(
                "Seleccione una opción del menú.",
                SwingConstants.CENTER
        );
        bienvenida.setFont(new Font("SansSerif", Font.PLAIN, 20));
        contenedor.add(bienvenida, BorderLayout.CENTER);

        botonInicio.addActionListener(e ->
                bienvenida.setText("Seleccione una opción del menú.")
        );

        botonCanchas.addActionListener(e -> {

            CanchaControlador canchaControlador =
                    new CanchaControlador(
                            new CanchaDAOImpl(),
                            controlador
                    );

            CanchaVista ventana = new CanchaVista(
                    this,
                    canchaControlador,
                    usuario.getRol() == Rol.ADMINISTRADOR
            );

            ventana.setVisible(true);
        });

        botonCerrar.addActionListener(e -> cerrarSesion());

        setContentPane(contenedor);
    }

    private void cerrarSesion() {
        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Desea cerrar la sesión?",
                "Cerrar sesión",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta == JOptionPane.YES_OPTION) {
            controlador.cerrarSesion();

            new LoginVista(controlador).setVisible(true);
            dispose();
        }
    }
}