package pe.sportcenter.controlador;

import pe.sportcenter.dao.ClienteDAO;
import pe.sportcenter.modelo.Cliente;
import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;
import pe.sportcenter.util.Validaciones;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ClienteControlador {

    private final ClienteDAO clienteDAO;
    private final AutenticacionControlador autenticacion;

    public ClienteControlador(
            ClienteDAO clienteDAO,
            AutenticacionControlador autenticacion) {

        this.clienteDAO = Objects.requireNonNull(clienteDAO);
        this.autenticacion = Objects.requireNonNull(autenticacion);
    }

    public List<Cliente> listarClientes()
            throws SQLException, IOException {

        exigirPermiso();
        return clienteDAO.listarTodos();
    }

    public void registrar(
            String nombre,
            String dni,
            String telefono)
            throws SQLException, IOException {

        exigirPermiso();

        String nombreValidado = Validaciones.validarNombre(nombre);
        String dniValidado = validarDni(dni);
        String telefonoValidado = validarTelefono(telefono);

        Cliente nuevo = new Cliente(
                0,
                nombreValidado,
                dniValidado,
                telefonoValidado
        );

        clienteDAO.registrar(nuevo);
    }

    private void exigirPermiso() {
        Usuario usuario = autenticacion.getUsuarioActual();

        if (usuario == null) {
            throw new SecurityException(
                    "Debe iniciar sesión para gestionar clientes.");
        }

        if (usuario.getRol() != Rol.ADMINISTRADOR
                && usuario.getRol() != Rol.RECEPCIONISTA) {

            throw new SecurityException(
                    "No tiene permisos para gestionar clientes.");
        }
    }

    private String validarDni(String dni) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException(
                    "El DNI es obligatorio.");
        }

        String limpio = dni.strip();

        if (!limpio.matches("[0-9]{8}")) {
            throw new IllegalArgumentException(
                    "El DNI debe contener exactamente 8 dígitos.");
        }

        return limpio;
    }

    private String validarTelefono(String telefono) {
        // Se permite registrar un cliente sin teléfono.
        if (telefono == null || telefono.isBlank()) {
            return null;
        }

        String limpio = telefono.strip();

        if (!limpio.matches("[0-9]{9}")) {
            throw new IllegalArgumentException(
                    "El teléfono debe contener exactamente 9 dígitos.");
        }

        return limpio;
    }
}