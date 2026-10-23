package pe.sportcenter.vista;

import pe.sportcenter.controlador.AlquilerControlador;
import pe.sportcenter.modelo.ResumenAlquiler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ReporteAlquilerVista extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private final AlquilerControlador controlador;

    private final JTextField campoDesde = new JTextField(10);
    private final JTextField campoHasta = new JTextField(10);

    private final JButton botonConsultar = new JButton("Consultar");
    private final JButton botonCerrar = new JButton("Cerrar");

    private final JLabel mensaje = new JLabel(
            "Ingrese un rango de fechas y pulse Consultar.");

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(
                    new Object[]{
                            "Estado",
                            "Cantidad",
                            "Total S/",
                            "Promedio S/",
                            "Mínimo S/",
                            "Máximo S/"
                    }, 0) {

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private boolean ocupado;

    public ReporteAlquilerVista(
            JFrame propietario,
            AlquilerControlador controlador) {

        super(propietario, "Sport Center - Reporte de alquileres", true);
        this.controlador = controlador;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(950, 430);
        setMinimumSize(new Dimension(850, 350));
        setLocationRelativeTo(propietario);

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));

        campoDesde.setText(
                hoy.withDayOfMonth(1).format(FORMATO_FECHA));

        campoHasta.setText(
                hoy.withDayOfMonth(hoy.lengthOfMonth())
                        .format(FORMATO_FECHA));

        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        contenido.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filtros.add(new JLabel("Desde (dd/MM/aaaa):"));
        filtros.add(campoDesde);

        filtros.add(new JLabel("Hasta (dd/MM/aaaa):"));
        filtros.add(campoHasta);

        filtros.add(botonConsultar);
        filtros.add(botonCerrar);

        JTable tabla = new JTable(modeloTabla);
        tabla.setRowHeight(28);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);

        JPanel inferior = new JPanel(new GridLayout(0, 1, 0, 6));

        inferior.add(new JLabel(
                "Importes de alquileres por estado; "
                        + "no representan pagos verificados."));

        inferior.add(new JLabel(
                "Sin alquileres, la cantidad es 0 "
                        + "y las estadísticas se muestran como —."));

        inferior.add(mensaje);

        contenido.add(filtros, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);
        contenido.add(inferior, BorderLayout.SOUTH);

        setContentPane(contenido);

        botonConsultar.addActionListener(e -> consultar());
        botonCerrar.addActionListener(e -> cerrar());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrar();
            }
        });
    }

    private void consultar() {
        if (ocupado) {
            return;
        }

        final LocalDate desde;
        final LocalDate hasta;

        try {
            desde = leerFecha(campoDesde, "inicial");
            hasta = leerFecha(campoHasta, "final");

            if (desde.isAfter(hasta)) {
                throw new IllegalArgumentException(
                        "La fecha inicial no puede ser posterior a la final.");
            }
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
            return;
        }

        establecerOcupado(true);

        // Quitamos los resultados anteriores para evitar confusiones
        // si la nueva consulta falla.
        modeloTabla.setRowCount(0);
        mensaje.setText("Consultando reporte...");

        new SwingWorker<List<ResumenAlquiler>, Void>() {

            @Override
            protected List<ResumenAlquiler> doInBackground()
                    throws Exception {

                return controlador.resumirPorFechas(desde, hasta);
            }

            @Override
            protected void done() {
                try {
                    List<ResumenAlquiler> resumenes = get();
                    long cantidadTotal = 0;

                    for (ResumenAlquiler resumen : resumenes) {
                        boolean tieneDatos = resumen.getCantidad() > 0;

                        modeloTabla.addRow(new Object[]{
                                resumen.getEstado(),
                                resumen.getCantidad(),
                                resumen.getMontoTotal().toPlainString(),
                                tieneDatos
                                        ? resumen.getMontoPromedio()
                                        .toPlainString()
                                        : "—",
                                tieneDatos
                                        ? resumen.getMontoMinimo()
                                        .toPlainString()
                                        : "—",
                                tieneDatos
                                        ? resumen.getMontoMaximo()
                                        .toPlainString()
                                        : "—"
                        });

                        cantidadTotal += resumen.getCantidad();
                    }

                    mensaje.setText(
                            "Periodo consultado: "
                                    + desde.format(FORMATO_FECHA)
                                    + " al "
                                    + hasta.format(FORMATO_FECHA)
                                    + " | Total de alquileres: "
                                    + cantidadTotal
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La consulta fue interrumpida.");

                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();

                    if (causa instanceof IllegalArgumentException
                            || causa instanceof SecurityException) {

                        mostrarError(causa.getMessage());
                    } else {
                        mostrarError(
                                "No se pudo consultar el reporte. "
                                        + "Revise la consola de IntelliJ.");

                        if (causa != null) {
                            causa.printStackTrace();
                        }
                    }

                } finally {
                    establecerOcupado(false);
                }
            }
        }.execute();
    }

    private LocalDate leerFecha(
            JTextField campo,
            String nombre) {

        String texto = campo.getText().strip();

        if (!texto.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}")) {
            throw new IllegalArgumentException(
                    "Ingrese la fecha " + nombre
                            + " con formato dd/MM/aaaa.");
        }

        try {
            return LocalDate.parse(texto, FORMATO_FECHA);

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "La fecha " + nombre + " no es válida.");
        }
    }

    private void establecerOcupado(boolean valor) {
        ocupado = valor;

        campoDesde.setEnabled(!valor);
        campoHasta.setEnabled(!valor);
        botonConsultar.setEnabled(!valor);
        botonCerrar.setEnabled(!valor);

        setCursor(Cursor.getPredefinedCursor(
                valor ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void mostrarError(String texto) {
        mensaje.setText(texto);

        JOptionPane.showMessageDialog(
                this,
                texto,
                "Reporte de alquileres",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void cerrar() {
        if (!ocupado) {
            dispose();
        }
    }
}