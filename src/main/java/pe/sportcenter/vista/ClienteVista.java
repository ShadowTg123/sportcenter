package pe.sportcenter.vista;

import pe.sportcenter.controlador.ClienteControlador;
import pe.sportcenter.modelo.Cliente;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ClienteVista extends JDialog {

    private final ClienteControlador controlador;

    private final JTextField campoNombre = new JTextField(25);
    private final JTextField campoDni = new JTextField(12);
    private final JTextField campoTelefono = new JTextField(12);

    private final JButton botonRegistrar = new JButton("Registrar");
    private final JButton botonConsultar = new JButton("Consultar");
    private final JButton botonLimpiar = new JButton("Limpiar");
    private final JButton botonCerrar = new JButton("Cerrar");

    private final JLabel mensaje = new JLabel(" ");

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(
                    new Object[]{"ID", "Nombre", "DNI", "Teléfono"}, 0) {

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private final JTable tabla = new JTable(modeloTabla);

    private boolean ocupado;

    public ClienteVista(
            JFrame propietario,
            ClienteControlador controlador) {

        super(propietario, "Sport Center - Clientes", true);
        this.controlador = controlador;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(850, 500);
        setMinimumSize(new Dimension(700, 400));
        setLocationRelativeTo(propietario);

        JPanel contenido = new JPanel(new BorderLayout(15, 15));
        contenido.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(
                BorderFactory.createTitledBorder("Registrar cliente"));

        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(5, 5, 5, 5);
        restricciones.anchor = GridBagConstraints.WEST;
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        agregarCampo(formulario, restricciones, 0,
                "Nombre completo:", campoNombre);
        agregarCampo(formulario, restricciones, 1,
                "DNI (8 dígitos):", campoDni);
        agregarCampo(formulario, restricciones, 2,
                "Teléfono (opcional, 9 dígitos):", campoTelefono);

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

        botonRegistrar.addActionListener(e -> registrar());
        botonConsultar.addActionListener(e -> consultar());
        botonLimpiar.addActionListener(e -> limpiarFormulario());
        botonCerrar.addActionListener(e -> cerrar());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrar();
            }
        });

        consultar();
    }

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

    private void consultar() {
        if (ocupado) {
            return;
        }

        establecerOcupado(true);
        mensaje.setText("Consultando clientes...");

        // La consulta se ejecuta sin bloquear la interfaz.
        new SwingWorker<List<Cliente>, Void>() {

            @Override
            protected List<Cliente> doInBackground() throws Exception {
                return controlador.listarClientes();
            }

            @Override
            protected void done() {
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

    private void registrar() {
        if (ocupado) {
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
                controlador.registrar(nombre, dni, telefono);
                return null;
            }

            @Override
            protected void done() {
                boolean registrado = false;

                try {
                    get();
                    registrado = true;
                    limpiarFormulario();

                    JOptionPane.showMessageDialog(
                            ClienteVista.this,
                            "Cliente registrado correctamente.",
                            "Registro de cliente",
                            JOptionPane.INFORMATION_MESSAGE
                    );
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
                if (registrado) {
                    consultar();
                }
            }
        }.execute();
    }

    private void mostrarClientes(List<Cliente> clientes) {
        modeloTabla.setRowCount(0);

        for (Cliente cliente : clientes) {
            modeloTabla.addRow(new Object[]{
                    cliente.getIdCliente(),
                    cliente.getNombre(),
                    cliente.getDni(),
                    cliente.getTelefono() == null
                            ? ""
                            : cliente.getTelefono()
            });
        }

        mensaje.setText("Clientes registrados: " + clientes.size());
    }

    private void limpiarFormulario() {
        campoNombre.setText("");
        campoDni.setText("");
        campoTelefono.setText("");
        campoNombre.requestFocusInWindow();
    }

    private void establecerOcupado(boolean valor) {
        ocupado = valor;

        campoNombre.setEnabled(!valor);
        campoDni.setEnabled(!valor);
        campoTelefono.setEnabled(!valor);

        botonRegistrar.setEnabled(!valor);
        botonConsultar.setEnabled(!valor);
        botonLimpiar.setEnabled(!valor);
        botonCerrar.setEnabled(!valor);

        setCursor(Cursor.getPredefinedCursor(
                valor ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void manejarError(Throwable causa) {
        if (causa instanceof IllegalArgumentException
                || causa instanceof SecurityException) {

            mostrarError(causa.getMessage());
            return;
        }

        if (causa instanceof SQLException errorSQL) {
            if (errorSQL.getErrorCode() == 1062) {
                mostrarError("Ya existe un cliente con ese DNI.");
                return;
            }
        }

        mostrarError(
                "No se pudo completar la operación. "
                        + "Revise la conexión con la base de datos.");

        if (causa != null) {
            causa.printStackTrace();
        }
    }

    private void mostrarError(String texto) {
        mensaje.setText(texto);

        JOptionPane.showMessageDialog(
                this,
                texto,
                "Clientes",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void cerrar() {
        if (!ocupado) {
            dispose();
        }
    }
}