package pe.sportcenter.vista;

import pe.sportcenter.controlador.AlquilerControlador;
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.CanchaResumen;
import pe.sportcenter.modelo.EstadoAlquiler;
import pe.sportcenter.modelo.IngresoPorDiaSemana;
import pe.sportcenter.modelo.OcupacionCancha;
import pe.sportcenter.modelo.ReporteExportacion;
import pe.sportcenter.modelo.ResumenAlquiler;
import pe.sportcenter.util.ExportadorReporteExcel;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

//  Esta ventana administra consultas y exportaciones de reportes sin mezclar SQL en Swing.
/**
 * Consulta indicadores y ocupación por periodo y permite exportar los resultados. La exportación
 * usa la última consulta exitosa aunque después se editen los filtros. El indicador de canchas más
 * alquiladas compara todas las canchas del periodo.
 */
public class ReporteAlquilerVista extends JDialog {

    //  Formato riguroso dd/MM/aaaa para que una fecha invalida no se interprete silenciosamente.
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    //  Controlador que autoriza el acceso y solicita las consultas de informacion a los DAO.
    private final AlquilerControlador controlador;

    //  Campo para la fecha inicial del reporte.
    private final JTextField campoDesde = new JTextField(10);
    //  Campo para la fecha final del reporte.
    private final JTextField campoHasta = new JTextField(10);

    // RF10: selector de cancha para el reporte.
    //  Selector opcional de cancha; un identificador null significa consultar todas las canchas.
    private final JComboBox<OpcionCancha> comboCanchas = new JComboBox<>();

    //  Bandera que evita consultas antes de terminar de cargar las canchas del selector.
    private boolean canchasCargadas;

    // null representa todas las canchas.
    //  Pequeno record que guarda ID y nombre de la opcion elegida en la lista.
    private record OpcionCancha(Integer id, String nombre) {

        @Override
        //  Swing usa toString para mostrar una etiqueta legible en el desplegable.
        public String toString() {
            return nombre;
        }
    }

    //  Boton de salida XLSX, habilitado solo despues de una consulta exitosa.
    private final JButton botonExportar = new JButton("Exportar a Excel");

    //  Boton que ejecuta las consultas de ingresos, indicadores y ocupacion.
    private final JButton botonConsultar = new JButton("Consultar");

    private final JButton botonCerrar = new JButton("Cerrar");

    //  Linea de estado visible para mostrar progreso, periodo consultado y errores.
    private final JLabel mensaje = new JLabel("Cargando canchas...");

    //  Indicador de suma de importes de alquileres confirmados y completados; no cobros comprobados.
    private final JLabel etiquetaIngresos = new JLabel("Ingresos del periodo: —");

    //  Indicador de ticket o importe promedio entre los alquileres no cancelados.
    private final JLabel etiquetaPromedio = new JLabel("Importe promedio por alquiler: —");

    //  Indicador: cancelados dividido entre total de alquileres, expresado en porcentaje.
    private final JLabel etiquetaCancelaciones = new JLabel("Reservas canceladas: —");

    //  Indicador de maxima cantidad de alquileres entre TODAS las canchas del rango.
    private final JLabel etiquetaCanchaMasAlquilada = new JLabel("Cancha más alquilada (todas): —");

    //  Indicador del dia de la SEMANA cuyo monto acumulado es mayor, respetando el filtro de cancha.
    private final JLabel etiquetaDiaMayorIngreso =
            new JLabel("Día de la semana con mayores ingresos: —");

    // Agrupa los resultados obtenidos en segundo plano.
    //  Agrupa cuatro resultados distintos que llegan juntos desde la tarea en segundo plano.
    private record DatosReporte(
            List<ResumenAlquiler> resumenes,
            List<CanchaResumen> canchasMasAlquiladas,
            List<IngresoPorDiaSemana> diasConMayoresIngresos,
            List<OcupacionCancha> ocupaciones) {}

