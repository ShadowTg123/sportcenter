package pe.sportcenter.dao;

import pe.sportcenter.modelo.Cliente;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface ClienteDAO {

    List<Cliente> listarTodos()
            throws SQLException, IOException;

    void registrar(Cliente cliente)
            throws SQLException, IOException;
}