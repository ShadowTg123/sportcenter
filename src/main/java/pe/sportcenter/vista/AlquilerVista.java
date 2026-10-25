
// TEMA: PANTALLA DE ALQUILERES: capturar, consultar, reprogramar, cancelar y completar


package pe.sportcenter.vista;

import pe.sportcenter.controlador.AlquilerControlador;
// IMPORT: Modelo de una reserva registrada.
import pe.sportcenter.modelo.Alquiler;
// IMPORT: Modelo para la cancha seleccionada.
import pe.sportcenter.modelo.Cancha;
// IMPORT: Modelo para el cliente que realiza la reserva.
import pe.sportcenter.modelo.Cliente;
// IMPORT: Define confirmado, cancelado y completado.
import pe.sportcenter.modelo.EstadoAlquiler;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
// IMPORT: Fecha sin hora, útil para reservar un día concreto.
import java.time.LocalDate;
// IMPORT: Fecha+hora usada para impedir reprogramar reservas ya iniciadas.
import java.time.LocalDateTime;
// IMPORT: Hora local, útil para inicio y fin del alquiler.
import java.time.LocalTime;
// IMPORT: Define America/Lima en comparaciones con la hora actual.
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
// IMPORT: Representa una operación que devuelve un resultado.
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
// IMPORT: Indica qué hacer con el resultado de una operación asíncrona.
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla de alquileres: consulta por fecha, disponibilidad, registro y reprogramación. Gestiona
 * también cancelaciones y finalización, delegando las reglas al controlador.
 */
// ESTA CLASE: PANTALLA DE ALQUILERES: capturar, consultar, reprogramar, cancelar y completar.
public class AlquilerVista extends JDialog {

    // CAMPO: Define el formato estricto que el usuario escribe en la interfaz.
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // CAMPO: Define el formato estricto que el usuario escribe en la interfaz.
    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    // CAMPO: Referencia al controlador: la vista no ejecuta SQL directamente.
    private final AlquilerControlador controlador;

    // CAMPO: Lista desplegable para elegir el registro relacionado.
    private final JComboBox<Cliente> comboClientes = new JComboBox<>();
    // CAMPO: Lista desplegable para elegir el registro relacionado.
    private final JComboBox<Cancha> comboCanchas = new JComboBox<>();

    // CAMPO: Campo de entrada visible de la ventana.
    private final JTextField campoFecha = new JTextField(12);
    // CAMPO: Campo de entrada visible de la ventana.
    private final JTextField campoInicio = new JTextField("18:00", 8);
    // CAMPO: Campo de entrada visible de la ventana.
    private final JTextField campoFin = new JTextField("19:00", 8);

    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonRegistrar = new JButton("Registrar alquiler");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonConsultar = new JButton("Consultar fecha");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonCancelar = new JButton("Cancelar alquiler");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonCompletar = new JButton("Completar alquiler");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonCerrar = new JButton("Cerrar");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonCargarEdicion = new JButton("Cargar para modificar");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonModificar = new JButton("Modificar alquiler");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonNuevo = new JButton("Nuevo alquiler");
    // CAMPO: Etiqueta para mostrar informacion al usuario.
    private final JLabel etiquetaModo = new JLabel("Modo: nuevo alquiler");
    // CAMPO: Boton de accion: luego se vincula a un listener.
    private final JButton botonDisponibilidad = new JButton("Ver disponibilidad");