    //  Tabla de resumen por estado: cantidad, importe total, promedio, minimo y maximo.
    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(
                    new Object[] {
                            "Estado", "Cantidad", "Total S/", "Promedio S/", "Mínimo S/", "Máximo S/"
                    },
                    0) {

                @Override
                //  Impide editar directamente las cifras calculadas en la tabla.
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    // RF10: tabla de ocupación por cancha (incluye canchas sin alquileres).
    //  Segunda tabla: ocupacion de cada cancha, expresada en horas y porcentaje teorico.
    private final DefaultTableModel modeloOcupacion =
            new DefaultTableModel(
                    new Object[] {
                            "ID", "Cancha", "Horas reservadas", "Capacidad teórica (h)", "Ocupación (%)"
                    },
                    0) {

                @Override
                //  Impide modificar la salida calculada de la tabla de ocupacion.
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    //  Indica que se esta realizando una operacion y evita pulsaciones repetidas.
    private boolean ocupado;

    //  Guarda una instantanea de la ultima consulta correcta para exportar exactamente esos datos.
    private ReporteExportacion ultimoReporte;

    //  Constructor de la ventana modal de reportes; configura filtros, pestanas y botones.
    public ReporteAlquilerVista(JFrame propietario, AlquilerControlador controlador) {

        //  La llamada super asigna titulo, ventana propietaria y comportamiento modal.
        super(propietario, "Sport Center - Reporte de alquileres", true);

        this.controlador = controlador;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1100, 610);
        setMinimumSize(new Dimension(950, 560));
        setLocationRelativeTo(propietario);

        //  La zona America/Lima determina el mes por defecto segun la fecha local del negocio.
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));

        //  Pone el primer dia del mes actual como inicio del filtro.
        campoDesde.setText(hoy.withDayOfMonth(1).format(FORMATO_FECHA));

        //  Pone el ultimo dia del mes actual como fin del filtro.
        campoHasta.setText(hoy.withDayOfMonth(hoy.lengthOfMonth()).format(FORMATO_FECHA));

        //  Panel principal que organiza los controles mediante BorderLayout.
        JPanel contenido = new JPanel(new BorderLayout(12, 12));

        contenido.setBorder(new EmptyBorder(15, 15, 15, 15));

        //  Primera banda de filtros: fecha de inicio, fin y botones de operaciones.
        // Primera fila: fechas y botones.
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filtros.add(new JLabel("Desde (dd/MM/aaaa):"));
        filtros.add(campoDesde);

        filtros.add(new JLabel("Hasta (dd/MM/aaaa):"));
        filtros.add(campoHasta);

        filtros.add(botonConsultar);
        filtros.add(botonExportar);
        filtros.add(botonCerrar);

        //  Segunda banda: filtrar por cancha sin alterar la interfaz del reporte.
        // Segunda fila: filtro por cancha.
        JPanel filtroCancha = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filtroCancha.add(new JLabel("Cancha:"));

        comboCanchas.setPreferredSize(new Dimension(350, 28));

        filtroCancha.add(comboCanchas);

        JPanel panelFiltros = new JPanel(new GridLayout(0, 1));

        panelFiltros.add(filtros);
        panelFiltros.add(filtroCancha);

        //  Crea la tabla de resumen empleando el modelo no editable anterior.
        JTable tabla = new JTable(modeloTabla);

        tabla.setRowHeight(28);
        tabla.setFillsViewportHeight(true);

        tabla.getTableHeader().setReorderingAllowed(false);

        //  Panel inferior destinado a avisos y a los cuatro indicadores de gestion.
        JPanel inferior = new JPanel(new GridLayout(0, 1, 0, 6));

        //  Aclara al usuario que los importes registrados no son pagos conciliados.
        inferior.add(
                new JLabel(
                        "Importes de alquileres por estado; "
                                + "no representan pagos verificados."));

        inferior.add(
                new JLabel(
                        "Sin alquileres, la cantidad es 0 "
                                + "y las estadísticas se muestran como —."));

        //  Destaca visualmente el importe acumulado del periodo.
        etiquetaIngresos.setFont(etiquetaIngresos.getFont().deriveFont(Font.BOLD));

        inferior.add(etiquetaIngresos);
        inferior.add(etiquetaPromedio);
        inferior.add(etiquetaCancelaciones);
        inferior.add(etiquetaCanchaMasAlquilada);
        inferior.add(etiquetaDiaMayorIngreso);
        inferior.add(mensaje);

        contenido.add(panelFiltros, BorderLayout.NORTH);

        //  El periodo y la cancha se comparten entre las dos pestanas.
        // Las dos pestañas comparten el mismo rango de fechas y filtro.
        JTable tablaOcupacion = new JTable(modeloOcupacion);
        tablaOcupacion.setRowHeight(28);
        tablaOcupacion.setFillsViewportHeight(true);
        tablaOcupacion.getTableHeader().setReorderingAllowed(false);

