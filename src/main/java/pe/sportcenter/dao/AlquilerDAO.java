package pe.sportcenter.dao;

import pe.sportcenter.modelo.Alquiler;
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.CanchaResumen;
import pe.sportcenter.modelo.IngresoPorDiaSemana;
import pe.sportcenter.modelo.OcupacionCancha;
import pe.sportcenter.modelo.ResumenAlquiler;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Contrato de persistencia de alquileres, disponibilidad e indicadores por periodo. En los filtros
 * opcionales, idCancha null representa todas las canchas.
 // [OBJETIVO 3 - EXPLICACION] Interfaz: declara que operaciones JDBC debe ofrecer una implementacion, sin escribir SQL aqui.
 */
public interface AlquilerDAO {

    // RF10: ingresos acumulados por día de la semana.
    // [OBJETIVO 3 - EXPLICACION] Agrupa montos validos por dia de semana y filtra por cancha si hay ID.
    // idCancha = null significa todas las canchas.
    List<IngresoPorDiaSemana> resumirIngresosPorDiaSemana(
            LocalDate desde, LocalDate hasta, Integer idCancha) throws SQLException, IOException;

    List<Alquiler> listarPorFecha(LocalDate fecha) throws SQLException, IOException;

    Alquiler registrar(
            int idCancha,
            int idCliente,
            // [OBJETIVO 3 - EXPLICACION] Contrato para registrar una reserva nueva, usado principalmente en el objetivo 2.
            int idUsuario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException;

    boolean cancelar(int idAlquiler) throws SQLException, IOException;

    // RF10: resumen por fechas de todas las canchas.
    List<ResumenAlquiler> resumirPorFechas(LocalDate desde, LocalDate hasta)
            throws SQLException, IOException;
// [OBJETIVO 3 - EXPLICACION] Obtiene resumen por fecha para todas las canchas.

    // RF10: resumen por fechas y cancha específica.
    // null significa todas las canchas.
    List<ResumenAlquiler> resumirPorFechas(LocalDate desde, LocalDate hasta, Integer idCancha)
            throws SQLException, IOException;
// [OBJETIVO 3 - EXPLICACION] Resumen por estado y cancha opcional; null indica todas.

    // RF10: cantidad de alquileres por cancha.
    // Incluye confirmados y completados.
    // Excluye cancelados.
    List<CanchaResumen> resumirAlquileresPorCancha(LocalDate desde, LocalDate hasta)
            throws SQLException, IOException;
// [OBJETIVO 3 - EXPLICACION] Cuenta alquileres por cancha para identificar la ganadora general del periodo.

    boolean completar(int idAlquiler, LocalDateTime ahora) throws SQLException, IOException;

    // RF06: consultar canchas libres por fecha y horario.
    List<Cancha> listarDisponibles(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin)
            throws SQLException, IOException;

    // RF07 y RF08: modificar alquiler y recalcular importe.
    Alquiler reprogramar(
            int idAlquiler,
            int idCancha,
            int idCliente,
            LocalDate fecha,
            LocalTime horaInicio,
            // [OBJETIVO 3 - EXPLICACION] Reprogramacion de alquileres, que se explico en la conclusion 2.
            LocalTime horaFin)
            throws SQLException, IOException;

    // RF10: consultar la ocupación por cancha y periodo.
    // idCancha = null significa todas las canchas.
    // [OBJETIVO 3 - EXPLICACION] Devuelve horas reservadas y capacidad teorica por cancha en el periodo.
    List<OcupacionCancha> consultarOcupacion(LocalDate desde, LocalDate hasta, Integer idCancha)
            throws SQLException, IOException;
}