    // CAMPO: Etiqueta para mostrar informacion al usuario.
    private final JLabel mensaje = new JLabel(" ");

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(
                    new Object[] {
                            "ID", "Cancha", "Cliente", "Fecha",
                            "Inicio", "Fin", "Monto S/", "Estado"
                    },
                    0) {

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    // PASO (clase): Devuelve el dato calculado o la respuesta de esta operacion.
                    return false;
                }
            };

    // CAMPO: Tabla visual de datos; se alimenta del modelo de tabla.
    private final JTable tabla = new JTable(modeloTabla);

    private List<Alquiler> alquileres = List.of();
    // CAMPO: Si es null estamos creando; si contiene un alquiler se esta modificando.
    private Alquiler alquilerEnEdicion;
    // CAMPO: Evita iniciar otra tarea mientras hay una consulta/registro en marcha.
    private boolean ocupado;

    // CAMPO: La zona horaria es America/Lima; controla comparaciones con la hora actual.
    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    // Agrupa los resultados de una consulta.
    // Agrupa resultados que la vista mostrará juntos, sin crear una clase adicional.
    private record Datos(List<Cliente> clientes, List<Cancha> canchas, List<Alquiler> alquileres) {}

    // EXPLICACIÓN DEL MÉTODO: Construye la ventana modal, sus campos, la tabla y los botones; vincula cada clic con el
    //     metodo de esta misma vista.
    public AlquilerVista(JFrame propietario, AlquilerControlador controlador) {

        super(propietario, "Sport Center - Alquileres", true);
        this.controlador = controlador;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1100, 660);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(propietario);

        campoFecha.setText(
                LocalDate.now(ZoneId.of("America/Lima")).plusDays(1).format(FORMATO_FECHA));

        configurarListas();

        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        contenido.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel formulario = new JPanel(new GridLayout(0, 2, 10, 8));
        formulario.setBorder(BorderFactory.createTitledBorder("Datos del alquiler"));

        formulario.add(new JLabel("Cliente:"));
        formulario.add(comboClientes);

        formulario.add(new JLabel("Cancha habilitada:"));
        formulario.add(comboCanchas);

        formulario.add(new JLabel("Fecha (dd/MM/aaaa):"));
        formulario.add(campoFecha);

        formulario.add(new JLabel("Hora inicial (HH:mm):"));
        formulario.add(campoInicio);

        formulario.add(new JLabel("Hora final (HH:mm, mismo día):"));
        formulario.add(campoFin);

        formulario.add(new JLabel("Canchas libres en este horario:"));
        formulario.add(botonDisponibilidad);

        // Separamos las acciones para evitar que se amontonen.
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(botonRegistrar);
        acciones.add(botonConsultar);
        acciones.add(botonCancelar);
        acciones.add(botonCompletar);
        acciones.add(botonCerrar);

        JPanel accionesEdicion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        accionesEdicion.add(botonCargarEdicion);
        accionesEdicion.add(botonModificar);
        accionesEdicion.add(botonNuevo);
        accionesEdicion.add(etiquetaModo);

        JPanel botones = new JPanel(new GridLayout(2, 1, 0, 2));
        botones.add(acciones);
        botones.add(accionesEdicion);

        JPanel superior = new JPanel(new BorderLayout(5, 5));
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

        // PASO (AlquilerVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonConsultar.addActionListener(e -> consultar());
        // PASO (AlquilerVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonDisponibilidad.addActionListener(e -> consultarDisponibilidad());
        // PASO (AlquilerVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonRegistrar.addActionListener(e -> registrar());
        // PASO (AlquilerVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonCargarEdicion.addActionListener(e -> cargarParaModificar());
        // PASO (AlquilerVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonModificar.addActionListener(e -> modificar());
        // PASO (AlquilerVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonNuevo.addActionListener(e -> prepararNuevoAlquiler());
        // PASO (AlquilerVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonCancelar.addActionListener(e -> cancelar());
        // PASO (AlquilerVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonCompletar.addActionListener(e -> completar());
        // PASO (AlquilerVista): Conecta el clic del boton con el metodo que realiza la accion.
        botonCerrar.addActionListener(e -> cerrar());

        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        cerrar();
                    }
                });

        actualizarModo();
        consultar();
    }

    /** Configura la presentación de clientes y canchas en los selectores. */
    // EXPLICACIÓN DEL MÉTODO: Configura el texto de los combos: clientes por nombre y canchas por nombre y tarifa; no
    //     modifica MySQL.
    private void configurarListas() {
        comboClientes.setRenderer(
                new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(
                            JList<?> lista,
                            Object valor,
                            int indice,
                            boolean seleccionado,
                            boolean tieneFoco) {

                        super.getListCellRendererComponent(
                                lista, valor, indice, seleccionado, tieneFoco);

                        if (valor instanceof Cliente cliente) {
                            setText(cliente.getNombre() + " | DNI: " + cliente.getDni());
                        }

                        return this;
                    }
                });

        comboCanchas.setRenderer(
                new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(
                            JList<?> lista,
                            Object valor,
                            int indice,
                            boolean seleccionado,
                            boolean tieneFoco) {

                        super.getListCellRendererComponent(
                                lista, valor, indice, seleccionado, tieneFoco);

                        if (valor instanceof Cancha cancha) {
                            setText(
                                    cancha.getNombre()
                                            + " | S/ "
                                            + cancha.getTarifaHora().toPlainString()
                                            + " por hora");
                        }

                        return this;
                    }
                });
    }

    /** Obtiene los datos del módulo en segundo plano y actualiza la pantalla. */
    // EXPLICACIÓN DEL MÉTODO: Consulta clientes, canchas habilitadas y alquileres de una fecha; la carga se realiza en
    //     segundo plano.
    private void consultar() {
        // PASO (consultar): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (consultar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        final LocalDate fecha;

        // PASO (consultar): Comienza un bloque protegido: permite controlar errores y liberar recursos.
        try {
            fecha = leerFecha();
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
            // PASO (consultar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // PASO (consultar): Ejecuta el trabajo mediante un SwingWorker, separado del hilo de la interfaz.
        ejecutar(
                () ->
                        new Datos(
                                controlador.listarClientes(),
                                controlador.listarCanchasHabilitadas(),
                                controlador.listarPorFecha(fecha)),
                datos -> mostrarDatos(datos, fecha),
                "Consultando alquileres...");
    }

    /** Actualiza las listas y la tabla con el resultado de la consulta por fecha. */
    // EXPLICACIÓN DEL MÉTODO: Vacia y vuelve a llenar la tabla y los combos con los resultados; por eso refresca la
    //     pantalla.
    private void mostrarDatos(Datos datos, LocalDate fecha) {
        // La consulta actualiza las listas y termina cualquier edición anterior.
        alquilerEnEdicion = null;
        tabla.clearSelection();
        comboClientes.removeAllItems();

        // PASO (mostrarDatos): Recorre los elementos de una lista para agregarlos o compararlos.
        for (Cliente cliente : datos.clientes()) {
            comboClientes.addItem(cliente);
        }

        comboCanchas.removeAllItems();

        // PASO (mostrarDatos): Recorre los elementos de una lista para agregarlos o compararlos.
        for (Cancha cancha : datos.canchas()) {
            comboCanchas.addItem(cancha);
        }

        alquileres = datos.alquileres();
        // PASO (mostrarDatos): Limpia la tabla para volver a cargar los resultados actuales.
        modeloTabla.setRowCount(0);

        // PASO (mostrarDatos): Recorre los elementos de una lista para agregarlos o compararlos.
        for (Alquiler alquiler : alquileres) {
            String nombreCancha =
                    datos.canchas().stream()
                            .filter(c -> c.getIdCancha() == alquiler.getIdCancha())
                            .map(Cancha::getNombre)
                            .findFirst()
                            .orElse("Cancha ID " + alquiler.getIdCancha());

            String nombreCliente =
                    datos.clientes().stream()
                            .filter(c -> c.getIdCliente() == alquiler.getIdCliente())
                            .map(Cliente::getNombre)
                            .findFirst()
                            .orElse("Cliente ID " + alquiler.getIdCliente());

            // PASO (mostrarDatos): Agrega una fila visible a la tabla de la interfaz.
            modeloTabla.addRow(
                    new Object[] {
                            alquiler.getIdAlquiler(),
                            nombreCancha,
                            nombreCliente,
                            alquiler.getFecha().format(FORMATO_FECHA),
                            alquiler.getHoraInicio().format(FORMATO_HORA),
                            alquiler.getHoraFin().format(FORMATO_HORA),
                            alquiler.getMontoTotal(),
                            alquiler.getEstado()
                    });
        }

        actualizarModo();
        mensaje.setText(
                "Fecha consultada: "
                        + fecha.format(FORMATO_FECHA)
                        + " | Alquileres: "
                        + alquileres.size());
    }

    // RF06: mostrar canchas libres para la fecha y horario indicados.
    // EXPLICACIÓN DEL MÉTODO: Recibe fecha e intervalo y solicita al controlador las canchas realmente libres para esa
    //     franja.
    private void consultarDisponibilidad() {
        // PASO (consultarDisponibilidad): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (consultarDisponibilidad): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // PASO (consultarDisponibilidad): Comienza un bloque protegido: permite controlar errores y liberar recursos.
        try {
            LocalDate fecha = leerFecha();
            LocalTime inicio = leerHora(campoInicio, "inicial");
            LocalTime fin = leerHora(campoFin, "final");

            // PASO (consultarDisponibilidad): Ejecuta el trabajo mediante un SwingWorker, separado del hilo de la interfaz.
            ejecutar(
                    () -> controlador.consultarDisponibilidad(fecha, inicio, fin),
                    disponibles -> {
                        String horario =
                                fecha.format(FORMATO_FECHA)
                                        + " | "
                                        + inicio.format(FORMATO_HORA)
                                        + " a "
                                        + fin.format(FORMATO_HORA);

                        mensaje.setText(
                                "Disponibilidad consultada: "
                                        + horario
                                        + " | Canchas libres: "
                                        + disponibles.size());

                        if (disponibles.isEmpty()) {
                            JOptionPane.showMessageDialog(
                                    this,
                                    "No hay canchas disponibles para:\n" + horario,
                                    "Disponibilidad",
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        DefaultTableModel modeloDisponibles =
                                new DefaultTableModel(
                                        new Object[] {
                                                "ID", "Cancha",
                                                "Deporte", "Tarifa S/ por hora"
                                        },
                                        0) {

                                    @Override
                                    public boolean isCellEditable(int fila, int columna) {
                                        return false;
                                    }
                                };

                        for (Cancha cancha : disponibles) {
                            modeloDisponibles.addRow(
                                    new Object[] {
                                            cancha.getIdCancha(),
                                            cancha.getNombre(),
                                            cancha.getDeporte(),
                                            cancha.getTarifaHora()
                                    });
                        }

                        JTable tablaDisponibles = new JTable(modeloDisponibles);

                        tablaDisponibles.setRowHeight(26);
                        tablaDisponibles.setFillsViewportHeight(true);

                        JScrollPane desplazamiento = new JScrollPane(tablaDisponibles);

                        desplazamiento.setPreferredSize(new Dimension(600, 220));

                        JPanel panel = new JPanel(new BorderLayout(8, 8));

                        panel.add(new JLabel("Horario consultado: " + horario), BorderLayout.NORTH);

                        panel.add(desplazamiento, BorderLayout.CENTER);

                        panel.add(
                                new JLabel(
                                        alquilerEnEdicion == null
                                                ? "La disponibilidad se verifica nuevamente al registrar."
                                                : "Al modificar, el propio alquiler se excluye del cruce."),
                                BorderLayout.SOUTH);

                        JOptionPane.showMessageDialog(
                                this,
                                panel,
                                "Canchas disponibles",
                                JOptionPane.INFORMATION_MESSAGE);
                    },
                    "Consultando disponibilidad...");

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    // RF07: carga un alquiler confirmado para editarlo sin crear otro registro.
    /** Carga el alquiler seleccionado en el formulario y verifica que pueda editarse. */
    // EXPLICACIÓN DEL MÉTODO: Toma el alquiler seleccionado, comprueba si puede editarse y carga sus datos en el formulario.
    private void cargarParaModificar() {
        // PASO (cargarParaModificar): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (cargarParaModificar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        int filaVista = tabla.getSelectedRow();
        // PASO (cargarParaModificar): Comprueba una condicion de negocio o controla un caso de error.
        if (filaVista < 0) {
            mostrarError("Seleccione primero un alquiler de la tabla.");
            // PASO (cargarParaModificar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // La tabla admite ordenación por columnas: convertir el índice.
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Alquiler seleccionado = alquileres.get(filaModelo);

        // PASO (cargarParaModificar): Solo los alquileres confirmados pueden modificarse o cancelarse.
        if (seleccionado.getEstado() != EstadoAlquiler.CONFIRMADO) {
            mostrarError("Solo se pueden modificar alquileres confirmados.");
            // PASO (cargarParaModificar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // PASO (cargarParaModificar): Combina fecha y hora para compararlas con el reloj de Lima.
        LocalDateTime inicioOriginal =
                LocalDateTime.of(seleccionado.getFecha(), seleccionado.getHoraInicio());
        // PASO (cargarParaModificar): Exige un inicio posterior al momento/limite indicado.
        if (!inicioOriginal.isAfter(LocalDateTime.now(ZONA))) {
            mostrarError("No se puede modificar un alquiler que ya comenzó.");
            // PASO (cargarParaModificar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        boolean clienteEncontrado = seleccionarCliente(seleccionado.getIdCliente());
        boolean canchaEncontrada = seleccionarCancha(seleccionado.getIdCancha());

        campoFecha.setText(seleccionado.getFecha().format(FORMATO_FECHA));
        campoInicio.setText(seleccionado.getHoraInicio().format(FORMATO_HORA));
        campoFin.setText(seleccionado.getHoraFin().format(FORMATO_HORA));

        alquilerEnEdicion = seleccionado;
        actualizarModo();
        mensaje.setText(
                "Editando alquiler ID "
                        + seleccionado.getIdAlquiler()
                        + ". Cambie los datos y pulse 'Modificar alquiler'.");

        // Una cancha antigua puede haber sido desactivada posteriormente.
        // PASO (cargarParaModificar): Comprueba una condicion de negocio o controla un caso de error.
        if (!clienteEncontrado || !canchaEncontrada) {
            // PASO (cargarParaModificar): Muestra una ventana de informacion, confirmacion o advertencia.
            JOptionPane.showMessageDialog(
                    this,
                    "Alguno de los datos originales ya no está disponible.\n"
                            + "Seleccione una cancha y un cliente válidos antes de modificar.",
                    "Revisar selección",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Busca por identificador y selecciona el cliente del alquiler en edición. */
    // EXPLICACIÓN DEL MÉTODO: Busca el ID del cliente original y lo selecciona dentro del combo.
    private boolean seleccionarCliente(int idCliente) {
        // PASO (seleccionarCliente): Recorre los elementos de una lista para agregarlos o compararlos.
        for (int i = 0; i < comboClientes.getItemCount(); i++) {
            // PASO (seleccionarCliente): Comprueba una condicion de negocio o controla un caso de error.
            if (comboClientes.getItemAt(i).getIdCliente() == idCliente) {
                comboClientes.setSelectedIndex(i);
                // PASO (seleccionarCliente): Devuelve el dato calculado o la respuesta de esta operacion.
                return true;
            }
        }
        comboClientes.setSelectedIndex(-1);
        // PASO (seleccionarCliente): Devuelve el dato calculado o la respuesta de esta operacion.
        return false;
    }

    /** Busca por identificador y selecciona la cancha del alquiler en edición. */
    // EXPLICACIÓN DEL MÉTODO: Busca el ID de la cancha original y lo selecciona en el combo.
    private boolean seleccionarCancha(int idCancha) {
        // PASO (seleccionarCancha): Recorre los elementos de una lista para agregarlos o compararlos.
        for (int i = 0; i < comboCanchas.getItemCount(); i++) {
            // PASO (seleccionarCancha): Comprueba una condicion de negocio o controla un caso de error.
            if (comboCanchas.getItemAt(i).getIdCancha() == idCancha) {
                comboCanchas.setSelectedIndex(i);
                // PASO (seleccionarCancha): Devuelve el dato calculado o la respuesta de esta operacion.
                return true;
            }
        }
        comboCanchas.setSelectedIndex(-1);
        // PASO (seleccionarCancha): Devuelve el dato calculado o la respuesta de esta operacion.
        return false;
    }

    // RF07 y RF08: modifica el registro actual y recalcula su importe.
    // EXPLICACIÓN DEL MÉTODO: Solicita confirmacion, envia el ID y los nuevos datos al controlador y muestra el importe
    //     recalculado.
    private void modificar() {
        // PASO (modificar): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (modificar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // PASO (modificar): Evita seguir si falta un dato necesario para la operacion.
        if (alquilerEnEdicion == null) {
            mostrarError("Seleccione un alquiler y pulse 'Cargar para modificar'.");
            // PASO (modificar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // PASO (modificar): Comienza un bloque protegido: permite controlar errores y liberar recursos.
        try {
            Cancha cancha = (Cancha) comboCanchas.getSelectedItem();
            Cliente cliente = (Cliente) comboClientes.getSelectedItem();

            // PASO (modificar): Evita seguir si falta un dato necesario para la operacion.
            if (cancha == null || cliente == null) {
                // PASO (modificar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                throw new IllegalArgumentException("Seleccione una cancha y un cliente válidos.");
            }

            LocalDate fecha = leerFecha();
            LocalTime inicio = leerHora(campoInicio, "inicial");
            LocalTime fin = leerHora(campoFin, "final");
            final int idAlquiler = alquilerEnEdicion.getIdAlquiler();

            int confirmacion =
                    // PASO (modificar): Muestra una ventana de informacion, confirmacion o advertencia.
                    JOptionPane.showConfirmDialog(
                            this,
                            "¿Modificar el alquiler ID "
                                    + idAlquiler
                                    + "?\n"
                                    + "Cancha: "
                                    + cancha.getNombre()
                                    + "\n"
                                    + "Cliente: "
                                    + cliente.getNombre()
                                    + "\n"
                                    + "Fecha: "
                                    + fecha.format(FORMATO_FECHA)
                                    + "\n"
                                    + "Horario: "
                                    + inicio.format(FORMATO_HORA)
                                    + " a "
                                    + fin.format(FORMATO_HORA)
                                    + "\n"
                                    + "El importe se recalculará automáticamente.",
                            "Confirmar modificación",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);

            // PASO (modificar): Respeta la decision de cancelar el dialogo: no cambia MySQL.
            if (confirmacion != JOptionPane.YES_OPTION) {
                // PASO (modificar): Devuelve el dato calculado o la respuesta de esta operacion.
                return;
            }

            // PASO (modificar): Ejecuta el trabajo mediante un SwingWorker, separado del hilo de la interfaz.
            ejecutar(
                    () ->
                            controlador.reprogramar(
                                    idAlquiler,
                                    cancha.getIdCancha(),
                                    cliente.getIdCliente(),
                                    fecha,
                                    inicio,
                                    fin),
                    modificado -> {
                        JOptionPane.showMessageDialog(
                                this,
                                "Alquiler modificado correctamente.\n"
                                        + "ID conservado: "
                                        + modificado.getIdAlquiler()
                                        + "\nNuevo monto: S/ "
                                        + modificado.getMontoTotal().toPlainString(),
                                "Modificación de alquiler",
                                JOptionPane.INFORMATION_MESSAGE);
                        // La fecha visible es ahora la nueva fecha del alquiler.
                        // Se vuelve a consultar sin duplicar el registro.
                        consultar();
                    },
                    "Modificando alquiler...");

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    // Sale del modo edición y permite registrar una reserva distinta.
    /** Termina la edición actual y vuelve al modo de registro. */
    // EXPLICACIÓN DEL MÉTODO: Sale del modo edicion, limpia la seleccion y propone una fecha futura y horario de ejemplo.
    private void prepararNuevoAlquiler() {
        // PASO (prepararNuevoAlquiler): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (prepararNuevoAlquiler): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }
        alquilerEnEdicion = null;
        tabla.clearSelection();
        campoFecha.setText(LocalDate.now(ZONA).plusDays(1).format(FORMATO_FECHA));
        campoInicio.setText("18:00");
        campoFin.setText("19:00");
        // PASO (prepararNuevoAlquiler): Comprueba una condicion de negocio o controla un caso de error.
        if (comboClientes.getItemCount() > 0) {
            comboClientes.setSelectedIndex(0);
        }
        // PASO (prepararNuevoAlquiler): Comprueba una condicion de negocio o controla un caso de error.
        if (comboCanchas.getItemCount() > 0) {
            comboCanchas.setSelectedIndex(0);
        }
        actualizarModo();
        mensaje.setText("Modo de registro: puede crear un nuevo alquiler.");
    }

    /** Actualiza la indicación visual del modo de registro o modificación. */
    // EXPLICACIÓN DEL MÉTODO: Activa o desactiva botones segun si la pantalla esta registrando un alquiler nuevo o
    //     modificando uno.
    private void actualizarModo() {
        boolean editando = alquilerEnEdicion != null;
        etiquetaModo.setText(
                editando
                        ? "Editando ID " + alquilerEnEdicion.getIdAlquiler()
                        : "Modo: nuevo alquiler");
        // PASO (actualizarModo): Habilita/deshabilita el boton segun estado de la pantalla.
        botonRegistrar.setEnabled(!ocupado && !editando);
        // PASO (actualizarModo): Habilita/deshabilita el boton segun estado de la pantalla.
        botonCargarEdicion.setEnabled(!ocupado);
        // PASO (actualizarModo): Habilita/deshabilita el boton segun estado de la pantalla.
        botonModificar.setEnabled(!ocupado && editando);
        // PASO (actualizarModo): Habilita/deshabilita el boton segun estado de la pantalla.
        botonNuevo.setEnabled(!ocupado && editando);
    }

    // EXPLICACIÓN DEL MÉTODO: Lee cliente, cancha, fecha y horas; solicita el registro al controlador y muestra el total
    //     devuelto.
    private void registrar() {
        // PASO (registrar): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (registrar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // PASO (registrar): Comienza un bloque protegido: permite controlar errores y liberar recursos.
        try {
            Cliente cliente = (Cliente) comboClientes.getSelectedItem();
            Cancha cancha = (Cancha) comboCanchas.getSelectedItem();

            // PASO (registrar): Evita seguir si falta un dato necesario para la operacion.
            if (cliente == null) {
                // PASO (registrar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                throw new IllegalArgumentException(
                        "Primero registre un cliente y consulte nuevamente.");
            }

            // PASO (registrar): Evita seguir si falta un dato necesario para la operacion.
            if (cancha == null) {
                // PASO (registrar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                throw new IllegalArgumentException(
                        "No hay canchas habilitadas para registrar alquileres.");
            }

            LocalDate fecha = leerFecha();
            LocalTime inicio = leerHora(campoInicio, "inicial");
            LocalTime fin = leerHora(campoFin, "final");

            // PASO (registrar): Ejecuta el trabajo mediante un SwingWorker, separado del hilo de la interfaz.
            ejecutar(
                    () ->
                            controlador.registrar(
                                    cancha.getIdCancha(),
                                    cliente.getIdCliente(),
                                    fecha,
                                    inicio,
                                    fin),
                    registrado -> {
                        JOptionPane.showMessageDialog(
                                this,
                                "Alquiler registrado correctamente.\n"
                                        + "ID: "
                                        + registrado.getIdAlquiler()
                                        + "\nMonto total: S/ "
                                        + registrado.getMontoTotal().toPlainString(),
                                "Registro de alquiler",
                                JOptionPane.INFORMATION_MESSAGE);

                        consultar();
                    },
                    "Registrando alquiler...");

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    // EXPLICACIÓN DEL MÉTODO: Exige seleccionar un alquiler confirmado, solicita confirmacion y cambia su estado mediante el
    //     controlador.
    private void cancelar() {
        // PASO (cancelar): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (cancelar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        int filaVista = tabla.getSelectedRow();

        // PASO (cancelar): Comprueba una condicion de negocio o controla un caso de error.
        if (filaVista < 0) {
            mostrarError("Seleccione un alquiler de la tabla.");
            // PASO (cancelar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // Convierte la posición cuando la tabla está ordenada.
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Alquiler seleccionado = alquileres.get(filaModelo);

        // PASO (cancelar): Solo los alquileres confirmados pueden modificarse o cancelarse.
        if (seleccionado.getEstado() != EstadoAlquiler.CONFIRMADO) {
            mostrarError("Solo puede cancelar alquileres confirmados.");
            // PASO (cancelar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        int respuesta =
                // PASO (cancelar): Muestra una ventana de informacion, confirmacion o advertencia.
                JOptionPane.showConfirmDialog(
                        this,
                        "¿Desea cancelar el alquiler "
                                + seleccionado.getIdAlquiler()
                                + " del "
                                + seleccionado.getFecha().format(FORMATO_FECHA)
                                + " a las "
                                + seleccionado.getHoraInicio().format(FORMATO_HORA)
                                + "?\nEl registro se conservará en el historial.",
                        "Confirmar cancelación",
                        JOptionPane.YES_NO_OPTION);

        // PASO (cancelar): Respeta la decision de cancelar el dialogo: no cambia MySQL.
        if (respuesta != JOptionPane.YES_OPTION) {
            // PASO (cancelar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // PASO (cancelar): Ejecuta el trabajo mediante un SwingWorker, separado del hilo de la interfaz.
        ejecutar(
                () -> {
                    controlador.cancelar(seleccionado.getIdAlquiler());
                    return Boolean.TRUE;
                },
                resultado -> {
                    JOptionPane.showMessageDialog(
                            this,
                            "Alquiler cancelado correctamente.",
                            "Cancelación",
                            JOptionPane.INFORMATION_MESSAGE);

                    consultar();
                },
                "Cancelando alquiler...");
    }

    // EXPLICACIÓN DEL MÉTODO: Solicita terminar un alquiler, dejando que el controlador/DAO comprueben si ya concluyo su
    //     horario.
    private void completar() {
        // PASO (completar): Si hay otra operacion en curso, no procesa un segundo clic.
        if (ocupado) {
            // PASO (completar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        int filaVista = tabla.getSelectedRow();

        // PASO (completar): Comprueba una condicion de negocio o controla un caso de error.
        if (filaVista < 0) {
            mostrarError("Seleccione un alquiler de la tabla.");
            // PASO (completar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Alquiler seleccionado = alquileres.get(filaModelo);

        // PASO (completar): Solo los alquileres confirmados pueden modificarse o cancelarse.
        if (seleccionado.getEstado() != EstadoAlquiler.CONFIRMADO) {
            mostrarError("Solo puede completar alquileres confirmados.");
            // PASO (completar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        int respuesta =
                // PASO (completar): Muestra una ventana de informacion, confirmacion o advertencia.
                JOptionPane.showConfirmDialog(
                        this,
                        "¿Confirma que terminó el servicio del alquiler "
                                + seleccionado.getIdAlquiler()
                                + "?\nFecha: "
                                + seleccionado.getFecha().format(FORMATO_FECHA)
                                + "\nHorario: "
                                + seleccionado.getHoraInicio().format(FORMATO_HORA)
                                + " a "
                                + seleccionado.getHoraFin().format(FORMATO_HORA)
                                + "\nEsta acción no registra un pago.",
                        "Completar alquiler",
                        JOptionPane.YES_NO_OPTION);

        // PASO (completar): Respeta la decision de cancelar el dialogo: no cambia MySQL.
        if (respuesta != JOptionPane.YES_OPTION) {
            // PASO (completar): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        // PASO (completar): Ejecuta el trabajo mediante un SwingWorker, separado del hilo de la interfaz.
        ejecutar(
                () -> {
                    controlador.completar(seleccionado.getIdAlquiler());
                    return Boolean.TRUE;
                },
                resultado -> {
                    JOptionPane.showMessageDialog(
                            this,
                            "Alquiler marcado como completado.",
                            "Alquiler completado",
                            JOptionPane.INFORMATION_MESSAGE);

                    consultar();
                },
                "Completando alquiler...");
    }

    /** Interpreta la entrada con formato estricto y comunica fechas inválidas. */
    // EXPLICACIÓN DEL MÉTODO: Transforma el texto dd/MM/aaaa en LocalDate y rechaza fechas imposibles o mal escritas.
    private LocalDate leerFecha() {
        String texto = campoFecha.getText().strip();

        // PASO (leerFecha): Comprueba una condicion de negocio o controla un caso de error.
        if (!texto.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}")) {
            // PASO (leerFecha): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Ingrese la fecha con formato dd/MM/aaaa.");
        }

        // PASO (leerFecha): Comienza un bloque protegido: permite controlar errores y liberar recursos.
        try {
            // PASO (leerFecha): Devuelve el dato calculado o la respuesta de esta operacion.
            return LocalDate.parse(texto, FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            // PASO (leerFecha): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("La fecha ingresada no es válida.");
        }
    }

    /** Convierte el texto del formulario a LocalTime o informa un formato inválido. */
    // EXPLICACIÓN DEL MÉTODO: Transforma el texto HH:mm en LocalTime y comunica errores legibles.
    private LocalTime leerHora(JTextField campo, String nombre) {
        String texto = campo.getText().strip();

        // PASO (leerHora): Comprueba una condicion de negocio o controla un caso de error.
        if (!texto.matches("[0-9]{2}:[0-9]{2}")) {
            // PASO (leerHora): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "Ingrese la hora " + nombre + " con formato HH:mm, por ejemplo 18:00.");
        }

        // PASO (leerHora): Comienza un bloque protegido: permite controlar errores y liberar recursos.
        try {
            // PASO (leerHora): Devuelve el dato calculado o la respuesta de esta operacion.
            return LocalTime.parse(texto, FORMATO_HORA);
        } catch (DateTimeParseException e) {
            // PASO (leerHora): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("La hora " + nombre + " no es válida.");
        }
    }

    /**
     * Ejecuta el trabajo en segundo plano y atiende el resultado en el hilo de eventos de Swing.
     */
    // EXPLICACIÓN DEL MÉTODO: Usa SwingWorker: ejecuta SQL lejos del hilo grafico y actualiza la interfaz al terminar.
    private <T> void ejecutar(Callable<T> tarea, Consumer<T> alTerminar, String texto) {

        establecerOcupado(true);
        mensaje.setText(texto);

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                // PASO (ejecutar): Devuelve el dato calculado o la respuesta de esta operacion.
                return tarea.call();
            }

            @Override
            protected void done() {
                T resultado;

                // PASO (ejecutar): Comienza un bloque protegido: permite controlar errores y liberar recursos.
                try {
                    resultado = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La operación fue interrumpida.");
                    // PASO (ejecutar): Devuelve el dato calculado o la respuesta de esta operacion.
                    return;
                } catch (ExecutionException e) {
                    manejarError(e.getCause());
                    // PASO (ejecutar): Devuelve el dato calculado o la respuesta de esta operacion.
                    return;
                } finally {
                    establecerOcupado(false);
                }

                // Los controles se habilitan antes de iniciar
                // una nueva consulta desde esta acción.
                alTerminar.accept(resultado);
            }
        }.execute();
    }

    /** Sincroniza los controles con el estado de la tarea para evitar acciones simultáneas. */
    // EXPLICACIÓN DEL MÉTODO: Evita botones repetidos mientras hay una consulta o modificacion en curso.
    private void establecerOcupado(boolean valor) {
        ocupado = valor;

        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        comboClientes.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        comboCanchas.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        campoFecha.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        campoInicio.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        campoFin.setEnabled(!valor);

        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        botonConsultar.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        botonDisponibilidad.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        botonCancelar.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        botonCompletar.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        botonCerrar.setEnabled(!valor);
        // PASO (establecerOcupado): Habilita/deshabilita el boton segun estado de la pantalla.
        tabla.setEnabled(!valor);
        actualizarModo();

        setCursor(Cursor.getPredefinedCursor(valor ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /** Distingue errores de validación o permisos de los fallos técnicos de la operación. */
    // EXPLICACIÓN DEL MÉTODO: Presenta mensajes segun la causa real de un fallo.
    private void manejarError(Throwable causa) {
        // PASO (manejarError): Comprueba una condicion de negocio o controla un caso de error.
        if (causa instanceof IllegalArgumentException || causa instanceof SecurityException) {

            mostrarError(causa.getMessage());
            // PASO (manejarError): Devuelve el dato calculado o la respuesta de esta operacion.
            return;
        }

        mostrarError(
                "No se pudo completar la operación. "
                        + "Revise el detalle en la consola de IntelliJ.");

        // PASO (manejarError): Comprueba una condicion de negocio o controla un caso de error.
        if (causa != null) {
            causa.printStackTrace();
        }
    }

    /** Presenta el mensaje de error al usuario mediante un diálogo. */
    // EXPLICACIÓN DEL MÉTODO: Muestra una advertencia al usuario sin cerrar la ventana.
    private void mostrarError(String texto) {
        mensaje.setText(texto);

        // PASO (mostrarError): Muestra una ventana de informacion, confirmacion o advertencia.
        JOptionPane.showMessageDialog(this, texto, "Alquileres", JOptionPane.WARNING_MESSAGE);
    }

    /** Cierra la ventana cuando no hay una operación en curso. */
    // EXPLICACIÓN DEL MÉTODO: Cierra la ventana de alquileres cuando no se esta ejecutando una operacion.
    private void cerrar() {
        // PASO (cerrar): Comprueba una condicion de negocio o controla un caso de error.
        if (!ocupado) {
            dispose();
        }
    }
}