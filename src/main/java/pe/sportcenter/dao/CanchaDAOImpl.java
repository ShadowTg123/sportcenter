
package pe.sportcenter.dao;

import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.CanchaResumen;
import pe.sportcenter.modelo.DeporteCancha;
import pe.sportcenter.modelo.EstadoCancha;
import pe.sportcenter.util.Conexion;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CanchaDAOImpl implements CanchaDAO {

    @Override
    public List<Cancha> listarTodas()
            throws SQLException, IOException {

        String sql = """
                SELECT idCancha, nombre, deporte, tarifaHora, estado
                FROM CANCHA
                ORDER BY idCancha
                """;

        List<Cancha> canchas = new ArrayList<>();

        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {

            while (resultado.next()) {
                canchas.add(convertirCancha(resultado));
            }
        }

        return canchas;
    }

    @Override
    public void registrar(Cancha cancha)
            throws SQLException, IOException {

        String sql = """
                INSERT INTO CANCHA (nombre, deporte, tarifaHora, estado)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            consulta.setString(1, cancha.getNombre());
            consulta.setString(2, cancha.getDeporte().valorBD());
            consulta.setBigDecimal(3, cancha.getTarifaHora());
            consulta.setString(4, cancha.getEstado().valorBD());

            consulta.executeUpdate();
        }
    }

    @Override
    public void actualizar(Cancha cancha)
            throws SQLException, IOException {

        String sql = """
                UPDATE CANCHA
                SET nombre = ?, deporte = ?, tarifaHora = ?, estado = ?
                WHERE idCancha = ?
                """;

        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            consulta.setString(1, cancha.getNombre());
            consulta.setString(2, cancha.getDeporte().valorBD());
            consulta.setBigDecimal(3, cancha.getTarifaHora());
            consulta.setString(4, cancha.getEstado().valorBD());
            consulta.setInt(5, cancha.getIdCancha());

            if (consulta.executeUpdate() == 0) {
                throw new SQLException(
                        "No se encontró la cancha para actualizar.");
            }
        }
    }

    @Override
    public boolean eliminarLogicamente(int idCancha)
            throws SQLException, IOException {

        String sql = """
                UPDATE CANCHA
                SET estado = 'inactiva'
                WHERE idCancha = ? AND estado <> 'inactiva'
                """;

        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            consulta.setInt(1, idCancha);
            return consulta.executeUpdate() == 1;
        }
    }

    @Override
    public boolean eliminarFisicamente(int idCancha)
            throws SQLException, IOException {

        // No borra la cancha si tiene alquileres asociados.
        String sql = """
                DELETE FROM CANCHA
                WHERE idCancha = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM ALQUILER
                      WHERE idCancha = ?
                  )
                """;

        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            consulta.setInt(1, idCancha);
            consulta.setInt(2, idCancha);

            return consulta.executeUpdate() == 1;
        }
    }

    @Override
    public List<CanchaResumen> listarConCantidadAlquileres()
            throws SQLException, IOException {

        String sql = """
                SELECT c.idCancha, c.nombre, c.deporte,
                       c.tarifaHora, c.estado,
                       COUNT(a.idAlquiler) AS cantidadAlquileres
                FROM CANCHA c
                LEFT JOIN ALQUILER a ON a.idCancha = c.idCancha
                GROUP BY c.idCancha, c.nombre, c.deporte,
                         c.tarifaHora, c.estado
                ORDER BY c.idCancha
                """;

        List<CanchaResumen> resumenes = new ArrayList<>();

        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {

            while (resultado.next()) {
                Cancha cancha = convertirCancha(resultado);

                long cantidad = resultado.getLong("cantidadAlquileres");

                resumenes.add(new CanchaResumen(cancha, cantidad));
            }
        }

        return resumenes;
    }

    private Cancha convertirCancha(ResultSet resultado)
            throws SQLException {

        return new Cancha(
                resultado.getInt("idCancha"),
                resultado.getString("nombre"),
                DeporteCancha.desdeBD(resultado.getString("deporte")),
                resultado.getBigDecimal("tarifaHora"),
                EstadoCancha.desdeBD(resultado.getString("estado"))
        );
    }
}