        JPanel panelOcupacion = new JPanel(new BorderLayout(8, 8));
        panelOcupacion.add(new JScrollPane(tablaOcupacion), BorderLayout.CENTER);

        //  Nota visible: 15 horas diarias es capacidad teorica, no asistencia real ni tiempo de apertura efectivo.
        JTextArea notaOcupacion =
                new JTextArea(
                        "Horas reservadas: alquileres confirmados y completados.\n"
                                + "Capacidad teórica: 15 horas por día y por cancha. "
                                + "No descuenta cierres por mantenimiento ni inactividad.\n"
                                + "La ocupación representa horarios reservados, "
                                + "no asistencia comprobada.");
        notaOcupacion.setEditable(false);
        notaOcupacion.setLineWrap(true);
        notaOcupacion.setWrapStyleWord(true);
        notaOcupacion.setRows(3);
        notaOcupacion.setFont(tablaOcupacion.getFont());
        notaOcupacion.setBackground(panelOcupacion.getBackground());
        panelOcupacion.add(notaOcupacion, BorderLayout.SOUTH);

        //  JTabbedPane permite alternar entre Resumen e ingresos y Ocupacion por cancha.
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Resumen e ingresos", new JScrollPane(tabla));
        pestanas.addTab("Ocupación por cancha", panelOcupacion);

        contenido.add(pestanas, BorderLayout.CENTER);

        contenido.add(inferior, BorderLayout.SOUTH);

        setContentPane(contenido);

        //  Conecta la pulsacion de Consultar con el metodo consultar().
        botonConsultar.addActionListener(e -> consultar());

        //  Conecta Exportar a Excel con el metodo exportarExcel().
        botonExportar.addActionListener(e -> exportarExcel());

        botonCerrar.addActionListener(e -> cerrar());

