
// TEMA: PANTALLA DE CLIENTES: alta y consulta, requisito previo para registrar alquileres


package pe.sportcenter.vista;

import pe.sportcenter.controlador.ClienteControlador;
// IMPORT: Modelo para el cliente que realiza la reserva.
import pe.sportcenter.modelo.Cliente;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Formulario de registro y consulta de clientes. Usa SwingWorker para el acceso a datos y actualiza
 * los controles cuando finaliza cada operación.
 */
// ESTA CLASE: PANTALLA DE CLIENTES: alta y consulta, requisito previo para registrar alquileres.
public class ClienteVista extends JDialog {

    // CAMPO: Referencia al controlador: la vista no ejecuta SQL directamente.
    private final ClienteControlador controlador;

    // CAMPO: Campo de entrada visible de la ventana.
    private final JTextField campoNombre = new JTextField(25);
    // CAMPO: Campo de entrada visible de la ventana.
    private final JTextField campoDni = new JTextField(12);
    // CAMPO: Campo de entrada visible de la ventana.
    private final JTextField campoTelefono = new JTextField(12);

    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonRegistrar = new JButton("Registrar");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonConsultar = new JButton("Consultar");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonLimpiar = new JButton("Limpiar");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonCerrar = new JButton("Cerrar");

    // CAMPO: Etiqueta para mostrar informacion al usuario.
    private final JLabel mensaje = new JLabel(" ");

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(new Object[] {"ID", "Nombre", "DNI", "Teléfono"}, 0) {

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    // PASO (clase): Devuelve el dato calculado o la respuesta de esta operacion.
                    return false;
                }
            };

    // CAMPO: Tabla visual de datos; se alimenta del modelo de tabla.
    private final JTable tabla = new JTable(modeloTabla);

    // CAMPO: Evita iniciar otra tarea mientras hay una consulta/registro en marcha.
    private boolean ocupado;

    // EXPLICACIÓN DEL MÉTODO: Arma el formulario de nombre, DNI y telefono y la tabla de clientes; registra los botones.
    public ClienteVista(JFrame propietario, ClienteControlador controlador) {

        super(propietario, "Sport Center - Clientes", true);
        this.controlador = controlador;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(850, 500);
        setMinimumSize(new Dimension(700, 400));
        setLocationRelativeTo(propietario);

        JPanel contenido = new JPanel(new BorderLayout(15, 15));
        contenido.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Registrar cliente"));

        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(5, 5, 5, 5);
        restricciones.anchor = GridBagConstraints.WEST;
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        agregarCampo(formulario, restricciones, 0, "Nombre completo:", campoNombre);
        agregarCampo(formulario, restricciones, 1, "DNI (8 dígitos):", campoDni);
        agregarCampo(
                formulario, restricciones, 2, "Teléfono (opcional, 9 dígitos):", campoTelefono);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(botonRegistrar);
        botones.add(botonConsultar);
        botones.add(botonLimpiar);
        botones.add(botonCerrar);

        JPanel superior = new JPanel(new BorderLayout());
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        tabla.setRowHeight(26);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);

        contenido.add(superior, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);
        contenido.add(mensaje, BorderLayout.SOUTH);

        setContentPane(contenido);

        // PASO (ClienteVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonRegistrar.addActionListener(e -> registrar());
        // PASO (ClienteVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonConsultar.addActionListener(e -> consultar());
        // PASO (ClienteVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonLimpiar.addActionListener(e -> limpiarFormulario());
        // PASO (ClienteVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonCerrar.addActionListener(e -> cerrar());

        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        cerrar();
                    }
                });

        consultar();
    }

    // EXPLICACIÓN DEL MÉTODO: Construye una fila del formulario con etiqueta y cuadro de texto.
    private void agregarCampo(
            JPanel panel,
            GridBagConstraints restricciones,
            int fila,
            String etiqueta,
            JTextField campo) {

        restricciones.gridy = fila;
        restricciones.gridx = 0;
        restricciones.weightx = 0;
        panel.add(new JLabel(etiqueta), restricciones);

        restricciones.gridx = 1;
        restricciones.weightx = 1;
        panel.add(campo, restricciones);
    }

    /** Obtiene los datos del módulo en segundo plano y actualiza la pantalla. */
    // EXPLICACIÓN DEL MÉTODO: Lee clientes desde el controlador sin bloquear la ventana Swing.
    private void consultar() {
        // PASO (consultar): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (consultar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        establecerOcupado(true);
        mensaje.setText("Consultando clientes...");

        // La consulta se ejecuta sin bloquear la interfaz.
        new SwingWorker<List<Cliente>, Void>() {

            @Override
            protected List<Cliente> doInBackground() throws Exception {
                // PASO (consultar): Devuelve el dato calculado o la respuesta de esta operacion.
                return controlador.listarClientes();
            }

            @Override
            protected void done() {
                // PASO (consultar): Comienza un bloque protegido: permite controlar errores y liberar recursos.
                try {
                    mostrarClientes(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La consulta fue interrumpida.");
                } catch (ExecutionException e) {
                    manejarError(e.getCause());
                } finally {
                    establecerOcupado(false);
                }
            }
        }.execute();
    }

    // EXPLICACIÓN DEL MÉTODO: Obtiene los datos, solicita su alta al controlador y actualiza la lista.
    private void registrar() {
        // PASO (registrar): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (registrar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // Leemos los campos antes de iniciar el trabajo en segundo plano.
        String nombre = campoNombre.getText();
        String dni = campoDni.getText();
        String telefono = campoTelefono.getText();

        establecerOcupado(true);
        mensaje.setText("Registrando cliente...");

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                // PASO (registrar): Pide al controlador que guarde el alquiler; la vista no calcula el precio.
                controlador.registrar(nombre, dni, telefono);
                // PASO (registrar): Devuelve ausencia de valor para el caso indicado.
                return null;
            }

            @Override
            protected void done() {
                boolean registrado = false;

                // PASO (registrar): Comienza un bloque protegido: permite controlar errores y liberar recursos.
                try {
                    get();
                    registrado = true;
                    limpiarFormulario();

                    // PASO (registrar): Muestra una ventana de informacion, confirmacion o advertencia.
                    JOptionPane.showMessageDialog(
                            ClienteVista.this,
                            "Cliente registrado correctamente.",
                            "Registro de cliente",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La operación fue interrumpida.");
                } catch (ExecutionException e) {
                    manejarError(e.getCause());
                } finally {
                    establecerOcupado(false);
                }

                // Consultamos por separado: si falla la actualización
                // de la tabla, el cliente ya quedó registrado.
                // PASO (registrar): Comprueba una condicion de negocio o controla un caso de error.
                if (registrado) {
                    consultar();
                }
            }
        }.execute();
    }

    /** Reemplaza las filas de la tabla con los clientes consultados. */
    // EXPLICACIÓN DEL MÉTODO: Limpia y vuelve a llenar la tabla con los clientes que trajo el DAO.
    private void mostrarClientes(List<Cliente> clientes) {
        // PASO (mostrarClientes): Limpia la tabla para volver a cargar los resultados actuales.
        modeloTabla.setRowCount(0);

        // PASO (mostrarClientes): Recorre los elementos de una lista para agregarlos o compararlos.
        for (Cliente cliente : clientes) {
            // PASO (mostrarClientes): Agrega una fila visible a la tabla de la interfaz.
            modeloTabla.addRow(
                    new Object[] {
                            cliente.getIdCliente(),
                            cliente.getNombre(),
                            cliente.getDni(),
                            cliente.getTelefono() == null ? "" : cliente.getTelefono()
                    });
        }

        mensaje.setText("Clientes registrados: " + clientes.size());
    }

    /** Restablece los campos del formulario para una nueva operación. */
    // EXPLICACIÓN DEL MÉTODO: Limpia los campos despues de registrar exitosamente.
    private void limpiarFormulario() {
        campoNombre.setText("");
        campoDni.setText("");
        campoTelefono.setText("");
        campoNombre.requestFocusInWindow();
    }

    /** Sincroniza los controles con el estado de la tarea para evitar acciones simultáneas. */
    // EXPLICACIÓN DEL MÉTODO: Bloquea temporalmente controles para impedir operaciones repetidas.
    private void establecerOcupado(boolean valor) {
        ocupado = valor;

        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        campoNombre.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        campoDni.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        campoTelefono.setEnabled(!valor);

        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        botonRegistrar.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        botonConsultar.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        botonLimpiar.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        botonCerrar.setEnabled(!valor);

        setCursor(Cursor.getPredefinedCursor(valor ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /** Distingue errores de validación o permisos de los fallos técnicos de la operación. */
    // EXPLICACIÓN DEL MÉTODO: Muestra errores provenientes de validaciones/SQL.
    private void manejarError(Throwable causa) {
        // PASO (manejarError): Comprueba una condicion de negocio o controla un caso de error.
        if (causa instanceof IllegalArgumentException || causa instanceof SecurityException) {

            mostrarError(causa.getMessage());
            // PASO (manejarError): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // PASO (manejarError): Comprueba una condicion de negocio o controla un caso de error.
        if (causa instanceof SQLException errorSQL) {
            // PASO (manejarError): Comprueba una condicion de negocio o controla un caso de error.
            if (errorSQL.getErrorCode() == 1062) {
                mostrarError("Ya existe un cliente con ese DNI.");
                // PASO (manejarError): Devuelve el dato calculado o la respuesta de esta operacion.
                return;
            }
        }

        mostrarError(
                "No se pudo completar la operación. " + "Revise la conexión con la base de datos.");

        // PASO (manejarError): Comprueba una condicion de negocio o controla un caso de error.
        if (causa != null) {
            causa.printStackTrace();
        }
    }

    /** Presenta el mensaje de error al usuario mediante un diálogo. */
    // EXPLICACIÓN DEL MÉTODO: Presenta al usuario un mensaje legible.
    private void mostrarError(String texto) {
        mensaje.setText(texto);

        // PASO (mostrarError): Muestra una ventana de informacion, confirmacion o advertencia.
        JOptionPane.showMessageDialog(this, texto, "Clientes", JOptionPane.WARNING_MESSAGE);
    }

    /** Cierra la ventana cuando no hay una operación en curso. */
    // EXPLICACIÓN DEL MÉTODO: Cierra el cuadro de clientes.
    private void cerrar() {
        // PASO (cerrar): Comprueba una condicion de negocio o controla un caso de error.
        if (!ocupado) {
            dispose();
        }
    }
}
