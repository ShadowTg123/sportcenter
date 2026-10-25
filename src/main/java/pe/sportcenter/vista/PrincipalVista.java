// : Paquete de esta clase: coincide con su ubicación en src/main/java y permite que otras clases la importen.
package pe.sportcenter.vista;

// : Coordina alquileres y verifica permisos/horarios.
import pe.sportcenter.controlador.AlquilerControlador;
// : Mantiene la sesión y autoriza acciones de registro de usuarios.
import pe.sportcenter.controlador.AutenticacionControlador;
// : Coordina CRUD de canchas y comprueba rol.
import pe.sportcenter.controlador.CanchaControlador;
// : Coordina operaciones con clientes.
import pe.sportcenter.controlador.ClienteControlador;
// : Implementación JDBC de operaciones de alquiler.
import pe.sportcenter.dao.AlquilerDAOImpl;
// : Implementación JDBC de operaciones de cancha.
import pe.sportcenter.dao.CanchaDAOImpl;
// : Implementación JDBC de operaciones de clientes.
import pe.sportcenter.dao.ClienteDAOImpl;
// : Enumeración de permisos ADMINISTRADOR y RECEPCIONISTA.
import pe.sportcenter.modelo.Rol;
// : Modelo que representa la cuenta y rol autenticados.
import pe.sportcenter.modelo.Usuario;

// : Importa *: tipo o clase utilizado más abajo en este archivo.
import java.awt.*;

// : Importa *: tipo o clase utilizado más abajo en este archivo.
import javax.swing.*;
// : Importa EmptyBorder: tipo o clase utilizado más abajo en este archivo.
import javax.swing.border.EmptyBorder;

/**
 * Menú principal de la sesión. Abre los módulos con el controlador de autenticación compartido y
 * muestra el registro de usuarios únicamente al administrador.
 */
// : Declara la clase y su responsabilidad dentro del módulo SportCenter.
public class PrincipalVista extends JFrame {

    // : Dependencia o atributo final: se asigna en el constructor y no se reemplaza después.
    private final AutenticacionControlador controlador;

    // : Constructor del menú principal: consulta la sesión activa, arma la interfaz y habilita rutas de navegación según perfil.
    public PrincipalVista(AutenticacionControlador controlador) {
        // : Inicializa/usa una referencia de un objeto necesario en este módulo.
        this.controlador = controlador;

        // : Recupera la sesión ya autenticada para saber quién ingresó y qué rol posee.
        Usuario usuario = controlador.getUsuarioActual();

        // No permite abrir esta ventana sin una sesión.
        // : No se debe abrir el menú principal si no existe un usuario autenticado.
        if (usuario == null) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new SecurityException("Debe iniciar sesión.");
        }

        // : Configura la interfaz, una propiedad de objeto o un parámetro para la operación.
        setTitle("Sport Center - Menú principal");
        // : Configura la interfaz, una propiedad de objeto o un parámetro para la operación.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // : Configura la interfaz, una propiedad de objeto o un parámetro para la operación.
        setSize(850, 500);
        // : Configura la interfaz, una propiedad de objeto o un parámetro para la operación.
        setMinimumSize(new Dimension(700, 400));
        // : Configura la interfaz, una propiedad de objeto o un parámetro para la operación.
        setLocationRelativeTo(null);

        // : Construye un componente de Swing o define su diseño en la interfaz.
        JPanel contenedor = new JPanel(new BorderLayout(20, 20));
        // : Configura la interfaz, una propiedad de objeto o un parámetro para la operación.
        contenedor.setBorder(new EmptyBorder(20, 20, 20, 20));

