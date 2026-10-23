package pe.sportcenter.vista;

import pe.sportcenter.controlador.CanchaControlador;
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.DeporteCancha;
import pe.sportcenter.modelo.EstadoCancha;
import pe.sportcenter.modelo.CanchaResumen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class CanchaVista extends JDialog {

    private final CanchaControlador controlador;
    private final boolean administrador;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Deporte", "Tarifa S/", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tabla = new JTable(modeloTabla);

    private final JTextField campoNombre = new JTextField();
    private final JTextField campoTarifa = new JTextField();
    private final JComboBox<DeporteCancha> campoDeporte =
            new JComboBox<>(DeporteCancha.values());
    private final JComboBox<EstadoCancha> campoEstado =
            new JComboBox<>(EstadoCancha.values());

    private final JButton botonAdicionar = new JButton("Adicionar");
    private final JButton botonModificar = new JButton("Modificar");
    private final JButton botonGuardar = new JButton("Guardar");
    private final JButton botonEliminar = new JButton("Eliminar");
    private final JButton botonConsultar = new JButton("Consultar");
    private final JButton botonResumen =
            new JButton("Alquileres por cancha");

    private final JLabel mensaje = new JLabel(" ");

    private List<Cancha> canchas = List.of();
    private boolean ocupado;
    private boolean editando;
    private int idEdicion;

    public CanchaVista(
            JFrame propietario,
            CanchaControlador controlador,
            boolean administrador) {

        super(propietario, "Sport Center - Canchas", true);
        this.controlador = controlador;
        this.administrador = administrador;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1000, 550);
        setMinimumSize(new Dimension(850, 450));
        setLocationRelativeTo(propietario);

        JPanel contenido = new JPanel(new BorderLayout(15, 15));
        contenido.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));

        if (administrador) {
            barra.add(botonAdicionar);
            barra.add(botonModificar);
            barra.add(botonGuardar);
            barra.add(botonEliminar);
        }

        barra.add(botonConsultar);
        barra.add(botonResumen);
        contenido.add(barra, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(26);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel detalle = new JPanel(new GridLayout(0, 1, 6, 6));
        detalle.setBorder(BorderFactory.createTitledBorder("Detalle de cancha"));
        detalle.setPreferredSize(new Dimension(240, 300));

        detalle.add(new JLabel("Nombre:"));
        detalle.add(campoNombre);
        detalle.add(new JLabel("Deporte:"));
        detalle.add(campoDeporte);
        detalle.add(new JLabel("Tarifa por hora (ejemplo: 50.00):"));
        detalle.add(campoTarifa);
        detalle.add(new JLabel("Estado:"));
        detalle.add(campoEstado);

        JPanel lateral = new JPanel(new BorderLayout());
        lateral.add(detalle, BorderLayout.NORTH);

        contenido.add(lateral, BorderLayout.EAST);
        contenido.add(mensaje, BorderLayout.SOUTH);
        setContentPane(contenido);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !ocupado && !editando) {
                mostrarSeleccion();
            }
        });

        botonAdicionar.addActionListener(e -> prepararRegistro());
        botonModificar.addActionListener(e -> prepararModificacion());
        botonGuardar.addActionListener(e -> guardar());
        botonEliminar.addActionListener(e -> eliminar());
        botonConsultar.addActionListener(e -> consultar());
        botonResumen.addActionListener(e -> consultarResumenAlquileres());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!ocupado) {
                    dispose();
                }
            }
        });

        actualizarControles();
        consultar();
    }

    private Cancha canchaSeleccionada() {
        int fila = tabla.getSelectedRow();

        if (fila < 0 || fila >= canchas.size()) {
            throw new IllegalArgumentException(
                    "Seleccione una cancha de la tabla.");
        }

        return canchas.get(fila);
    }

    private void mostrarSeleccion() {
        if (tabla.getSelectedRow() < 0) {
            return;
        }

        Cancha cancha = canchaSeleccionada();

        campoNombre.setText(cancha.getNombre());
        campoDeporte.setSelectedItem(cancha.getDeporte());
        campoTarifa.setText(cancha.getTarifaHora().toPlainString());
        campoEstado.setSelectedItem(cancha.getEstado());
    }

    private void prepararRegistro() {
        if (!administrador || ocupado) {
            return;
        }

        tabla.clearSelection();
        limpiarFormulario();

        idEdicion = 0;
        editando = true;
        actualizarControles();

        mensaje.setText("Ingrese los datos y pulse Guardar.");
        campoNombre.requestFocusInWindow();
    }

    private void prepararModificacion() {
        try {
            Cancha cancha = canchaSeleccionada();

            mostrarSeleccion();
            idEdicion = cancha.getIdCancha();
            editando = true;
            actualizarControles();

            mensaje.setText("Modifique los datos y pulse Guardar.");

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void guardar() {
        if (!editando || ocupado) {
            return;
        }

        try {
            String nombre = campoNombre.getText();
            DeporteCancha deporte =
                    (DeporteCancha) campoDeporte.getSelectedItem();
            EstadoCancha estado =
                    (EstadoCancha) campoEstado.getSelectedItem();

            BigDecimal tarifa =
                    new BigDecimal(campoTarifa.getText().strip());

            int id = idEdicion;

            ejecutar(() -> {
                if (id == 0) {
                    controlador.registrar(nombre, deporte, tarifa);
                } else {
                    controlador.actualizar(id, nombre, deporte, tarifa, estado);
                }

                return controlador.listarCanchas();
            }, "Datos guardados correctamente.");

        } catch (NumberFormatException e) {
            mostrarError(
                    "Ingrese una tarifa numérica usando punto decimal. "
                            + "Ejemplo: 50.00");
        }
    }

    private void eliminar() {
        try {
            Cancha cancha = canchaSeleccionada();

            Object[] opciones = {
                    "Eliminación lógica",
                    "Eliminación física",
                    "Cancelar"
            };

            int opcion = JOptionPane.showOptionDialog(
                    this,
                    "Cancha: " + cancha.getNombre()
                            + "\nLógica: conserva el registro como inactivo."
                            + "\nFísica: borra definitivamente el registro"
                            + "\nsi no tiene alquileres asociados.",
                    "Confirmar eliminación",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    opciones,
                    opciones[2]
            );

            if (opcion != 0 && opcion != 1) {
                return;
            }

            ejecutar(() -> {
                boolean realizada;

                if (opcion == 0) {
                    realizada = controlador.eliminarLogicamente(
                            cancha.getIdCancha());
                } else {
                    realizada = controlador.eliminarFisicamente(
                            cancha.getIdCancha());
                }

                if (!realizada) {
                    throw new IllegalArgumentException(
                            opcion == 0
                                    ? "La cancha ya está inactiva o no existe."
                                    : "No se pudo eliminar: la cancha tiene "
                                      + "alquileres asociados o ya no existe."
                    );
                }

                return controlador.listarCanchas();
            }, "Eliminación realizada correctamente.");

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void consultar() {
        if (ocupado) {
            return;
        }

        if (editando) {
            int respuesta = JOptionPane.showConfirmDialog(
                    this,
                    "¿Descartar los cambios del formulario y consultar?",
                    "Consultar canchas",
                    JOptionPane.YES_NO_OPTION
            );

            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
        }

        ejecutar(controlador::listarCanchas, "Consulta actualizada.");
    }

    private void ejecutar(
            Callable<List<Cancha>> operacion,
            String mensajeExito) {

        ocupado = true;
        actualizarControles();
        mensaje.setText("Procesando...");

        SwingWorker<List<Cancha>, Void> tarea = new SwingWorker<>() {

            @Override
            protected List<Cancha> doInBackground() throws Exception {
                return operacion.call();
            }

            @Override
            protected void done() {
                try {
                    canchas = get();

                    editando = false;
                    idEdicion = 0;
                    modeloTabla.setRowCount(0);

                    for (Cancha cancha : canchas) {
                        modeloTabla.addRow(new Object[]{
                                cancha.getIdCancha(),
                                cancha.getNombre(),
                                cancha.getDeporte().valorBD(),
                                cancha.getTarifaHora().toPlainString(),
                                cancha.getEstado().valorBD()
                        });
                    }

                    limpiarFormulario();
                    mensaje.setText(
                            mensajeExito + " Registros: " + canchas.size());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La operación fue interrumpida.");

                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();

                    if (causa instanceof IllegalArgumentException
                            || causa instanceof SecurityException) {
                        mostrarError(causa.getMessage());
                    } else {
                        mostrarError(
                                "No se pudo completar o confirmar la operación."
                                        + "\nConsulte los datos antes de volver a intentarlo."
                        );
                        causa.printStackTrace();
                    }

                    mensaje.setText("No se pudo completar la operación.");

                } finally {
                    ocupado = false;
                    actualizarControles();
                }
            }
        };

        tarea.execute();
    }

    private void consultarResumenAlquileres() {
        if (ocupado || editando) {
            return;
        }

        ocupado = true;
        actualizarControles();
        mensaje.setText("Consultando alquileres por cancha...");

        SwingWorker<List<CanchaResumen>, Void> tarea = new SwingWorker<>() {

            @Override
            protected List<CanchaResumen> doInBackground() throws Exception {
                return controlador.listarResumenAlquileres();
            }

            @Override
            protected void done() {
                try {
                    List<CanchaResumen> resumenes = get();

                    DefaultTableModel modeloResumen = new DefaultTableModel(
                            new Object[]{
                                    "ID", "Cancha", "Deporte",
                                    "Estado", "Total de alquileres"
                            }, 0) {

                        @Override
                        public boolean isCellEditable(int fila, int columna) {
                            return false;
                        }
                    };

                    for (CanchaResumen resumen : resumenes) {
                        Cancha cancha = resumen.getCancha();

                        modeloResumen.addRow(new Object[]{
                                cancha.getIdCancha(),
                                cancha.getNombre(),
                                cancha.getDeporte().valorBD(),
                                cancha.getEstado().valorBD(),
                                resumen.getCantidadAlquileres()
                        });
                    }

                    JTable tablaResumen = new JTable(modeloResumen);
                    tablaResumen.setRowHeight(26);
                    tablaResumen.setFillsViewportHeight(true);

                    JDialog ventana = new JDialog(
                            CanchaVista.this,
                            "Sport Center - Alquileres por cancha",
                            true
                    );

                    ventana.setDefaultCloseOperation(
                            WindowConstants.DISPOSE_ON_CLOSE);

                    JPanel contenido = new JPanel(new BorderLayout(10, 10));
                    contenido.setBorder(new EmptyBorder(15, 15, 15, 15));

                    contenido.add(
                            new JLabel(
                                    "Total histórico: incluye alquileres "
                                            + "confirmados, cancelados y completados."),
                            BorderLayout.NORTH
                    );

                    contenido.add(
                            new JScrollPane(tablaResumen),
                            BorderLayout.CENTER
                    );

                    JButton botonCerrar = new JButton("Cerrar");
                    botonCerrar.addActionListener(e -> ventana.dispose());

                    JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                    pie.add(botonCerrar);
                    contenido.add(pie, BorderLayout.SOUTH);

                    ventana.setContentPane(contenido);
                    ventana.setSize(750, 400);
                    ventana.setLocationRelativeTo(CanchaVista.this);

                    mensaje.setText(
                            "Resumen consultado. Canchas: " + resumenes.size());

                    ventana.setVisible(true);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mensaje.setText("Consulta interrumpida.");
                    mostrarError("La consulta fue interrumpida.");

                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();

                    mensaje.setText("No se pudo consultar el resumen.");

                    if (causa instanceof SecurityException
                            || causa instanceof IllegalArgumentException) {
                        mostrarError(causa.getMessage());
                    } else {
                        mostrarError(
                                "No se pudo consultar el resumen de alquileres.");
                        causa.printStackTrace();
                    }

                } finally {
                    ocupado = false;
                    actualizarControles();
                }
            }
        };

        tarea.execute();
    }

    private void limpiarFormulario() {
        campoNombre.setText("");
        campoTarifa.setText("");
        campoDeporte.setSelectedItem(DeporteCancha.FUTBOL);
        campoEstado.setSelectedItem(EstadoCancha.DISPONIBLE);
    }

    private void actualizarControles() {
        boolean puedeEditar = administrador && editando && !ocupado;

        campoNombre.setEnabled(puedeEditar);
        campoTarifa.setEnabled(puedeEditar);
        campoDeporte.setEnabled(puedeEditar);
        campoEstado.setEnabled(puedeEditar && idEdicion > 0);

        tabla.setEnabled(!ocupado && !editando);

        botonAdicionar.setEnabled(administrador && !ocupado && !editando);
        botonModificar.setEnabled(administrador && !ocupado && !editando);
        botonGuardar.setEnabled(puedeEditar);
        botonEliminar.setEnabled(administrador && !ocupado && !editando);
        botonConsultar.setEnabled(!ocupado);
        botonResumen.setEnabled(!ocupado && !editando);
    }

    private void mostrarError(String texto) {
        JOptionPane.showMessageDialog(
                this,
                texto,
                "Gestión de canchas",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
