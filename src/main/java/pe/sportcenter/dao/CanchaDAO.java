
package pe.sportcenter.dao;

import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.CanchaResumen;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface CanchaDAO {

    List<Cancha> listarTodas() throws SQLException, IOException;

    void registrar(Cancha cancha) throws SQLException, IOException;

    void actualizar(Cancha cancha) throws SQLException, IOException;

    boolean eliminarLogicamente(int idCancha)
            throws SQLException, IOException;

    boolean eliminarFisicamente(int idCancha)
            throws SQLException, IOException;

    List<CanchaResumen> listarConCantidadAlquileres()
            throws SQLException, IOException;
}
