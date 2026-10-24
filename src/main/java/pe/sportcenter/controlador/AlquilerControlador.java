package pe.sportcenter.controlador;

import pe.sportcenter.dao.AlquilerDAO;
import pe.sportcenter.dao.CanchaDAO;
import pe.sportcenter.dao.ClienteDAO;
import pe.sportcenter.modelo.Alquiler;
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.CanchaResumen;
import pe.sportcenter.modelo.IngresoPorDiaSemana;
import pe.sportcenter.modelo.Cliente;
import pe.sportcenter.modelo.EstadoCancha;
import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;
import pe.sportcenter.modelo.ResumenAlquiler;
import pe.sportcenter.util.ReglasAlquiler;
import pe.sportcenter.modelo.OcupacionCancha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public class AlquilerControlador {



    private static final Logger LOG =
            LoggerFactory.getLogger(AlquilerControlador.class);

    private static final ZoneId ZONA =
            ZoneId.of("America/Lima");

    private final AlquilerDAO alquilerDAO;
    private final CanchaDAO canchaDAO;
    private final ClienteDAO clienteDAO;
    private final AutenticacionControlador autenticacion;

    public AlquilerControlador(
            AlquilerDAO alquilerDAO,
            CanchaDAO canchaDAO,
            ClienteDAO clienteDAO,
            AutenticacionControlador autenticacion) {

        this.alquilerDAO = Objects.requireNonNull(alquilerDAO);
        this.canchaDAO = Objects.requireNonNull(canchaDAO);
        this.clienteDAO = Objects.requireNonNull(clienteDAO);
        this.autenticacion = Objects.requireNonNull(autenticacion);
    }

    // RF10: incluye canchas inactivas para consultar su historial.
    public List<Cancha> listarCanchasParaReporte()
            throws SQLException, IOException {

        exigirPermiso();
        return canchaDAO.listarTodas();
    }

    public List<Cancha> listarCanchasHabilitadas()
            throws SQLException, IOException {

        exigirPermiso();

        return canchaDAO.listarTodas().stream()
                .filter(cancha ->
                        cancha.getEstado() == EstadoCancha.DISPONIBLE
                                || cancha.getEstado() == EstadoCancha.ALQUILADA)
                .toList();
    }

    // RF06: consultar disponibilidad por fecha y horario.
    public List<Cancha> consultarDisponibilidad(
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException {

        exigirPermiso();

        validarFecha(fecha);
        validarHorario(horaInicio, horaFin);

        return alquilerDAO.listarDisponibles(
                fecha, horaInicio, horaFin);
    }

    public List<Cliente> listarClientes()
            throws SQLException, IOException {

        exigirPermiso();
        return clienteDAO.listarTodos();
    }

    public List<Alquiler> listarPorFecha(LocalDate fecha)
            throws SQLException, IOException {

        exigirPermiso();
        validarFecha(fecha);

        // Permite consultar también fechas pasadas.
        return alquilerDAO.listarPorFecha(fecha);
    }


    public List<ResumenAlquiler> resumirPorFechas(
            LocalDate desde,
            LocalDate hasta)
            throws SQLException, IOException {

        return resumirPorFechas(desde, hasta, null);
    }

    // RF10: resumen por fechas y cancha.
    public List<ResumenAlquiler> resumirPorFechas(
            LocalDate desde,
            LocalDate hasta,
            Integer idCancha)
            throws SQLException, IOException {

        exigirPermiso();

        validarFecha(desde);
        validarFecha(hasta);

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        if (idCancha != null && idCancha <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione una cancha válida.");
        }

        return alquilerDAO.resumirPorFechas(
                desde, hasta, idCancha);
    }



    // RF10: identifica las canchas con mayor cantidad de alquileres
    // confirmados y completados en el periodo. Conserva los empates.
    public List<CanchaResumen> obtenerCanchasMasAlquiladas(
            LocalDate desde,
            LocalDate hasta)
            throws SQLException, IOException {

        exigirPermiso();
        validarFecha(desde);
        validarFecha(hasta);

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        List<CanchaResumen> resumenes =
                alquilerDAO.resumirAlquileresPorCancha(desde, hasta);

        if (resumenes.isEmpty()) {
            return List.of();
        }

        long cantidadMaxima = resumenes.get(0).getCantidadAlquileres();

        return resumenes.stream()
                .filter(resumen ->
                        resumen.getCantidadAlquileres() == cantidadMaxima)
                .toList();
    }

    // RF10: dia(s) con mayores ingresos acumulados del periodo y cancha.
    // Incluye todos los empates en el importe maximo.
    public List<IngresoPorDiaSemana> obtenerDiasConMayoresIngresos(
            LocalDate desde,
            LocalDate hasta,
            Integer idCancha)
            throws SQLException, IOException {

        exigirPermiso();
        validarFecha(desde);
        validarFecha(hasta);

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        if (idCancha != null && idCancha <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione una cancha válida.");
        }

        List<IngresoPorDiaSemana> ingresos =
                alquilerDAO.resumirIngresosPorDiaSemana(
                        desde, hasta, idCancha);

        if (ingresos.isEmpty()) {
            return List.of();
        }

        BigDecimal montoMaximo = ingresos.get(0).montoTotal();

        return ingresos.stream()
                .filter(ingreso ->
                        ingreso.montoTotal().compareTo(montoMaximo) == 0)
                .toList();
    }

    public Alquiler registrar(
            int idCancha,
            int idCliente,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException {

        Usuario usuario = exigirPermiso();

        if (idCancha <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione una cancha.");
        }

        if (idCliente <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un cliente.");
        }

        validarFecha(fecha);
        validarHorario(horaInicio, horaFin);

        LocalDateTime inicio =
                LocalDateTime.of(fecha, horaInicio);

        if (!inicio.isAfter(LocalDateTime.now(ZONA))) {
            throw new IllegalArgumentException(
                    "El alquiler debe comenzar en una fecha "
                            + "y hora futuras.");
        }

        // El usuario responsable se obtiene de la sesión.
        Alquiler registrado = alquilerDAO.registrar(
                idCancha,
                idCliente,
                usuario.getIdUsuario(),
                fecha,
                horaInicio,
                horaFin
        );

        LOG.info(
                "Alquiler registrado: idAlquiler={}, idCancha={}, "
                        + "idUsuario={}, montoTotal={}",
                registrado.getIdAlquiler(),
                registrado.getIdCancha(),
                usuario.getIdUsuario(),
                registrado.getMontoTotal()
        );

        return registrado;
    }

    // RF07 y RF08: modificar un alquiler y recalcular su importe.
    public Alquiler reprogramar(
            int idAlquiler,
            int idCancha,
            int idCliente,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException {

        Usuario usuario = exigirPermiso();

        if (idAlquiler <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un alquiler válido.");
        }

        if (idCancha <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione una cancha.");
        }

        if (idCliente <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un cliente.");
        }

        validarFecha(fecha);
        validarHorario(horaInicio, horaFin);

        LocalDateTime nuevoInicio =
                LocalDateTime.of(fecha, horaInicio);

        if (!nuevoInicio.isAfter(LocalDateTime.now(ZONA))) {
            throw new IllegalArgumentException(
                    "El nuevo horario debe comenzar en el futuro.");
        }

        Alquiler modificado = alquilerDAO.reprogramar(
                idAlquiler,
                idCancha,
                idCliente,
                fecha,
                horaInicio,
                horaFin
        );

        LOG.info(
                "Alquiler reprogramado: idAlquiler={}, "
                        + "idCancha={}, idUsuario={}, montoTotal={}",
                modificado.getIdAlquiler(),
                modificado.getIdCancha(),
                usuario.getIdUsuario(),
                modificado.getMontoTotal()
        );

        return modificado;
    }

    public void cancelar(int idAlquiler)
            throws SQLException, IOException {

        Usuario usuario = exigirPermiso();

        if (idAlquiler <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un alquiler válido.");
        }

        boolean cancelado = alquilerDAO.cancelar(idAlquiler);

        if (!cancelado) {
            throw new IllegalArgumentException(
                    "No se pudo cancelar: el alquiler no existe "
                            + "o ya no está confirmado.");
        }

        LOG.info(
                "Alquiler cancelado: idAlquiler={}, idUsuario={}",
                idAlquiler,
                usuario.getIdUsuario()
        );
    }

    public void completar(int idAlquiler)
            throws SQLException, IOException {

        Usuario usuario = exigirPermiso();

        if (idAlquiler <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un alquiler válido.");
        }

        // Usa la hora de Lima, sin pedirla al usuario.
        LocalDateTime ahora = LocalDateTime.now(ZONA);

        boolean completado = alquilerDAO.completar(idAlquiler, ahora);

        if (!completado) {
            throw new IllegalArgumentException(
                    "No se pudo completar el alquiler. "
                            + "Debe existir, estar confirmado "
                            + "y haber terminado su horario.");
        }

        LOG.info(
                "Alquiler completado: idAlquiler={}, idUsuario={}",
                idAlquiler,
                usuario.getIdUsuario()
        );
    }


    private Usuario exigirPermiso() {
        Usuario usuario = autenticacion.getUsuarioActual();

        if (usuario == null) {
            throw new SecurityException(
                    "Debe iniciar sesión para gestionar alquileres.");
        }

        if (usuario.getRol() != Rol.ADMINISTRADOR
                && usuario.getRol() != Rol.RECEPCIONISTA) {

            throw new SecurityException(
                    "No tiene permisos para gestionar alquileres.");
        }

        return usuario;
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha es obligatoria.");
        }

        // Rango admitido por el tipo DATE de MySQL.
        if (fecha.getYear() < 1000 || fecha.getYear() > 9999) {
            throw new IllegalArgumentException(
                    "La fecha debe tener un año entre 1000 y 9999.");
        }
    }

    private void validarHorario(
            LocalTime horaInicio,
            LocalTime horaFin) {

        ReglasAlquiler.validarHorario(horaInicio, horaFin);
    }
    // RF10: consultar la ocupación por cancha.
    public List<OcupacionCancha> consultarOcupacion(
            LocalDate desde,
            LocalDate hasta,
            Integer idCancha)
            throws SQLException, IOException {

        exigirPermiso();

        validarFecha(desde);
        validarFecha(hasta);

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        if (idCancha != null && idCancha <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione una cancha válida.");
        }

        return alquilerDAO.consultarOcupacion(
                desde, hasta, idCancha);
    }
}