package pe.sportcenter.dao;

import pe.sportcenter.modelo.Alquiler;
import pe.sportcenter.modelo.ResumenAlquiler;
import pe.sportcenter.modelo.Cancha;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.time.LocalDateTime;

public interface AlquilerDAO {

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

    List<ResumenAlquiler> resumirPorFechas(
            LocalDate desde,
            LocalDate hasta)
            throws SQLException, IOException;

    boolean completar(int idAlquiler, LocalDateTime ahora)
            throws SQLException, IOException;

    // RF06: consulta las canchas libres en una fecha y horario.
    List<Cancha> listarDisponibles(
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException;
}