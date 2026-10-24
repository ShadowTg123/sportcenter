package pe.sportcenter.dao;

import pe.sportcenter.modelo.Alquiler;
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.CanchaResumen;
import pe.sportcenter.modelo.ResumenAlquiler;
import pe.sportcenter.modelo.IngresoPorDiaSemana;
import pe.sportcenter.modelo.OcupacionCancha;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface AlquilerDAO {

    // RF10: ingresos acumulados por día de la semana.
// idCancha = null significa todas las canchas.
    List<IngresoPorDiaSemana> resumirIngresosPorDiaSemana(
            LocalDate desde,
            LocalDate hasta,
            Integer idCancha)
            throws SQLException, IOException;

    List<Alquiler> listarPorFecha(LocalDate fecha)
            throws SQLException, IOException;

    Alquiler registrar(
            int idCancha,
            int idCliente,
            int idUsuario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException;

    boolean cancelar(int idAlquiler)
            throws SQLException, IOException;

    // RF10: resumen por fechas de todas las canchas.
    List<ResumenAlquiler> resumirPorFechas(
            LocalDate desde,
            LocalDate hasta)
            throws SQLException, IOException;

    // RF10: resumen por fechas y cancha específica.
    // null significa todas las canchas.
    List<ResumenAlquiler> resumirPorFechas(
            LocalDate desde,
            LocalDate hasta,
            Integer idCancha)
            throws SQLException, IOException;

    // RF10: cantidad de alquileres por cancha.
    // Incluye confirmados y completados.
    // Excluye cancelados.
    List<CanchaResumen> resumirAlquileresPorCancha(
            LocalDate desde,
            LocalDate hasta)
            throws SQLException, IOException;

    boolean completar(
            int idAlquiler,
            LocalDateTime ahora)
            throws SQLException, IOException;

    // RF06: consultar canchas libres por fecha y horario.
    List<Cancha> listarDisponibles(
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException;

    // RF07 y RF08: modificar alquiler y recalcular importe.
    Alquiler reprogramar(
            int idAlquiler,
            int idCancha,
            int idCliente,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException;

    // RF10: consultar la ocupación por cancha y periodo.
// idCancha = null significa todas las canchas.
    List<OcupacionCancha> consultarOcupacion(
            LocalDate desde,
            LocalDate hasta,
            Integer idCancha)
            throws SQLException, IOException;
}