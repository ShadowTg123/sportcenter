package pe.sportcenter.dao;

import pe.sportcenter.modelo.Cliente;
import pe.sportcenter.util.Conexion;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAOImpl implements ClienteDAO {

    @Override
    public List<Cliente> listarTodos()
            throws SQLException, IOException {

        String sql = """
                SELECT idCliente, nombre, dni, telefono
                FROM CLIENTE
                ORDER BY idCliente
                """;

        List<Cliente> clientes = new ArrayList<>();

        try (Connection conexion =
                     Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {

            while (resultado.next()) {
                Cliente cliente = new Cliente(
                        resultado.getInt("idCliente"),
                        resultado.getString("nombre"),
                        resultado.getString("dni"),
                        resultado.getString("telefono")
                );

                clientes.add(cliente);
            }
        }

        return clientes;
    }

    @Override
    public void registrar(Cliente cliente)
            throws SQLException, IOException {

        String sql = """
                INSERT INTO CLIENTE (nombre, dni, telefono)
                VALUES (?, ?, ?)
                """;

        try (Connection conexion =
                     Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setString(1, cliente.getNombre());
            consulta.setString(2, cliente.getDni());

            // El teléfono es opcional.
            String telefono = cliente.getTelefono();

            if (telefono == null || telefono.isBlank()) {
                consulta.setNull(3, java.sql.Types.VARCHAR);
            } else {
                consulta.setString(3, telefono);
            }

            if (consulta.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo registrar el cliente.");
            }
        }
    }
}