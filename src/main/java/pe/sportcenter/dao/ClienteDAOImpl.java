
// TEMA: PERSISTENCIA DE CLIENTES: SELECT e INSERT en MySQL

package pe.sportcenter.dao;

// IMPORT: Modelo para el cliente que realiza la reserva.
import pe.sportcenter.modelo.Cliente;
// IMPORT: Centraliza la apertura de la conexión a MySQL.
import pe.sportcenter.util.Conexion;

import java.io.IOException;
// IMPORT: Representa la sesión con MySQL, incluido commit y rollback.
import java.sql.Connection;
// IMPORT: Permite enviar datos en parámetros SQL y evita concatenar entradas del usuario.
import java.sql.PreparedStatement;
// IMPORT: Contiene las filas que devuelve una consulta SELECT.
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC del registro y consulta de clientes. Guarda SQL NULL cuando no se proporciona
 * un teléfono.
 */
// ESTA CLASE: PERSISTENCIA DE CLIENTES: SELECT e INSERT en MySQL.
public class ClienteDAOImpl implements ClienteDAO {

    @Override
    // EXPLICACIÓN DEL MÉTODO: SELECT sobre CLIENTE y construccion de objetos Cliente para llenar el combo y la tabla.
    public List<Cliente> listarTodos() throws SQLException, IOException {

        // PASO (listarTodos): Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (listarTodos): SELECT recupera los clientes existentes de la tabla CLIENTE.
        String sql =
                """
                SELECT idCliente, nombre, dni, telefono
                FROM CLIENTE
                ORDER BY idCliente
                """;

        // PASO (listarTodos): Crea la lista que se llenara leyendo filas de MySQL.
        List<Cliente> clientes = new ArrayList<>();

        // PASO (listarTodos): Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {

            // PASO (listarTodos): Lee resultados hasta que ya no queden filas.
            while (resultado.next()) {
                Cliente cliente =
                        new Cliente(
                                resultado.getInt("idCliente"),
                                resultado.getString("nombre"),
                                resultado.getString("dni"),
                                resultado.getString("telefono"));

                clientes.add(cliente);
            }
        }

        // PASO (listarTodos): Devuelve el dato calculado o la respuesta de esta operacion.
        return clientes;
    }

    @Override
    // EXPLICACIÓN DEL MÉTODO: INSERT parametrizado en CLIENTE; el telefono puede almacenarse como SQL NULL.
    public void registrar(Cliente cliente) throws SQLException, IOException {

        // PASO (registrar): Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (registrar): Los INSERT necesarios se ejecutan dentro de la misma transaccion.
        String sql =
                """
                INSERT INTO CLIENTE (nombre, dni, telefono)
                VALUES (?, ?, ?)
                """;

        // PASO (registrar): Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            // PASO (registrar): Asocia texto a un parametro ?; no concatena valores en la SQL.
            consulta.setString(1, cliente.getNombre());
            // PASO (registrar): Asocia texto a un parametro ?; no concatena valores en la SQL.
            consulta.setString(2, cliente.getDni());

            // El teléfono es opcional.
            String telefono = cliente.getTelefono();

            // PASO (registrar): Evita seguir si falta un dato necesario para la operacion.
            if (telefono == null || telefono.isBlank()) {
                // PASO (registrar): Guarda SQL NULL cuando el dato opcional no tiene valor.
                consulta.setNull(3, java.sql.Types.VARCHAR);
            } else {
                // PASO (registrar): Asocia texto a un parametro ?; no concatena valores en la SQL.
                consulta.setString(3, telefono);
            }

            // PASO (registrar): Comprueba una condicion de negocio o controla un caso de error.
            if (consulta.executeUpdate() != 1) {
                // PASO (registrar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                throw new SQLException("No se pudo registrar el cliente.");
            }
        }
    }
}