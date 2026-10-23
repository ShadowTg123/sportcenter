package pe.sportcenter.dao;

import pe.sportcenter.modelo.Alquiler;
import pe.sportcenter.modelo.EstadoAlquiler;
import pe.sportcenter.util.Conexion;
import pe.sportcenter.modelo.ResumenAlquiler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class AlquilerDAOImpl implements AlquilerDAO {

    @Override
    public List<Alquiler> listarPorFecha(LocalDate fecha)
            throws SQLException, IOException {

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Seleccione una fecha.");
        }

        String sql = """
                SELECT idAlquiler, idCancha, idCliente, idUsuario,
                       fecha, horaInicio, horaFin, montoTotal, estado
                FROM ALQUILER
                WHERE fecha = ?
                ORDER BY horaInicio, idCancha, idAlquiler
                """;

        List<Alquiler> alquileres = new ArrayList<>();

        try (Connection conexion =
                     Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setDate(1, Date.valueOf(fecha));

            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    alquileres.add(convertirAlquiler(resultado));
                }
            }
        }

        return alquileres;
    }

    @Override
    public Alquiler registrar(
            int idCancha,
            int idCliente,
            int idUsuario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException {

        validarDatos(
                idCancha, idCliente, idUsuario,
                fecha, horaInicio, horaFin
        );

        try (Connection conexion =
                     Conexion.getInstancia().abrirConexion()) {

            // Comienza la transacción.
            conexion.setAutoCommit(false);

            try {
                // Bloqueamos esta cancha hasta terminar el registro.
                BigDecimal tarifaHora =
                        obtenerTarifaYBloquearCancha(conexion, idCancha);

                comprobarHorario(
                        conexion, idCancha, fecha, horaInicio, horaFin
                );

                long minutos =
                        Duration.between(horaInicio, horaFin).toMinutes();

                BigDecimal montoTotal = tarifaHora
                        .multiply(BigDecimal.valueOf(minutos))
                        .divide(
                                BigDecimal.valueOf(60),
                                2,
                                RoundingMode.HALF_UP
                        );

                int idAlquiler = insertar(
                        conexion,
                        idCancha,
                        idCliente,
                        idUsuario,
                        fecha,
                        horaInicio,
                        horaFin,
                        montoTotal
                );

                Alquiler registrado = new Alquiler(
                        idAlquiler,
                        idCancha,
                        idCliente,
                        idUsuario,
                        fecha,
                        horaInicio,
                        horaFin,
                        montoTotal,
                        EstadoAlquiler.CONFIRMADO
                );

                // Confirma todos los cambios de la transacción.
                conexion.commit();

                return registrado;

            } catch (SQLException | RuntimeException e) {
                // Si falla alguna operación, deshace el registro.
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                throw e;
            }
        }
    }

    private BigDecimal obtenerTarifaYBloquearCancha(
            Connection conexion,
            int idCancha) throws SQLException {

        String sql = """
                SELECT tarifaHora, estado
                FROM CANCHA
                WHERE idCancha = ?
                FOR UPDATE
                """;

        try (PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setInt(1, idCancha);

            try (ResultSet resultado = consulta.executeQuery()) {

                if (!resultado.next()) {
                    throw new IllegalArgumentException(
                            "La cancha seleccionada ya no existe.");
                }

                String estado = resultado.getString("estado");

                if (!"disponible".equals(estado)) {
                    throw new IllegalArgumentException(
                            "Solo se pueden registrar alquileres "
                                    + "para canchas en estado disponible.");
                }

                BigDecimal tarifa = resultado.getBigDecimal("tarifaHora");

                if (tarifa == null
                        || tarifa.compareTo(new BigDecimal("10.00")) < 0
                        || tarifa.compareTo(new BigDecimal("500.00")) > 0) {

                    throw new IllegalArgumentException(
                            "La cancha tiene una tarifa inválida.");
                }

                return tarifa;
            }
        }
    }

    private void comprobarHorario(
            Connection conexion,
            int idCancha,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin) throws SQLException {

        String sql = """
                SELECT idAlquiler
                FROM ALQUILER
                WHERE idCancha = ?
                  AND fecha = ?
                  AND estado IN ('confirmado', 'completado')
                  AND horaInicio < ?
                  AND horaFin > ?
                LIMIT 1
                FOR UPDATE
                """;

        try (PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setInt(1, idCancha);
            consulta.setDate(2, Date.valueOf(fecha));
            consulta.setTime(3, Time.valueOf(horaFin));
            consulta.setTime(4, Time.valueOf(horaInicio));

            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    throw new IllegalArgumentException(
                            "La cancha ya tiene un alquiler "
                                    + "que se cruza con ese horario.");
                }
            }
        }
    }

    private int insertar(
            Connection conexion,
            int idCancha,
            int idCliente,
            int idUsuario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            BigDecimal montoTotal) throws SQLException {

        String sql = """
                INSERT INTO ALQUILER
                    (idCancha, idCliente, idUsuario, fecha,
                     horaInicio, horaFin, montoTotal, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'confirmado')
                """;

        try (PreparedStatement consulta = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            consulta.setInt(1, idCancha);
            consulta.setInt(2, idCliente);
            consulta.setInt(3, idUsuario);
            consulta.setDate(4, Date.valueOf(fecha));
            consulta.setTime(5, Time.valueOf(horaInicio));
            consulta.setTime(6, Time.valueOf(horaFin));
            consulta.setBigDecimal(7, montoTotal);

            if (consulta.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo registrar el alquiler.");
            }

            try (ResultSet claves = consulta.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
            }

            throw new SQLException(
                    "No se pudo obtener el ID del alquiler.");
        }
    }

    @Override
    public boolean cancelar(int idAlquiler)
            throws SQLException, IOException {

        if (idAlquiler <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un alquiler válido.");
        }

        String sql = """
                UPDATE ALQUILER
                SET estado = 'cancelado'
                WHERE idAlquiler = ?
                  AND estado = 'confirmado'
                """;

        try (Connection conexion =
                     Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setInt(1, idAlquiler);

            return consulta.executeUpdate() == 1;
        }
    }

    @Override
    public boolean completar(int idAlquiler, LocalDateTime ahora)
            throws SQLException, IOException {

        if (idAlquiler <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un alquiler válido.");
        }

        if (ahora == null) {
            throw new IllegalArgumentException(
                    "La fecha y hora de comprobación son obligatorias.");
        }

        String sql = """
            UPDATE ALQUILER
            SET estado = 'completado'
            WHERE idAlquiler = ?
              AND estado = 'confirmado'
              AND (
                    fecha < ?
                    OR (fecha = ? AND horaFin <= ?)
                  )
            """;

        try (Connection conexion =
                     Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            Date fechaActual = Date.valueOf(ahora.toLocalDate());

            consulta.setInt(1, idAlquiler);
            consulta.setDate(2, fechaActual);
            consulta.setDate(3, fechaActual);
            consulta.setTime(4, Time.valueOf(ahora.toLocalTime()));

            return consulta.executeUpdate() == 1;
        }
    }


    private void validarDatos(
            int idCancha,
            int idCliente,
            int idUsuario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin) {

        if (idCancha <= 0 || idCliente <= 0 || idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "La cancha, el cliente y el usuario deben ser válidos.");
        }

        if (fecha == null || horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException(
                    "La fecha y las horas son obligatorias.");
        }

        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                    "La hora final debe ser posterior a la inicial "
                            + "dentro del mismo día.");
        }

        // Trabajamos con horas y minutos, sin segundos.
        if (horaInicio.getSecond() != 0
                || horaFin.getSecond() != 0
                || horaInicio.getNano() != 0
                || horaFin.getNano() != 0) {

            throw new IllegalArgumentException(
                    "Ingrese las horas con precisión de minutos.");
        }
    }


    @Override
    public List<ResumenAlquiler> resumirPorFechas(
            LocalDate desde,
            LocalDate hasta)
            throws SQLException, IOException {

        if (desde == null || hasta == null) {
            throw new IllegalArgumentException(
                    "Las fechas inicial y final son obligatorias.");
        }

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        String sql = """
            SELECT estado,
                   COUNT(*) AS cantidad,
                   SUM(montoTotal) AS total,
                   AVG(montoTotal) AS promedio,
                   MIN(montoTotal) AS minimo,
                   MAX(montoTotal) AS maximo
            FROM ALQUILER
            WHERE fecha BETWEEN ? AND ?
            GROUP BY estado
            """;

        java.util.Map<EstadoAlquiler, ResumenAlquiler> porEstado =
                new java.util.EnumMap<>(EstadoAlquiler.class);

        try (Connection conexion =
                     Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setDate(1, Date.valueOf(desde));
            consulta.setDate(2, Date.valueOf(hasta));

            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    EstadoAlquiler estado = EstadoAlquiler.desdeBD(
                            resultado.getString("estado"));

                    ResumenAlquiler resumen = new ResumenAlquiler(
                            estado,
                            resultado.getLong("cantidad"),
                            resultado.getBigDecimal("total"),
                            resultado.getBigDecimal("promedio")
                                    .setScale(2, RoundingMode.HALF_UP),
                            resultado.getBigDecimal("minimo"),
                            resultado.getBigDecimal("maximo")
                    );

                    porEstado.put(estado, resumen);
                }
            }
        }

        List<ResumenAlquiler> resumenes = new ArrayList<>();
        BigDecimal cero = new BigDecimal("0.00");

        for (EstadoAlquiler estado : EstadoAlquiler.values()) {
            ResumenAlquiler resumen = porEstado.get(estado);

            if (resumen == null) {
                resumen = new ResumenAlquiler(
                        estado,
                        0,
                        cero,
                        cero,
                        cero,
                        cero
                );
            }

            resumenes.add(resumen);
        }

        return resumenes;
    }

    private Alquiler convertirAlquiler(ResultSet resultado)
            throws SQLException {

        return new Alquiler(
                resultado.getInt("idAlquiler"),
                resultado.getInt("idCancha"),
                resultado.getInt("idCliente"),
                resultado.getInt("idUsuario"),
                resultado.getDate("fecha").toLocalDate(),
                resultado.getTime("horaInicio").toLocalTime(),
                resultado.getTime("horaFin").toLocalTime(),
                resultado.getBigDecimal("montoTotal"),
                EstadoAlquiler.desdeBD(resultado.getString("estado"))
        );
    }
}