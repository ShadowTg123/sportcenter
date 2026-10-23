package pe.sportcenter.vista;

import pe.sportcenter.controlador.AlquilerControlador;
import pe.sportcenter.modelo.Alquiler;
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.Cliente;
import pe.sportcenter.modelo.EstadoAlquiler;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class AlquilerVista extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm")
                    .withResolverStyle(ResolverStyle.STRICT);

    private final AlquilerControlador controlador;

    private final JComboBox<Cliente> comboClientes = new JComboBox<>();
    private final JComboBox<Cancha> comboCanchas = new JComboBox<>();

    private final JTextField campoFecha = new JTextField(12);
    private final JTextField campoInicio = new JTextField("18:00", 8);
    private final JTextField campoFin = new JTextField("19:00", 8);

    private final JButton botonRegistrar = new JButton("Registrar alquiler");
    private final JButton botonConsultar = new JButton("Consultar fecha");
    private final JButton botonCancelar = new JButton("Cancelar alquiler");
    private final JButton botonCompletar = new JButton("Completar alquiler");
    private final JButton botonCerrar = new JButton("Cerrar");
    private final JButton botonCargarEdicion =
            new JButton("Cargar para modificar");
    private final JButton botonModificar =
            new JButton("Modificar alquiler");
    private final JButton botonNuevo =
            new JButton("Nuevo alquiler");
    private final JLabel etiquetaModo =
            new JLabel("Modo: nuevo alquiler");
    private final JButton botonDisponibilidad =
            new JButton("Ver disponibilidad");

    private final JLabel mensaje = new JLabel(" ");

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(
                    new Object[]{
                            "ID", "Cancha", "Cliente", "Fecha",
                            "Inicio", "Fin", "Monto S/", "Estado"
                    }, 0) {

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

    private final JTable tabla = new JTable(modeloTabla);

    private List<Alquiler> alquileres = List.of();
    private Alquiler alquilerEnEdicion;
    private boolean ocupado;

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    // Agrupa los resultados de una consulta.
    private record Datos(
            List<Cliente> clientes,
            List<Cancha> canchas,
            List<Alquiler> alquileres) {
    }

    public AlquilerVista(
            JFrame propietario,
            AlquilerControlador controlador) {

        super(propietario, "Sport Center - Alquileres", true);
        this.controlador = controlador;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1100, 660);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(propietario);

        campoFecha.setText(
                LocalDate.now(ZoneId.of("America/Lima"))
                        .plusDays(1)
                        .format(FORMATO_FECHA)
        );

        configurarListas();

        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        contenido.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel formulario = new JPanel(new GridLayout(0, 2, 10, 8));
        formulario.setBorder(
                BorderFactory.createTitledBorder("Datos del alquiler"));

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

        botonConsultar.addActionListener(e -> consultar());
        botonDisponibilidad.addActionListener(
                e -> consultarDisponibilidad());
        botonRegistrar.addActionListener(e -> registrar());
        botonCargarEdicion.addActionListener(e -> cargarParaModificar());
        botonModificar.addActionListener(e -> modificar());
        botonNuevo.addActionListener(e -> prepararNuevoAlquiler());
        botonCancelar.addActionListener(e -> cancelar());
        botonCompletar.addActionListener(e -> completar());
        botonCerrar.addActionListener(e -> cerrar());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrar();
            }
        });

        actualizarModo();
        consultar();
    }

    private void configurarListas() {
        comboClientes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> lista, Object valor, int indice,
                    boolean seleccionado, boolean tieneFoco) {

                super.getListCellRendererComponent(
                        lista, valor, indice, seleccionado, tieneFoco);

                if (valor instanceof Cliente cliente) {
                    setText(cliente.getNombre() + " | DNI: "
                            + cliente.getDni());
                }

                return this;
            }
        });

        comboCanchas.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> lista, Object valor, int indice,
                    boolean seleccionado, boolean tieneFoco) {

                super.getListCellRendererComponent(
                        lista, valor, indice, seleccionado, tieneFoco);

                if (valor instanceof Cancha cancha) {
                    setText(cancha.getNombre() + " | S/ "
                            + cancha.getTarifaHora().toPlainString()
                            + " por hora");
                }

                return this;
            }
        });
    }

    private void consultar() {
        if (ocupado) {
            return;
        }

        final LocalDate fecha;

        try {
            fecha = leerFecha();
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
            return;
        }

        ejecutar(
                () -> new Datos(
                        controlador.listarClientes(),
                        controlador.listarCanchasHabilitadas(),
                        controlador.listarPorFecha(fecha)
                ),
                datos -> mostrarDatos(datos, fecha),
                "Consultando alquileres..."
        );
    }

    private void mostrarDatos(Datos datos, LocalDate fecha) {
        // La consulta actualiza las listas y termina cualquier edición anterior.
        alquilerEnEdicion = null;
        tabla.clearSelection();
        comboClientes.removeAllItems();

        for (Cliente cliente : datos.clientes()) {
            comboClientes.addItem(cliente);
        }

        comboCanchas.removeAllItems();

        for (Cancha cancha : datos.canchas()) {
            comboCanchas.addItem(cancha);
        }

        alquileres = datos.alquileres();
        modeloTabla.setRowCount(0);

        for (Alquiler alquiler : alquileres) {
            String nombreCancha = datos.canchas().stream()
                    .filter(c -> c.getIdCancha() == alquiler.getIdCancha())
                    .map(Cancha::getNombre)
                    .findFirst()
                    .orElse("Cancha ID " + alquiler.getIdCancha());

            String nombreCliente = datos.clientes().stream()
                    .filter(c -> c.getIdCliente() == alquiler.getIdCliente())
                    .map(Cliente::getNombre)
                    .findFirst()
                    .orElse("Cliente ID " + alquiler.getIdCliente());

            modeloTabla.addRow(new Object[]{
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
                "Fecha consultada: " + fecha.format(FORMATO_FECHA)
                        + " | Alquileres: " + alquileres.size());
    }

    // RF06: mostrar canchas libres para la fecha y horario indicados.
    private void consultarDisponibilidad() {
        if (ocupado) {
            return;
        }

        try {
            LocalDate fecha = leerFecha();
            LocalTime inicio = leerHora(campoInicio, "inicial");
            LocalTime fin = leerHora(campoFin, "final");

            ejecutar(
                    () -> controlador.consultarDisponibilidad(
                            fecha, inicio, fin),
                    disponibles -> {
                        String horario = fecha.format(FORMATO_FECHA)
                                + " | "
                                + inicio.format(FORMATO_HORA)
                                + " a "
                                + fin.format(FORMATO_HORA);

                        mensaje.setText(
                                "Disponibilidad consultada: " + horario
                                        + " | Canchas libres: "
                                        + disponibles.size());

                        if (disponibles.isEmpty()) {
                            JOptionPane.showMessageDialog(
                                    this,
                                    "No hay canchas disponibles para:\n"
                                            + horario,
                                    "Disponibilidad",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                            return;
                        }

                        DefaultTableModel modeloDisponibles =
                                new DefaultTableModel(
                                        new Object[]{
                                                "ID", "Cancha",
                                                "Deporte", "Tarifa S/ por hora"
                                        }, 0) {

                                    @Override
                                    public boolean isCellEditable(
                                            int fila, int columna) {
                                        return false;
                                    }
                                };

                        for (Cancha cancha : disponibles) {
                            modeloDisponibles.addRow(new Object[]{
                                    cancha.getIdCancha(),
                                    cancha.getNombre(),
                                    cancha.getDeporte(),
                                    cancha.getTarifaHora()
                            });
                        }

                        JTable tablaDisponibles =
                                new JTable(modeloDisponibles);

                        tablaDisponibles.setRowHeight(26);
                        tablaDisponibles.setFillsViewportHeight(true);

                        JScrollPane desplazamiento =
                                new JScrollPane(tablaDisponibles);

                        desplazamiento.setPreferredSize(
                                new Dimension(600, 220));

                        JPanel panel = new JPanel(
                                new BorderLayout(8, 8));

                        panel.add(
                                new JLabel("Horario consultado: " + horario),
                                BorderLayout.NORTH);

                        panel.add(
                                desplazamiento,
                                BorderLayout.CENTER);

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
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    },
                    "Consultando disponibilidad..."
            );

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    // RF07: carga un alquiler confirmado para editarlo sin crear otro registro.
    private void cargarParaModificar() {
        if (ocupado) {
            return;
        }

        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            mostrarError("Seleccione primero un alquiler de la tabla.");
            return;
        }

        // La tabla admite ordenación por columnas: convertir el índice.
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Alquiler seleccionado = alquileres.get(filaModelo);

        if (seleccionado.getEstado() != EstadoAlquiler.CONFIRMADO) {
            mostrarError("Solo se pueden modificar alquileres confirmados.");
            return;
        }

        LocalDateTime inicioOriginal = LocalDateTime.of(
                seleccionado.getFecha(), seleccionado.getHoraInicio());
        if (!inicioOriginal.isAfter(LocalDateTime.now(ZONA))) {
            mostrarError("No se puede modificar un alquiler que ya comenzó.");
            return;
        }

        boolean clienteEncontrado = seleccionarCliente(
                seleccionado.getIdCliente());
        boolean canchaEncontrada = seleccionarCancha(
                seleccionado.getIdCancha());

        campoFecha.setText(seleccionado.getFecha().format(FORMATO_FECHA));
        campoInicio.setText(seleccionado.getHoraInicio().format(FORMATO_HORA));
        campoFin.setText(seleccionado.getHoraFin().format(FORMATO_HORA));

        alquilerEnEdicion = seleccionado;
        actualizarModo();
        mensaje.setText("Editando alquiler ID " + seleccionado.getIdAlquiler()
                + ". Cambie los datos y pulse 'Modificar alquiler'.");

        // Una cancha antigua puede haber sido desactivada posteriormente.
        if (!clienteEncontrado || !canchaEncontrada) {
            JOptionPane.showMessageDialog(
                    this,
                    "Alguno de los datos originales ya no está disponible.\n"
                            + "Seleccione una cancha y un cliente válidos antes de modificar.",
                    "Revisar selección",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private boolean seleccionarCliente(int idCliente) {
        for (int i = 0; i < comboClientes.getItemCount(); i++) {
            if (comboClientes.getItemAt(i).getIdCliente() == idCliente) {
                comboClientes.setSelectedIndex(i);
                return true;
            }
        }
        comboClientes.setSelectedIndex(-1);
        return false;
    }

    private boolean seleccionarCancha(int idCancha) {
        for (int i = 0; i < comboCanchas.getItemCount(); i++) {
            if (comboCanchas.getItemAt(i).getIdCancha() == idCancha) {
                comboCanchas.setSelectedIndex(i);
                return true;
            }
        }
        comboCanchas.setSelectedIndex(-1);
        return false;
    }

    // RF07 y RF08: modifica el registro actual y recalcula su importe.
    private void modificar() {
        if (ocupado) {
            return;
        }

        if (alquilerEnEdicion == null) {
            mostrarError("Seleccione un alquiler y pulse 'Cargar para modificar'.");
            return;
        }

        try {
            Cancha cancha = (Cancha) comboCanchas.getSelectedItem();
            Cliente cliente = (Cliente) comboClientes.getSelectedItem();

            if (cancha == null || cliente == null) {
                throw new IllegalArgumentException(
                        "Seleccione una cancha y un cliente válidos.");
            }

            LocalDate fecha = leerFecha();
            LocalTime inicio = leerHora(campoInicio, "inicial");
            LocalTime fin = leerHora(campoFin, "final");
            final int idAlquiler = alquilerEnEdicion.getIdAlquiler();

            int confirmacion = JOptionPane.showConfirmDialog(
                    this,
                    "¿Modificar el alquiler ID " + idAlquiler + "?\n"
                            + "Cancha: " + cancha.getNombre() + "\n"
                            + "Cliente: " + cliente.getNombre() + "\n"
                            + "Fecha: " + fecha.format(FORMATO_FECHA) + "\n"
                            + "Horario: " + inicio.format(FORMATO_HORA)
                            + " a " + fin.format(FORMATO_HORA) + "\n"
                            + "El importe se recalculará automáticamente.",
                    "Confirmar modificación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirmacion != JOptionPane.YES_OPTION) {
                return;
            }

            ejecutar(
                    () -> controlador.reprogramar(
                            idAlquiler,
                            cancha.getIdCancha(),
                            cliente.getIdCliente(),
                            fecha, inicio, fin),
                    modificado -> {
                        JOptionPane.showMessageDialog(
                                this,
                                "Alquiler modificado correctamente.\n"
                                        + "ID conservado: " + modificado.getIdAlquiler()
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
    private void prepararNuevoAlquiler() {
        if (ocupado) {
            return;
        }
        alquilerEnEdicion = null;
        tabla.clearSelection();
        campoFecha.setText(LocalDate.now(ZONA)
                .plusDays(1).format(FORMATO_FECHA));
        campoInicio.setText("18:00");
        campoFin.setText("19:00");
        if (comboClientes.getItemCount() > 0) {
            comboClientes.setSelectedIndex(0);
        }
        if (comboCanchas.getItemCount() > 0) {
            comboCanchas.setSelectedIndex(0);
        }
        actualizarModo();
        mensaje.setText("Modo de registro: puede crear un nuevo alquiler.");
    }

    private void actualizarModo() {
        boolean editando = alquilerEnEdicion != null;
        etiquetaModo.setText(editando
                ? "Editando ID " + alquilerEnEdicion.getIdAlquiler()
                : "Modo: nuevo alquiler");
        botonRegistrar.setEnabled(!ocupado && !editando);
        botonCargarEdicion.setEnabled(!ocupado);
        botonModificar.setEnabled(!ocupado && editando);
        botonNuevo.setEnabled(!ocupado && editando);
    }

    private void registrar() {
        if (ocupado) {
            return;
        }

        try {
            Cliente cliente = (Cliente) comboClientes.getSelectedItem();
            Cancha cancha = (Cancha) comboCanchas.getSelectedItem();

            if (cliente == null) {
                throw new IllegalArgumentException(
                        "Primero registre un cliente y consulte nuevamente.");
            }

            if (cancha == null) {
                throw new IllegalArgumentException(
                        "No hay canchas habilitadas para registrar alquileres.");
            }

            LocalDate fecha = leerFecha();
            LocalTime inicio = leerHora(campoInicio, "inicial");
            LocalTime fin = leerHora(campoFin, "final");

            ejecutar(
                    () -> controlador.registrar(
                            cancha.getIdCancha(),
                            cliente.getIdCliente(),
                            fecha,
                            inicio,
                            fin
                    ),
                    registrado -> {
                        JOptionPane.showMessageDialog(
                                this,
                                "Alquiler registrado correctamente.\n"
                                        + "ID: " + registrado.getIdAlquiler()
                                        + "\nMonto total: S/ "
                                        + registrado.getMontoTotal()
                                        .toPlainString(),
                                "Registro de alquiler",
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        consultar();
                    },
                    "Registrando alquiler..."
            );

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cancelar() {
        if (ocupado) {
            return;
        }

        int filaVista = tabla.getSelectedRow();

        if (filaVista < 0) {
            mostrarError("Seleccione un alquiler de la tabla.");
            return;
        }

        // Convierte la posición cuando la tabla está ordenada.
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Alquiler seleccionado = alquileres.get(filaModelo);

        if (seleccionado.getEstado() != EstadoAlquiler.CONFIRMADO) {
            mostrarError("Solo puede cancelar alquileres confirmados.");
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Desea cancelar el alquiler "
                        + seleccionado.getIdAlquiler()
                        + " del "
                        + seleccionado.getFecha().format(FORMATO_FECHA)
                        + " a las "
                        + seleccionado.getHoraInicio().format(FORMATO_HORA)
                        + "?\nEl registro se conservará en el historial.",
                "Confirmar cancelación",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

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
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    consultar();
                },
                "Cancelando alquiler..."
        );
    }

    private void completar() {
        if (ocupado) {
            return;
        }

        int filaVista = tabla.getSelectedRow();

        if (filaVista < 0) {
            mostrarError("Seleccione un alquiler de la tabla.");
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Alquiler seleccionado = alquileres.get(filaModelo);

        if (seleccionado.getEstado() != EstadoAlquiler.CONFIRMADO) {
            mostrarError("Solo puede completar alquileres confirmados.");
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
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
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

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
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    consultar();
                },
                "Completando alquiler..."
        );
    }

    private LocalDate leerFecha() {
        String texto = campoFecha.getText().strip();

        if (!texto.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}")) {
            throw new IllegalArgumentException(
                    "Ingrese la fecha con formato dd/MM/aaaa.");
        }

        try {
            return LocalDate.parse(texto, FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "La fecha ingresada no es válida.");
        }
    }

    private LocalTime leerHora(JTextField campo, String nombre) {
        String texto = campo.getText().strip();

        if (!texto.matches("[0-9]{2}:[0-9]{2}")) {
            throw new IllegalArgumentException(
                    "Ingrese la hora " + nombre
                            + " con formato HH:mm, por ejemplo 18:00.");
        }

        try {
            return LocalTime.parse(texto, FORMATO_HORA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "La hora " + nombre + " no es válida.");
        }
    }

    private <T> void ejecutar(
            Callable<T> tarea,
            Consumer<T> alTerminar,
            String texto) {

        establecerOcupado(true);
        mensaje.setText(texto);

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarea.call();
            }

            @Override
            protected void done() {
                T resultado;

                try {
                    resultado = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La operación fue interrumpida.");
                    return;
                } catch (ExecutionException e) {
                    manejarError(e.getCause());
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

    private void establecerOcupado(boolean valor) {
        ocupado = valor;

        comboClientes.setEnabled(!valor);
        comboCanchas.setEnabled(!valor);
        campoFecha.setEnabled(!valor);
        campoInicio.setEnabled(!valor);
        campoFin.setEnabled(!valor);

        botonConsultar.setEnabled(!valor);
        botonDisponibilidad.setEnabled(!valor);
        botonCancelar.setEnabled(!valor);
        botonCompletar.setEnabled(!valor);
        botonCerrar.setEnabled(!valor);
        tabla.setEnabled(!valor);
        actualizarModo();

        setCursor(Cursor.getPredefinedCursor(
                valor ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void manejarError(Throwable causa) {
        if (causa instanceof IllegalArgumentException
                || causa instanceof SecurityException) {

            mostrarError(causa.getMessage());
            return;
        }

        mostrarError(
                "No se pudo completar la operación. "
                        + "Revise el detalle en la consola de IntelliJ.");

        if (causa != null) {
            causa.printStackTrace();
        }
    }

    private void mostrarError(String texto) {
        mensaje.setText(texto);

        JOptionPane.showMessageDialog(
                this,
                texto,
                "Alquileres",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void cerrar() {
        if (!ocupado) {
            dispose();
        }
    }
}
