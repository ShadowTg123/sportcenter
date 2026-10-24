package pe.sportcenter.vista;

import pe.sportcenter.controlador.AlquilerControlador;
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.CanchaResumen;
import pe.sportcenter.modelo.ResumenAlquiler;
import pe.sportcenter.modelo.EstadoAlquiler;
import pe.sportcenter.modelo.IngresoPorDiaSemana;
import pe.sportcenter.modelo.OcupacionCancha;
import pe.sportcenter.modelo.ReporteExportacion;
import pe.sportcenter.util.ExportadorReporteExcel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ReporteAlquilerVista extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private final AlquilerControlador controlador;

    private final JTextField campoDesde = new JTextField(10);
    private final JTextField campoHasta = new JTextField(10);

    // RF10: selector de cancha para el reporte.
    private final JComboBox<OpcionCancha> comboCanchas =
            new JComboBox<>();

    private boolean canchasCargadas;

    // null representa todas las canchas.
    private record OpcionCancha(Integer id, String nombre) {

        @Override
        public String toString() {
            return nombre;
        }
    }

    private final JButton botonExportar =
            new JButton("Exportar a Excel");

    private final JButton botonConsultar =
            new JButton("Consultar");

    private final JButton botonCerrar =
            new JButton("Cerrar");

    private final JLabel mensaje = new JLabel(
            "Cargando canchas...");

    private final JLabel etiquetaIngresos =
            new JLabel("Ingresos del periodo: —");

    private final JLabel etiquetaPromedio =
            new JLabel("Importe promedio por alquiler: —");

    private final JLabel etiquetaCancelaciones =
            new JLabel("Reservas canceladas: —");

    private final JLabel etiquetaCanchaMasAlquilada =
            new JLabel("Cancha más alquilada (todas): —");

    private final JLabel etiquetaDiaMayorIngreso =
            new JLabel("Día de la semana con mayores ingresos: —");

    // Agrupa los resultados obtenidos en segundo plano.
    private record DatosReporte(
            List<ResumenAlquiler> resumenes,
            List<CanchaResumen> canchasMasAlquiladas,
            List<IngresoPorDiaSemana> diasConMayoresIngresos,
            List<OcupacionCancha> ocupaciones) {
    }

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
                public boolean isCellEditable(
                        int fila, int columna) {
                    return false;
                }
            };

    // RF10: tabla de ocupación por cancha (incluye canchas sin alquileres).
    private final DefaultTableModel modeloOcupacion =
            new DefaultTableModel(
                    new Object[]{
                            "ID",
                            "Cancha",
                            "Horas reservadas",
                            "Capacidad teórica (h)",
                            "Ocupación (%)"
                    }, 0) {

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private boolean ocupado;

    private ReporteExportacion ultimoReporte;

    public ReporteAlquilerVista(
            JFrame propietario,
            AlquilerControlador controlador) {

        super(
                propietario,
                "Sport Center - Reporte de alquileres",
                true
        );

        this.controlador = controlador;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1100, 610);
        setMinimumSize(new Dimension(950, 560));
        setLocationRelativeTo(propietario);

        LocalDate hoy =
                LocalDate.now(ZoneId.of("America/Lima"));

        campoDesde.setText(
                hoy.withDayOfMonth(1).format(FORMATO_FECHA));

        campoHasta.setText(
                hoy.withDayOfMonth(hoy.lengthOfMonth())
                        .format(FORMATO_FECHA));

        JPanel contenido =
                new JPanel(new BorderLayout(12, 12));

        contenido.setBorder(
                new EmptyBorder(15, 15, 15, 15));

        // Primera fila: fechas y botones.
        JPanel filtros =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        filtros.add(new JLabel("Desde (dd/MM/aaaa):"));
        filtros.add(campoDesde);

        filtros.add(new JLabel("Hasta (dd/MM/aaaa):"));
        filtros.add(campoHasta);

        filtros.add(botonConsultar);
        filtros.add(botonExportar);
        filtros.add(botonCerrar);

        // Segunda fila: filtro por cancha.
        JPanel filtroCancha =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        filtroCancha.add(new JLabel("Cancha:"));

        comboCanchas.setPreferredSize(
                new Dimension(350, 28));

        filtroCancha.add(comboCanchas);

        JPanel panelFiltros =
                new JPanel(new GridLayout(0, 1));

        panelFiltros.add(filtros);
        panelFiltros.add(filtroCancha);

        JTable tabla = new JTable(modeloTabla);

        tabla.setRowHeight(28);
        tabla.setFillsViewportHeight(true);

        tabla.getTableHeader()
                .setReorderingAllowed(false);

        JPanel inferior =
                new JPanel(new GridLayout(0, 1, 0, 6));

        inferior.add(new JLabel(
                "Importes de alquileres por estado; "
                        + "no representan pagos verificados."));

        inferior.add(new JLabel(
                "Sin alquileres, la cantidad es 0 "
                        + "y las estadísticas se muestran como —."));

        etiquetaIngresos.setFont(
                etiquetaIngresos.getFont().deriveFont(Font.BOLD));

        inferior.add(etiquetaIngresos);
        inferior.add(etiquetaPromedio);
        inferior.add(etiquetaCancelaciones);
        inferior.add(etiquetaCanchaMasAlquilada);
        inferior.add(etiquetaDiaMayorIngreso);
        inferior.add(mensaje);

        contenido.add(
                panelFiltros,
                BorderLayout.NORTH
        );

        // Las dos pestañas comparten el mismo rango de fechas y filtro.
        JTable tablaOcupacion = new JTable(modeloOcupacion);
        tablaOcupacion.setRowHeight(28);
        tablaOcupacion.setFillsViewportHeight(true);
        tablaOcupacion.getTableHeader().setReorderingAllowed(false);

        JPanel panelOcupacion = new JPanel(new BorderLayout(8, 8));
        panelOcupacion.add(
                new JScrollPane(tablaOcupacion),
                BorderLayout.CENTER);

        JTextArea notaOcupacion = new JTextArea(
                "Horas reservadas: alquileres confirmados y completados.\n"
                        + "Capacidad teórica: 15 horas por día y por cancha. "
                        + "No descuenta cierres por mantenimiento ni inactividad.\n"
                        + "La ocupación representa horarios reservados, "
                        + "no asistencia comprobada."
        );
        notaOcupacion.setEditable(false);
        notaOcupacion.setLineWrap(true);
        notaOcupacion.setWrapStyleWord(true);
        notaOcupacion.setRows(3);
        notaOcupacion.setFont(tablaOcupacion.getFont());
        notaOcupacion.setBackground(panelOcupacion.getBackground());
        panelOcupacion.add(notaOcupacion, BorderLayout.SOUTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Resumen e ingresos", new JScrollPane(tabla));
        pestanas.addTab("Ocupación por cancha", panelOcupacion);

        contenido.add(pestanas, BorderLayout.CENTER);

        contenido.add(
                inferior,
                BorderLayout.SOUTH
        );

        setContentPane(contenido);

        botonConsultar.addActionListener(
                e -> consultar());

        botonExportar.addActionListener(e -> exportarExcel());

        botonCerrar.addActionListener(
                e -> cerrar());

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                cerrar();
            }
        });

        // RF10: cargar las canchas al abrir el reporte.
        cargarCanchas();
    }

    // RF10: obtiene las canchas para el selector.
    private void cargarCanchas() {

        canchasCargadas = false;

        establecerOcupado(true);

        mensaje.setText("Cargando canchas...");

        new SwingWorker<List<Cancha>, Void>() {

            @Override
            protected List<Cancha> doInBackground()
                    throws Exception {

                return controlador.listarCanchasParaReporte();
            }

            @Override
            protected void done() {

                try {
                    List<Cancha> canchas = get();

                    comboCanchas.removeAllItems();

                    comboCanchas.addItem(
                            new OpcionCancha(
                                    null,
                                    "Todas las canchas"
                            )
                    );

                    for (Cancha cancha : canchas) {

                        comboCanchas.addItem(
                                new OpcionCancha(
                                        cancha.getIdCancha(),
                                        cancha.getNombre()
                                                + " | ID: "
                                                + cancha.getIdCancha()
                                                + " | "
                                                + cancha.getEstado()
                                                .valorBD()
                                )
                        );
                    }

                    comboCanchas.setSelectedIndex(0);

                    canchasCargadas = true;

                    mensaje.setText(
                            "Seleccione las fechas y la cancha; "
                                    + "después pulse Consultar.");

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    mostrarError(
                            "La carga de canchas fue interrumpida.");

                } catch (ExecutionException e) {

                    Throwable causa = e.getCause();

                    if (causa instanceof IllegalArgumentException
                            || causa instanceof SecurityException) {

                        mostrarError(causa.getMessage());

                    } else {

                        mostrarError(
                                "No se pudieron cargar las canchas. "
                                        + "Cierre y vuelva a abrir el reporte.");

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

    private void consultar() {

        if (ocupado || !canchasCargadas) {
            return;
        }

        // Captura la cancha seleccionada antes de consultar.
        final OpcionCancha seleccion =
                (OpcionCancha) comboCanchas.getSelectedItem();

        if (seleccion == null) {

            mostrarError(
                    "Seleccione una opción de cancha.");

            return;
        }

        final Integer idCancha = seleccion.id();
        final String nombreFiltro = seleccion.nombre();

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

        // Limpia los resultados de la consulta anterior.
        ultimoReporte = null;
        modeloTabla.setRowCount(0);
        modeloOcupacion.setRowCount(0);
        etiquetaIngresos.setText("Ingresos del periodo: —");
        etiquetaPromedio.setText("Importe promedio por alquiler: —");
        etiquetaCancelaciones.setText("Reservas canceladas: —");
        etiquetaCanchaMasAlquilada.setText(
                "Cancha más alquilada (todas): —");
        etiquetaCanchaMasAlquilada.setToolTipText(null);
        etiquetaDiaMayorIngreso.setText(
                "Día de la semana con mayores ingresos: —");
        etiquetaDiaMayorIngreso.setToolTipText(null);

        mensaje.setText("Consultando reporte...");

        new SwingWorker<DatosReporte, Void>() {

            @Override
            protected DatosReporte doInBackground()
                    throws Exception {

                List<ResumenAlquiler> resumenes =
                        controlador.resumirPorFechas(desde, hasta, idCancha);

                List<CanchaResumen> masAlquiladas =
                        controlador.obtenerCanchasMasAlquiladas(desde, hasta);

                List<IngresoPorDiaSemana> dias =
                        controlador.obtenerDiasConMayoresIngresos(
                                desde, hasta, idCancha);

                List<OcupacionCancha> ocupaciones =
                        controlador.consultarOcupacion(
                                desde, hasta, idCancha);

                return new DatosReporte(
                        resumenes, masAlquiladas, dias, ocupaciones);
            }

            @Override
            protected void done() {

                try {

                    DatosReporte datos = get();

                    for (OcupacionCancha ocupacion : datos.ocupaciones()) {
                        modeloOcupacion.addRow(new Object[]{
                                ocupacion.idCancha(),
                                ocupacion.nombre(),
                                ocupacion.horasReservadas().toPlainString(),
                                ocupacion.horasCapacidadTeorica().toPlainString(),
                                ocupacion.porcentajeOcupacion().toPlainString()
                        });
                    }

                    List<ResumenAlquiler> resumenes = datos.resumenes();

                    long cantidadTotal = 0;
                    BigDecimal ingresos = new BigDecimal("0.00");
                    long cantidadConIngreso = 0;
                    long cantidadCancelados = 0;

                    for (ResumenAlquiler resumen : resumenes) {

                        boolean tieneDatos =
                                resumen.getCantidad() > 0;

                        modeloTabla.addRow(new Object[]{

                                resumen.getEstado(),

                                resumen.getCantidad(),

                                resumen.getMontoTotal()
                                        .toPlainString(),

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

                        if (resumen.getEstado() == EstadoAlquiler.CONFIRMADO
                                || resumen.getEstado() == EstadoAlquiler.COMPLETADO) {
                            ingresos = ingresos.add(resumen.getMontoTotal());
                            cantidadConIngreso += resumen.getCantidad();
                        } else if (resumen.getEstado() == EstadoAlquiler.CANCELADO) {
                            cantidadCancelados += resumen.getCantidad();
                        }

                        cantidadTotal += resumen.getCantidad();
                    }

                    if (cantidadConIngreso > 0) {
                        BigDecimal promedio = ingresos.divide(
                                BigDecimal.valueOf(cantidadConIngreso),
                                2, RoundingMode.HALF_UP);
                        etiquetaPromedio.setText(
                                "Importe promedio por alquiler: S/ "
                                        + promedio.toPlainString()
                                        + " (confirmados y completados)");
                    } else {
                        etiquetaPromedio.setText(
                                "Importe promedio por alquiler: — "
                                        + "(sin confirmados ni completados)");
                    }

                    if (cantidadTotal > 0) {
                        BigDecimal porcentajeCancelados =
                                BigDecimal.valueOf(cantidadCancelados)
                                        .multiply(BigDecimal.valueOf(100))
                                        .divide(BigDecimal.valueOf(cantidadTotal),
                                                2, RoundingMode.HALF_UP);
                        etiquetaCancelaciones.setText(
                                "Reservas canceladas: "
                                        + porcentajeCancelados.toPlainString()
                                        + " % (" + cantidadCancelados
                                        + " de " + cantidadTotal + ")");
                    } else {
                        etiquetaCancelaciones.setText(
                                "Reservas canceladas: — (sin alquileres)");
                    }

                    etiquetaIngresos.setText(
                            "Ingresos del periodo: S/ " + ingresos.toPlainString()
                                    + " (confirmados + completados; "
                                    + "no son cobros verificados)");

                    List<CanchaResumen> masAlquiladas =
                            datos.canchasMasAlquiladas();

                    if (masAlquiladas.isEmpty()) {
                        etiquetaCanchaMasAlquilada.setText(
                                "Cancha más alquilada (todas): — "
                                        + "(sin confirmados ni completados)");
                    } else {
                        String nombres = masAlquiladas.stream()
                                .map(resumen ->
                                        resumen.getCancha().getNombre()
                                                + " [ID "
                                                + resumen.getCancha().getIdCancha()
                                                + "]")
                                .collect(Collectors.joining(", "));

                        long cantidad =
                                masAlquiladas.get(0).getCantidadAlquileres();

                        String titulo = masAlquiladas.size() == 1
                                ? "Cancha más alquilada (todas): "
                                : "Canchas más alquiladas (empate; todas): ";

                        etiquetaCanchaMasAlquilada.setText(
                                titulo + nombres + " | " + cantidad
                                        + " alquiler(es) por cancha");
                    }
                    etiquetaCanchaMasAlquilada.setToolTipText(
                            etiquetaCanchaMasAlquilada.getText());

                    List<IngresoPorDiaSemana> dias =
                            datos.diasConMayoresIngresos();

                    if (dias.isEmpty()) {
                        etiquetaDiaMayorIngreso.setText(
                                "Día de la semana con mayores ingresos: — "
                                        + "(sin confirmados ni completados)");
                    } else {
                        String nombresDias = dias.stream()
                                .map(ingreso -> ingreso.dia().getDisplayName(
                                        TextStyle.FULL,
                                        Locale.forLanguageTag("es-PE")))
                                .collect(Collectors.joining(", "));

                        BigDecimal monto = dias.get(0).montoTotal();

                        String titulo = dias.size() == 1
                                ? "Día de la semana con mayores ingresos: "
                                : "Días con mayores ingresos (empate): ";

                        etiquetaDiaMayorIngreso.setText(
                                titulo + nombresDias
                                        + " | S/ " + monto.toPlainString()
                                        + " acumulados por día de la semana");
                    }
                    etiquetaDiaMayorIngreso.setToolTipText(
                            etiquetaDiaMayorIngreso.getText());

                    mensaje.setText(
                            "Periodo: "
                                    + desde.format(FORMATO_FECHA)
                                    + " al "
                                    + hasta.format(FORMATO_FECHA)
                                    + " | Cancha: "
                                    + nombreFiltro
                                    + " | Alquileres: "
                                    + cantidadTotal
                    );

                    ultimoReporte = new ReporteExportacion(
                            desde,
                            hasta,
                            nombreFiltro,
                            datos.resumenes(),
                            datos.canchasMasAlquiladas(),
                            datos.diasConMayoresIngresos(),
                            datos.ocupaciones()
                    );

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    mostrarError(
                            "La consulta fue interrumpida.");

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

    private void exportarExcel() {
        if (ocupado || ultimoReporte == null) {
            return;
        }

        // Conserva los datos y filtros de la última consulta exitosa.
        final ReporteExportacion reporte = ultimoReporte;

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar reporte de SportCenter");
        selector.setAcceptAllFileFilterUsed(false);
        selector.setFileFilter(
                new FileNameExtensionFilter(
                        "Archivo Excel (*.xlsx)", "xlsx"));

        selector.setSelectedFile(new File(
                "SportCenter_"
                        + reporte.desde()
                        + "_"
                        + reporte.hasta()
                        + ".xlsx"));

        if (selector.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File elegido = selector.getSelectedFile();

        if (!elegido.getName().toLowerCase(Locale.ROOT)
                .endsWith(".xlsx")) {
            elegido = new File(
                    elegido.getParentFile(),
                    elegido.getName() + ".xlsx");
        }

        final Path destino = elegido.toPath().toAbsolutePath();

        if (Files.isDirectory(destino)) {
            mostrarError("Seleccione un archivo, no una carpeta.");
            return;
        }

        final boolean reemplazar = Files.exists(destino);

        if (reemplazar) {
            int respuesta = JOptionPane.showConfirmDialog(
                    this,
                    "El archivo ya existe:\n"
                            + destino
                            + "\n\n¿Desea reemplazarlo?",
                    "Confirmar reemplazo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
        }

        establecerOcupado(true);
        mensaje.setText("Exportando reporte a Excel...");

        new SwingWorker<Path, Void>() {

            @Override
            protected Path doInBackground() throws Exception {
                byte[] contenido =
                        ExportadorReporteExcel.generar(reporte);

                // Escribe primero en un archivo temporal.
                // Así no vaciamos el archivo anterior si falla la generación.
                Path temporal = Files.createTempFile(
                        destino.getParent(),
                        "sportcenter-",
                        ".tmp");

                try {
                    Files.write(temporal, contenido);

                    if (reemplazar) {
                        Files.move(
                                temporal,
                                destino,
                                StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        // Si alguien creó el destino entretanto,
                        // falla en lugar de reemplazarlo sin autorización.
                        Files.move(temporal, destino);
                    }

                    return destino;

                } finally {
                    Files.deleteIfExists(temporal);
                }
            }

            @Override
            protected void done() {
                try {
                    Path guardado = get();

                    mensaje.setText(
                            "Reporte exportado: " + guardado);

                    JOptionPane.showMessageDialog(
                            ReporteAlquilerVista.this,
                            "Reporte exportado correctamente.\n\n"
                                    + guardado
                                    + "\n\nPeriodo: "
                                    + reporte.desde().format(FORMATO_FECHA)
                                    + " al "
                                    + reporte.hasta().format(FORMATO_FECHA)
                                    + "\nCancha: "
                                    + reporte.nombreFiltro(),
                            "Exportación completada",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La exportación fue interrumpida.");

                } catch (ExecutionException e) {
                    mostrarError(
                            "No se pudo exportar el reporte. "
                                    + "Compruebe los permisos de la carpeta "
                                    + "y cierre el archivo si está abierto en Excel.");

                    if (e.getCause() != null) {
                        e.getCause().printStackTrace();
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

        if (!texto.matches(
                "[0-9]{2}/[0-9]{2}/[0-9]{4}")) {

            throw new IllegalArgumentException(
                    "Ingrese la fecha " + nombre
                            + " con formato dd/MM/aaaa.");
        }

        try {

            return LocalDate.parse(
                    texto,
                    FORMATO_FECHA
            );

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException(
                    "La fecha " + nombre + " no es válida.");
        }
    }

    private void establecerOcupado(boolean valor) {

        ocupado = valor;

        campoDesde.setEnabled(!valor);
        campoHasta.setEnabled(!valor);

        comboCanchas.setEnabled(
                !valor && canchasCargadas
        );

        botonExportar.setEnabled(!valor && ultimoReporte != null);

        botonConsultar.setEnabled(
                !valor && canchasCargadas
        );


        botonCerrar.setEnabled(!valor);

        setCursor(
                Cursor.getPredefinedCursor(
                        valor
                                ? Cursor.WAIT_CURSOR
                                : Cursor.DEFAULT_CURSOR
                )
        );
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