        // : Construye un componente de Swing o define su diseño en la interfaz.
        JLabel encabezado =
                // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                new JLabel(
                        "SPORT CENTER | "
                                + usuario.getNombre()
                                + " | Perfil: "
                                + usuario.getRol().valorBD());
        // : Configura la interfaz, una propiedad de objeto o un parámetro para la operación.
        encabezado.setFont(new Font("SansSerif", Font.BOLD, 18));
        // : Añade el elemento a la colección o contenedor correspondiente.
        contenedor.add(encabezado, BorderLayout.NORTH);

        // Menú lateral.
        // : Construye un componente de Swing o define su diseño en la interfaz.
        JPanel menu = new JPanel(new GridLayout(0, 1, 10, 10));

        // : Construye un componente de Swing o define su diseño en la interfaz.
        JButton botonInicio = new JButton("Inicio");
        // : Construye un componente de Swing o define su diseño en la interfaz.
        JButton botonCanchas =
                // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                new JButton(
                        // : Ternario: el administrador verá el texto Gestionar canchas; el recepcionista, Consultar canchas.
                        usuario.getRol() == Rol.ADMINISTRADOR
                                ? "Gestionar canchas"
                                : "Consultar canchas");
        // : Construye un componente de Swing o define su diseño en la interfaz.
        JButton botonClientes = new JButton("Clientes");
        // : Construye un componente de Swing o define su diseño en la interfaz.
        JButton botonAlquileres = new JButton("Alquileres");
        // : Construye un componente de Swing o define su diseño en la interfaz.
        JButton botonReportes = new JButton("Reportes de alquileres");
        // : Construye un componente de Swing o define su diseño en la interfaz.
        JButton botonCerrar = new JButton("Cerrar sesión");

        // : Añade el elemento a la colección o contenedor correspondiente.
        menu.add(botonInicio);

        // Esta opción solo se muestra al administrador.
        // : AUTORIZACIÓN VISUAL: solo administrador verá la opción Registrar usuario.
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            // : Crea botón de registro solo dentro del bloque ADMINISTRADOR.
            JButton botonUsuarios = new JButton("Registrar usuario");
            // : Incorpora la opción restringida al menú principal.
            menu.add(botonUsuarios);

            // : Registra qué acción debe ejecutarse cuando el usuario pulsa el botón.
            botonUsuarios.addActionListener(
                    // : Muestra en pantalla la ventana del módulo elegido.
                    e -> new RegistroUsuarioVista(this, controlador).setVisible(true));
        }

        // : Añade el elemento a la colección o contenedor correspondiente.
        menu.add(botonCanchas);
        // : Añade el elemento a la colección o contenedor correspondiente.
        menu.add(botonClientes);
        // : Añade el elemento a la colección o contenedor correspondiente.
        menu.add(botonAlquileres);
        // : Añade el elemento a la colección o contenedor correspondiente.
        menu.add(botonReportes);
        // : Añade el elemento a la colección o contenedor correspondiente.
        menu.add(botonCerrar);

        // : Construye un componente de Swing o define su diseño en la interfaz.
        JPanel lateral = new JPanel(new BorderLayout());
        // : Añade el elemento a la colección o contenedor correspondiente.
        lateral.add(menu, BorderLayout.NORTH);
        // : Añade el elemento a la colección o contenedor correspondiente.
        contenedor.add(lateral, BorderLayout.WEST);

        // : Construye un componente de Swing o define su diseño en la interfaz.
        JLabel bienvenida = new JLabel("Seleccione una opción del menú.", SwingConstants.CENTER);
        // : Configura la interfaz, una propiedad de objeto o un parámetro para la operación.
        bienvenida.setFont(new Font("SansSerif", Font.PLAIN, 20));
        // : Añade el elemento a la colección o contenedor correspondiente.
        contenedor.add(bienvenida, BorderLayout.CENTER);

        // : Registra qué acción debe ejecutarse cuando el usuario pulsa el botón.
        botonInicio.addActionListener(e -> bienvenida.setText("Seleccione una opción del menú."));

        // : Registra qué acción debe ejecutarse cuando el usuario pulsa el botón.
        botonCanchas.addActionListener(
                e -> {
                    CanchaControlador canchaControlador =
                            // : El módulo de canchas recibe el DAO y el MISMO controlador de autenticación con la sesión actual.
                            new CanchaControlador(new CanchaDAOImpl(), controlador);

                    CanchaVista ventana =
                            // : Se abre la ventana de canchas y se indica si el usuario es administrador para habilitar los botones CRUD.
                            new CanchaVista(
                                    this, canchaControlador, usuario.getRol() == Rol.ADMINISTRADOR);

                    // : Muestra en pantalla la ventana del módulo elegido.
                    ventana.setVisible(true);
                });

        // : Registra qué acción debe ejecutarse cuando el usuario pulsa el botón.
        botonClientes.addActionListener(
                e -> {
                    ClienteControlador clienteControlador =
                            // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                            new ClienteControlador(new ClienteDAOImpl(), controlador);

                    ClienteVista ventana = new ClienteVista(this, clienteControlador);

                    // : Muestra en pantalla la ventana del módulo elegido.
                    ventana.setVisible(true);
                });

        // : Registra qué acción debe ejecutarse cuando el usuario pulsa el botón.
        botonAlquileres.addActionListener(
                e -> {
                    AlquilerControlador alquilerControlador =
                            // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                            new AlquilerControlador(
                                    // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                                    new AlquilerDAOImpl(),
                                    // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                                    new CanchaDAOImpl(),
                                    // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                                    new ClienteDAOImpl(),
                                    controlador);

                    // : Muestra en pantalla la ventana del módulo elegido.
                    new AlquilerVista(this, alquilerControlador).setVisible(true);
                });

        // : Registra qué acción debe ejecutarse cuando el usuario pulsa el botón.
        botonReportes.addActionListener(
                e -> {
                    AlquilerControlador alquilerControlador =
                            // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                            new AlquilerControlador(
                                    // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                                    new AlquilerDAOImpl(),
                                    // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                                    new CanchaDAOImpl(),
                                    // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                                    new ClienteDAOImpl(),
                                    controlador);

                    // : Muestra en pantalla la ventana del módulo elegido.
                    new ReporteAlquilerVista(this, alquilerControlador).setVisible(true);
                });

        // : El botón cierra la sesión al hacer clic; no solo oculta la ventana.
        botonCerrar.addActionListener(e -> cerrarSesion());

        // : Configura la interfaz, una propiedad de objeto o un parámetro para la operación.
        setContentPane(contenedor);
    }

    /** Pide confirmación, limpia la sesión y vuelve a abrir el login. */
    // : Solicita confirmación, limpia el usuario autenticado, regresa al formulario LoginVista y cierra este JFrame.
    private void cerrarSesion() {
        // : Guarda elección realizada por el usuario en la ventana de confirmación.
        int respuesta =
                JOptionPane.showConfirmDialog(
                        this,
                        "¿Desea cerrar la sesión?",
                        "Cerrar sesión",
                        JOptionPane.YES_NO_OPTION);

        // : Condición: si se cumple, aplica la validación o decisión descrita en el bloque siguiente.
        if (respuesta == JOptionPane.YES_OPTION) {
            // : Limpia usuarioActual en el controlador para que las operaciones siguientes requieran nuevo login.
            controlador.cerrarSesion();

            // : Vuelve a mostrar el formulario de inicio de sesión.
            new LoginVista(controlador).setVisible(true);
            // : Cierra/libera la ventana principal anterior.
            dispose();
        }
    }
}