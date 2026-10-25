
//CONTROLADOR DE CLIENTES: sesion, validacion y DAO



package pe.sportcenter.controlador;

import pe.sportcenter.dao.ClienteDAO;
// IMPORT: Modelo para el cliente que realiza la reserva.
import pe.sportcenter.modelo.Cliente;
import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;
import pe.sportcenter.util.Validaciones;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Valida los datos de clientes y permite su registro y consulta a ambos perfiles. Delega la
 * persistencia al DAO y comunica los errores de validación a la vista.
 */
// ESTA CLASE: CONTROLADOR DE CLIENTES: sesion, validacion y DAO.
public class ClienteControlador {

    // CAMPO: Dependencia del DAO: acceso a datos mediante una interfaz.
    private final ClienteDAO clienteDAO;
    // CAMPO: Referencia a la sesion para comprobar permisos.
    private final AutenticacionControlador autenticacion;

    // : Recibe ClienteDAO y autenticacion para controlar el registro/consulta de clientes.
    public ClienteControlador(ClienteDAO clienteDAO, AutenticacionControlador autenticacion) {

        this.clienteDAO = Objects.requireNonNull(clienteDAO);
        this.autenticacion = Objects.requireNonNull(autenticacion);
    }

    // : Revisa sesion y solicita al DAO la lista de clientes.
    public List<Cliente> listarClientes() throws SQLException, IOException {

        exigirPermiso();
        // PASO (listarClientes): Devuelve el dato calculado o la respuesta de esta operacion.
        return clienteDAO.listarTodos();
    }

    // CLAVE OBJETIVO 2: valida nombre, DNI de 8 digitos y telefono opcional de 9 digitos; guarda en CLIENTE.
    public void registrar(String nombre, String dni, String telefono)
            throws SQLException, IOException {

        exigirPermiso();

        // PASO (registrar): Normaliza y valida el dato antes de guardarlo.
        String nombreValidado = Validaciones.validarNombre(nombre);
        // PASO (registrar): Normaliza y valida el dato antes de guardarlo.
        String dniValidado = validarDni(dni);
        // PASO (registrar): Normaliza y valida el dato antes de guardarlo.
        String telefonoValidado = validarTelefono(telefono);

        // PASO (registrar): Construye un Cliente sin ID definitivo; MySQL se lo asignara al insertarlo.
        Cliente nuevo = new Cliente(0, nombreValidado, dniValidado, telefonoValidado);

        // PASO (registrar): Inserta un cliente nuevo en MySQL mediante el DAO.
        clienteDAO.registrar(nuevo);
    }

    /** Comprueba la sesión y los perfiles autorizados antes de acceder al módulo. */
    // : Autoriza gestion de clientes a administrador o recepcionista con sesion activa.
    private void exigirPermiso() {
        Usuario usuario = autenticacion.getUsuarioActual();

        // PASO (exigirPermiso): Evita seguir si falta un dato necesario para la operacion.
        if (usuario == null) {
            // PASO (exigirPermiso): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new SecurityException("Debe iniciar sesión para gestionar clientes.");
        }

        // PASO (exigirPermiso): Comprueba una condicion de negocio o controla un caso de error.
        if (usuario.getRol() != Rol.ADMINISTRADOR && usuario.getRol() != Rol.RECEPCIONISTA) {

            // PASO (exigirPermiso): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new SecurityException("No tiene permisos para gestionar clientes.");
        }
    }

    /** Valida que el DNI contenga exactamente ocho dígitos. */
    // : Normaliza y exige exactamente ocho digitos numericos.
    private String validarDni(String dni) {
        // PASO (validarDni): Evita seguir si falta un dato necesario para la operacion.
        if (dni == null || dni.isBlank()) {
            // PASO (validarDni): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("El DNI es obligatorio.");
        }

        String limpio = dni.strip();

        // PASO (validarDni): Comprueba una condicion de negocio o controla un caso de error.
        if (!limpio.matches("[0-9]{8}")) {
            // PASO (validarDni): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos.");
        }

        // PASO (validarDni): Devuelve el dato calculado o la respuesta de esta operacion.
        return limpio;
    }

    /** Valida el teléfono opcional antes de enviarlo al DAO. */
    // : Permite vacio; si se informa debe tener nueve digitos numericos.
    private String validarTelefono(String telefono) {
        // Se permite registrar un cliente sin teléfono.
        // PASO (validarTelefono): Evita seguir si falta un dato necesario para la operacion.
        if (telefono == null || telefono.isBlank()) {
            // PASO (validarTelefono): Devuelve ausencia de valor para el caso indicado.
            return null;
        }

        String limpio = telefono.strip();

        // PASO (validarTelefono): Comprueba una condicion de negocio o controla un caso de error.
        if (!limpio.matches("[0-9]{9}")) {
            // PASO (validarTelefono): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("El teléfono debe contener exactamente 9 dígitos.");
        }

        // PASO (validarTelefono): Devuelve el dato calculado o la respuesta de esta operacion.
        return limpio;
    }
}
