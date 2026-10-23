package pe.sportcenter.controlador;

import pe.sportcenter.dao.AlquilerDAO;
import pe.sportcenter.dao.CanchaDAO;
import pe.sportcenter.dao.ClienteDAO;
import pe.sportcenter.modelo.Alquiler;
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.Cliente;
import pe.sportcenter.modelo.EstadoCancha;
import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;
import pe.sportcenter.modelo.ResumenAlquiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    public List<Cancha> listarCanchasHabilitadas()
            throws SQLException, IOException {

        exigirPermiso();

        // El horario se comprobará al registrar.
        // Aquí solo filtramos el estado general de la cancha.
        return canchaDAO.listarTodas().stream()
                .filter(cancha ->
                        cancha.getEstado() == EstadoCancha.DISPONIBLE)
                .toList();
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

        exigirPermiso();

        validarFecha(desde);
        validarFecha(hasta);

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        return alquilerDAO.resumirPorFechas(desde, hasta);
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

        if (horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException(
                    "Las horas de inicio y fin son obligatorias.");
        }

        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                    "La hora final debe ser posterior a la inicial "
                            + "dentro del mismo día.");
        }

        if (horaInicio.getSecond() != 0
                || horaFin.getSecond() != 0
                || horaInicio.getNano() != 0
                || horaFin.getNano() != 0) {

            throw new IllegalArgumentException(
                    "Ingrese las horas sin segundos.");
        }
    }
}