        addWindowListener(
                new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent e) {
                        cerrar();
                    }
                });

        //  Carga la lista de canchas al abrir la ventana, incluyendo inactivas con historial.
        // RF10: cargar las canchas al abrir el reporte.
        cargarCanchas();
    }

    // RF10: obtiene las canchas para el selector.
    /** Carga el selector en segundo plano, incluyendo canchas inactivas con historial. */
    //  METODO CLAVE: prepara el combo sin bloquear el hilo grafico de Swing.
    private void cargarCanchas() {

        //  Mientras se carga la lista, se marca como no disponible.
        canchasCargadas = false;

        //  Bloquea controles durante la lectura de canchas.
        establecerOcupado(true);

        mensaje.setText("Cargando canchas...");

        //  SwingWorker ejecuta consultas JDBC en un hilo secundario para no congelar la ventana.
        new SwingWorker<List<Cancha>, Void>() {

            @Override
            //  doInBackground: consulta la lista de canchas en segundo plano.
            protected List<Cancha> doInBackground() throws Exception {

                //  Solicita al controlador todas las canchas registradas, incluso las inactivas.
                return controlador.listarCanchasParaReporte();
            }

            @Override
            //  done: termina en el hilo grafico y actualiza los componentes de la ventana.
            protected void done() {

                try {
                    //  get recupera el resultado de la tarea, o lanza el error si fallo.
                    List<Cancha> canchas = get();

                    //  Limpia las opciones anteriores para evitar duplicarlas al recargar.
                    comboCanchas.removeAllItems();

                    //  Primera opcion especial: todas las canchas; su ID es null.
                    comboCanchas.addItem(new OpcionCancha(null, "Todas las canchas"));

                    //  Recorre las canchas de MySQL y las agrega al selector.
                    for (Cancha cancha : canchas) {

                        //  El item muestra nombre, ID y estado para distinguir canchas similares o inactivas.
                        comboCanchas.addItem(
                                new OpcionCancha(
                                        cancha.getIdCancha(),
                                        cancha.getNombre()
                                                + " | ID: "
                                                + cancha.getIdCancha()
                                                + " | "
                                                + cancha.getEstado().valorBD()));
                    }

                    //  Selecciona Todas las canchas por defecto.
                    comboCanchas.setSelectedIndex(0);

                    //  Ahora se puede consultar usando un filtro de cancha valido.
                    canchasCargadas = true;

                    mensaje.setText(
                            "Seleccione las fechas y la cancha; " + "después pulse Consultar.");

                    //  Si se interrumpe la tarea, se restablece la interrupcion del hilo.
                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    mostrarError("La carga de canchas fue interrumpida.");

                    //  Si fallo la consulta, se informa al usuario sin dejar la ventana bloqueada.
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

                    //  finally restablece el estado de botones haya terminado bien o mal.
                } finally {
                    establecerOcupado(false);
                }
            }
        }.execute();
    }

    /** Obtiene los datos del módulo en segundo plano y actualiza la pantalla. */
    //
    private void consultar() {

        //  Evita iniciar una segunda consulta o consultar antes de cargar las canchas.
        if (ocupado || !canchasCargadas) {
            return;
        }

        // Captura la cancha seleccionada antes de consultar.
        //  Lee la opcion elegida de forma segura ANTES de iniciar el SwingWorker.
        final OpcionCancha seleccion = (OpcionCancha) comboCanchas.getSelectedItem();

        //  No permite consultar sin tener una opcion valida en el selector.
        if (seleccion == null) {

            mostrarError("Seleccione una opción de cancha.");

            return;
        }

        //  idCancha sera null para todas las canchas; un entero para una sola.
        final Integer idCancha = seleccion.id();
        //  Guarda la etiqueta legible del filtro para mostrarla y exportarla.
        final String nombreFiltro = seleccion.nombre();

        //  Se declaran las dos fechas que se interpretaran desde los campos.
        final LocalDate desde;
        final LocalDate hasta;

        try {

            //  Convierte el texto dd/MM/aaaa a LocalDate usando un analizador estricto.
            desde = leerFecha(campoDesde, "inicial");
            hasta = leerFecha(campoHasta, "final");

            //  La fecha inicial no puede ser posterior a la fecha final.
            if (desde.isAfter(hasta)) {

                throw new IllegalArgumentException(
                        "La fecha inicial no puede ser posterior a la final.");
            }

        } catch (IllegalArgumentException e) {

            mostrarError(e.getMessage());
            return;
        }

        //  Marca la interfaz como ocupada para bloquear acciones incompatibles.
        establecerOcupado(true);

        // Limpia los resultados de la consulta anterior.
        //  Descarta el ultimo reporte para impedir exportar resultados anteriores con una consulta nueva.
        ultimoReporte = null;
        //  Limpia la tabla de resumen mientras se obtienen nuevos datos.
        modeloTabla.setRowCount(0);
        //  Limpia la tabla de ocupacion para no mezclar dos periodos.
        modeloOcupacion.setRowCount(0);
        etiquetaIngresos.setText("Ingresos del periodo: —");
        etiquetaPromedio.setText("Importe promedio por alquiler: —");
        etiquetaCancelaciones.setText("Reservas canceladas: —");
        etiquetaCanchaMasAlquilada.setText("Cancha más alquilada (todas): —");
        etiquetaCanchaMasAlquilada.setToolTipText(null);
        etiquetaDiaMayorIngreso.setText("Día de la semana con mayores ingresos: —");
        etiquetaDiaMayorIngreso.setToolTipText(null);

        mensaje.setText("Consultando reporte...");

        //  SwingWorker agrupa TODAS las consultas en una tarea asincrona.
        new SwingWorker<DatosReporte, Void>() {

            @Override
            //  doInBackground consulta JDBC fuera del hilo de Swing para mantener fluida la interfaz.
            protected DatosReporte doInBackground() throws Exception {

                //Aquí tenemos la parte principal de nuestro método consultar. Realizamos cuatro llamadas al controlador para obtener el resumen
                // de alquileres, la cancha más alquilada, el día con mayores ingresos y la ocupación de las canchas.
                //
                //Estos métodos se comunican con el DAO, donde se realizan las consultas SQL a MySQL, utilizando el rango de fechas y, cuando corresponde, el filtro de cancha seleccionado.
                List<ResumenAlquiler> resumenes =
                        controlador.resumirPorFechas(desde, hasta, idCancha);

                //  Segunda consulta: cancha mas alquilada. OJO: deliberadamente no recibe idCancha.
                List<CanchaResumen> masAlquiladas =
                        controlador.obtenerCanchasMasAlquiladas(desde, hasta);

                //  Tercera consulta: dia de la semana con mayor suma de importes, con filtro opcional.
                List<IngresoPorDiaSemana> dias =
                        controlador.obtenerDiasConMayoresIngresos(desde, hasta, idCancha);

                //  Cuarta consulta: minutos reservados por cancha y capacidad teorica en el periodo.
                List<OcupacionCancha> ocupaciones =
                        controlador.consultarOcupacion(desde, hasta, idCancha);

                //  Reune las cuatro listas en un solo objeto para entregarlas a la ventana.
                return new DatosReporte(resumenes, masAlquiladas, dias, ocupaciones);
            }

            @Override
            //  done recibe los resultados y actualiza las tablas y los indicadores.
            protected void done() {

                try {

                    //  get recupera los datos o propaga el error de la tarea.
                    DatosReporte datos = get();

                    //  Cada OcupacionCancha se convierte en una fila de la segunda pestana.
                    for (OcupacionCancha ocupacion : datos.ocupaciones()) {
                        modeloOcupacion.addRow(
                                new Object[] {
                                        //  Primera columna de ocupacion: identificador de la cancha.
                                        ocupacion.idCancha(),
                                        ocupacion.nombre(),
                                        //  Convierte a horas los minutos reservados para presentarlos en la tabla.
                                        ocupacion.horasReservadas().toPlainString(),
                                        //  Muestra las horas teoricas disponibles en el periodo, calculadas a 15 h/dia.
                                        ocupacion.horasCapacidadTeorica().toPlainString(),
                                        //  Calcula porcentaje: minutos reservados / minutos de capacidad * 100.
                                        ocupacion.porcentajeOcupacion().toPlainString()
                                });
                    }

                    //  Recupera los resúmenes separados por estados confirmado, cancelado y completado.
                    List<ResumenAlquiler> resumenes = datos.resumenes();

                    //  Acumulador de total de alquileres, incluidos los cancelados.
                    long cantidadTotal = 0;
                    //  Acumulador de importes validos para el indicador de ingresos.
                    BigDecimal ingresos = new BigDecimal("0.00");
                    //  Cuenta alquileres confirmados y completados, denominador del promedio.
                    long cantidadConIngreso = 0;
                    //  Cuenta de cancelados, numerador de la tasa de cancelacion.
                    long cantidadCancelados = 0;

                    //  Recorre cada estado para llenar la tabla y sumar los indicadores.
                    for (ResumenAlquiler resumen : resumenes) {

                        //  Un estado sin registros muestra cantidad cero y estadisticas sin dato (—).
                        boolean tieneDatos = resumen.getCantidad() > 0;

                        //  Inserta la fila de estado con cantidad y estadisticas que devolvio el DAO.
                        modeloTabla.addRow(
                                new Object[] {
                                        resumen.getEstado(),
                                        resumen.getCantidad(),
                                        resumen.getMontoTotal().toPlainString(),
                                        tieneDatos ? resumen.getMontoPromedio().toPlainString() : "—",
                                        tieneDatos ? resumen.getMontoMinimo().toPlainString() : "—",
                                        tieneDatos ? resumen.getMontoMaximo().toPlainString() : "—"
                                });

                        //  Los importes de confirmados y completados cuentan para ingresos; cancelados NO.
                        if (resumen.getEstado() == EstadoAlquiler.CONFIRMADO
                                || resumen.getEstado() == EstadoAlquiler.COMPLETADO) {
                            //  Suma montos monetarios con BigDecimal, sin errores de flotantes.
                            ingresos = ingresos.add(resumen.getMontoTotal());
                            //  Cuenta solamente alquileres que generan importe no cancelado.
                            cantidadConIngreso += resumen.getCantidad();
                            //  Las cancelaciones incrementan el numerador de la tasa.
                        } else if (resumen.getEstado() == EstadoAlquiler.CANCELADO) {
                            cantidadCancelados += resumen.getCantidad();
                        }

                        //  El total incluye todos los estados; sera el denominador del porcentaje cancelado.
                        cantidadTotal += resumen.getCantidad();
                    }

                    //  Solo divide para el promedio si existen alquileres confirmados/completados.
                    if (cantidadConIngreso > 0) {
                        //  Promedio = importe total / numero de alquileres no cancelados, 2 decimales.
                        //Después procesamos los resultados para calcular nuestros indicadores. Por ejemplo, el importe promedio se obtiene dividiendo
                        // el total de los importes no cancelados entre la cantidad de alquileres confirmados y completados.
                        //
                        //También calculamos el porcentaje de cancelaciones, dividiendo las reservas canceladas entre el total de reservas y
                        // multiplicando por cien.
                        BigDecimal promedio =
                                ingresos.divide(
                                        BigDecimal.valueOf(cantidadConIngreso),
                                        2,
                                        RoundingMode.HALF_UP);
                        etiquetaPromedio.setText(
                                "Importe promedio por alquiler: S/ "
                                        + promedio.toPlainString()
                                        + " (confirmados y completados)");
                    } else {
                        etiquetaPromedio.setText(
                                "Importe promedio por alquiler: — "
                                        + "(sin confirmados ni completados)");
                    }

                    //  Evita la division por cero si no hay alquileres en el periodo.
                    if (cantidadTotal > 0) {
                        //  Porcentaje cancelado = cancelados * 100 / total, 2 decimales.
                        BigDecimal porcentajeCancelados =
                                BigDecimal.valueOf(cantidadCancelados)
                                        .multiply(BigDecimal.valueOf(100))
                                        .divide(
                                                BigDecimal.valueOf(cantidadTotal),
                                                2,
                                                RoundingMode.HALF_UP);
                        etiquetaCancelaciones.setText(
                                "Reservas canceladas: "
                                        + porcentajeCancelados.toPlainString()
                                        + " % ("
                                        + cantidadCancelados
                                        + " de "
                                        + cantidadTotal
                                        + ")");
                    } else {
                        etiquetaCancelaciones.setText("Reservas canceladas: — (sin alquileres)");
                    }

                    //  Muestra la SUMA de importes no cancelados; no implica haber verificado pagos reales.
                    etiquetaIngresos.setText(
                            "Ingresos del periodo: S/ "
                                    + ingresos.toPlainString()
                                    + " (confirmados + completados; "
                                    + "no son cobros verificados)");

                    //  Obtiene todas las canchas empatadas en la cantidad maxima del periodo.
                    List<CanchaResumen> masAlquiladas = datos.canchasMasAlquiladas();

                    //  Sin alquileres confirmados o completados no hay cancha ganadora.
                    if (masAlquiladas.isEmpty()) {
                        etiquetaCanchaMasAlquilada.setText(
                                "Cancha más alquilada (todas): — "
                                        + "(sin confirmados ni completados)");
                    } else {
                        //  Concatena los nombres e IDs de todas las canchas empatadas.
                        String nombres =
                                masAlquiladas.stream()
                                        .map(
                                                resumen ->
                                                        resumen.getCancha().getNombre()
                                                                + " [ID "
                                                                + resumen.getCancha().getIdCancha()
                                                                + "]")
                                        .collect(Collectors.joining(", "));

                        //  Las ganadoras tienen la misma cantidad; se usa el primer conteo como referencia.
                        long cantidad = masAlquiladas.get(0).getCantidadAlquileres();

                        //  Distingue una unica ganadora frente a varias empatadas.
                        String titulo =
                                masAlquiladas.size() == 1
                                        ? "Cancha más alquilada (todas): "
                                        : "Canchas más alquiladas (empate; todas): ";

                        //  Actualiza la etiqueta con nombre, ID y numero de alquileres por cancha.
                        etiquetaCanchaMasAlquilada.setText(
                                titulo + nombres + " | " + cantidad + " alquiler(es) por cancha");
                    }
                    etiquetaCanchaMasAlquilada.setToolTipText(etiquetaCanchaMasAlquilada.getText());

                    //  Obtiene el dia o dias empatados con importe acumulado maximo.
                    List<IngresoPorDiaSemana> dias = datos.diasConMayoresIngresos();

                    //  Sin importes validos no puede identificarse un dia con mayores ingresos.
                    if (dias.isEmpty()) {
                        etiquetaDiaMayorIngreso.setText(
                                "Día de la semana con mayores ingresos: — "
                                        + "(sin confirmados ni completados)");
                    } else {
                        //  Traduce DayOfWeek al nombre del dia en castellano de Peru.
                        String nombresDias =
                                dias.stream()
                                        .map(
                                                ingreso ->
                                                        ingreso.dia()
                                                                .getDisplayName(
                                                                        TextStyle.FULL,
                                                                        Locale.forLanguageTag(
                                                                                "es-PE")))
                                        .collect(Collectors.joining(", "));

                        //  Obtiene el importe maximo acumulado por dia de la semana.
                        BigDecimal monto = dias.get(0).montoTotal();

                        //  Distingue un unico dia ganador frente a un empate.
                        String titulo =
                                dias.size() == 1
                                        ? "Día de la semana con mayores ingresos: "
                                        : "Días con mayores ingresos (empate): ";

                        etiquetaDiaMayorIngreso.setText(
                                titulo
                                        + nombresDias
                                        + " | S/ "
                                        + monto.toPlainString()
                                        + " acumulados por día de la semana");
                    }
                    etiquetaDiaMayorIngreso.setToolTipText(etiquetaDiaMayorIngreso.getText());

                    //  Muestra el periodo, el filtro y el total de alquileres consultados.
                    mensaje.setText(
                            "Periodo: "
                                    + desde.format(FORMATO_FECHA)
                                    + " al "
                                    + hasta.format(FORMATO_FECHA)
                                    + " | Cancha: "
                                    + nombreFiltro
                                    + " | Alquileres: "
                                    + cantidadTotal);

                    //  odjetivo3: guarda la ultima consulta correcta con filtros y sus cuatro listas.
                    ultimoReporte =
                            new ReporteExportacion(
                                    desde,
                                    hasta,
                                    nombreFiltro,
                                    datos.resumenes(),
                                    datos.canchasMasAlquiladas(),
                                    datos.diasConMayoresIngresos(),
                                    datos.ocupaciones());

                    //  Manejo de interrupciones mientras se calculan los indicadores.
                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    mostrarError("La consulta fue interrumpida.");

                    //  Manejo de errores de la consulta asincrona.
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

                    //  Habilita nuevamente la interfaz en todos los casos.
                } finally {
                    establecerOcupado(false);
                }
            }
        }.execute();
    }

    /**
     * Captura el reporte consultado, solicita destino y genera el archivo en segundo plano. Escribe
     * primero un temporal y pide confirmación antes de reemplazar un archivo existente.
     */
    //  METODO CLAVE PARA EXPORTAR: solicita destino y escribe el XLSX sin bloquear Swing.
    private void exportarExcel() {
        //  No exporta durante otra operacion ni sin resultados consultados previamente.
        if (ocupado || ultimoReporte == null) {
            return;
        }

        // Conserva los datos y filtros de la última consulta exitosa.
        //  Usa la instantanea de la ultima consulta, aunque el usuario haya editado los filtros.
        final ReporteExportacion reporte = ultimoReporte;

        //  JFileChooser permite que el usuario seleccione la ubicacion del XLSX.
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar reporte de SportCenter");
        //  Se limita el selector de archivos a Excel xlsx.
        selector.setAcceptAllFileFilterUsed(false);
        selector.setFileFilter(new FileNameExtensionFilter("Archivo Excel (*.xlsx)", "xlsx"));

        //  Sugiere un nombre formado por el periodo consultado.
        selector.setSelectedFile(
                new File("SportCenter_" + reporte.desde() + "_" + reporte.hasta() + ".xlsx"));

        //  Si cancela el selector no modifica ningun archivo.
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File elegido = selector.getSelectedFile();

        //  Asegura la extension xlsx si el usuario no la escribio.
        if (!elegido.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            elegido = new File(elegido.getParentFile(), elegido.getName() + ".xlsx");
        }

        //  Convierte el destino en ruta absoluta.
        final Path destino = elegido.toPath().toAbsolutePath();

        //  Evita que el usuario seleccione una carpeta como si fuera un archivo.
        if (Files.isDirectory(destino)) {
            mostrarError("Seleccione un archivo, no una carpeta.");
            return;
        }

        //  Comprueba si ya existe un archivo en el destino.
        final boolean reemplazar = Files.exists(destino);

        //  Pide confirmacion explicita antes de sobreescribir un Excel anterior.
        if (reemplazar) {
            int respuesta =
                    JOptionPane.showConfirmDialog(
                            this,
                            "El archivo ya existe:\n" + destino + "\n\n¿Desea reemplazarlo?",
                            "Confirmar reemplazo",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);

            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
        }

        //  Bloquea acciones mientras se genera y guarda el archivo.
        establecerOcupado(true);
        mensaje.setText("Exportando reporte a Excel...");

        //  Escritura asincrona: Excel se genera en segundo plano, no en el hilo grafico.
        new SwingWorker<Path, Void>() {

            @Override
            //  doInBackground calcula los bytes del workbook y los escribe en disco.
            protected Path doInBackground() throws Exception {
                //  ExportadorReporteExcel genera el XLSX en memoria mediante Apache POI.
                byte[] contenido = ExportadorReporteExcel.generar(reporte);

                // Escribe primero en un archivo temporal.
                // Así no vaciamos el archivo anterior si falla la generación.
                //  Crea un archivo temporal en la misma carpeta del Excel definitivo.
                Path temporal = Files.createTempFile(destino.getParent(), "sportcenter-", ".tmp");

                try {
                    //  Guarda primero los bytes completos en el archivo temporal.
                    Files.write(temporal, contenido);

                    //  Solo reemplaza un Excel existente si se acepto el dialogo.
                    if (reemplazar) {
                        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        //  Sin permiso de reemplazo, un destino creado por otro proceso ocasiona fallo.
                        // Si alguien creó el destino entretanto,
                        // falla en lugar de reemplazarlo sin autorización.
                        Files.move(temporal, destino);
                    }

                    return destino;

                } finally {
                    //  Limpia siempre el archivo temporal tras completar o fallar.
                    Files.deleteIfExists(temporal);
                }
            }

            @Override
            //  done vuelve al hilo grafico para informar el resultado de la exportacion.
            protected void done() {
                try {
                    //  Obtiene la ruta guardada o la excepcion de fondo.
                    Path guardado = get();

                    mensaje.setText("Reporte exportado: " + guardado);

                    //  Muestra la confirmacion final indicando periodo y cancha exportados.
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
                            JOptionPane.INFORMATION_MESSAGE);

                    //  La interrupcion se maneja de forma segura.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La exportación fue interrumpida.");

                    //  Avisa si no pudo guardar, por ejemplo por permisos o archivo abierto.
                } catch (ExecutionException e) {
                    mostrarError(
                            "No se pudo exportar el reporte. "
                                    + "Compruebe los permisos de la carpeta "
                                    + "y cierre el archivo si está abierto en Excel.");

                    if (e.getCause() != null) {
                        e.getCause().printStackTrace();
                    }

                    //  Devuelve los controles al estado normal, haya exito o fallo.
                } finally {
                    establecerOcupado(false);
                }
            }
        }.execute();
    }

    /** Interpreta la entrada con formato estricto y comunica fechas inválidas. */
    //  Lee y valida las fechas que el usuario escribe en el filtro.
    private LocalDate leerFecha(JTextField campo, String nombre) {

        //  strip elimina espacios al inicio y final.
        String texto = campo.getText().strip();

        //  Primero verifica la estructura numerica dd/MM/aaaa.
        if (!texto.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}")) {

            throw new IllegalArgumentException(
                    "Ingrese la fecha " + nombre + " con formato dd/MM/aaaa.");
        }

        try {

            //  LocalDate.parse valida tambien fechas imposibles como 31/02.
            return LocalDate.parse(texto, FORMATO_FECHA);

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException("La fecha " + nombre + " no es válida.");
        }
    }

    /** Sincroniza los controles con el estado de la tarea para evitar acciones simultáneas. */
    //  Habilita o deshabilita controles segun si hay una tarea ejecutandose.
    private void establecerOcupado(boolean valor) {

        //  Actualiza la bandera interna de ocupacion.
        ocupado = valor;

        campoDesde.setEnabled(!valor);
        campoHasta.setEnabled(!valor);

        //  No habilita el filtro de cancha hasta completar su carga.
        comboCanchas.setEnabled(!valor && canchasCargadas);

        //  Exportar solo se habilita despues de una consulta correcta y sin tarea activa.
        botonExportar.setEnabled(!valor && ultimoReporte != null);

        //  Consultar solo se habilita cuando el combo este cargado y no haya otra operacion.
        botonConsultar.setEnabled(!valor && canchasCargadas);

        botonCerrar.setEnabled(!valor);

        //  Cambia el cursor a espera mientras se consulta o exporta.
        setCursor(Cursor.getPredefinedCursor(valor ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /** Presenta el mensaje de error al usuario mediante un diálogo. */
    //  Muestra un error en etiqueta y ventana de dialogo.
    private void mostrarError(String texto) {

        mensaje.setText(texto);

        JOptionPane.showMessageDialog(
                this, texto, "Reporte de alquileres", JOptionPane.WARNING_MESSAGE);
    }

    /** Cierra la ventana cuando no hay una operación en curso. */
    //  Cierra la ventana solo si no se esta realizando una operacion.
    private void cerrar() {

        if (!ocupado) {
            dispose();
        }
    }
}