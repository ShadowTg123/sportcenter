// : Paquete de esta clase: coincide con su ubicación en src/main/java y permite que otras clases la importen.
package pe.sportcenter.controlador;

// : Contrato de operaciones del catálogo de canchas.
import pe.sportcenter.dao.CanchaDAO;
// : Modelo de cancha con nombre, deporte, tarifa y estado.
import pe.sportcenter.modelo.Cancha;
// : Resultado de contar alquileres por cancha.
import pe.sportcenter.modelo.CanchaResumen;
// : Enumeración del deporte permitido en una cancha.
import pe.sportcenter.modelo.DeporteCancha;
// : Enumeración: disponible, alquilada, mantenimiento, inactiva.
import pe.sportcenter.modelo.EstadoCancha;
// : Enumeración de permisos ADMINISTRADOR y RECEPCIONISTA.
import pe.sportcenter.modelo.Rol;
// : Modelo que representa la cuenta y rol autenticados.
import pe.sportcenter.modelo.Usuario;

// : Excepción en operaciones de entrada/salida como configuración.
import java.io.IOException;
// : Tipo decimal preciso para importes monetarios.
import java.math.BigDecimal;
// : Importa RoundingMode: tipo o clase utilizado más abajo en este archivo.
import java.math.RoundingMode;
// : Excepción cuando falla consulta o actualización de MySQL.
import java.sql.SQLException;
// : Colección ordenada de resultados de consultas.
import java.util.List;
// : Permite comprobar referencias nulas con requireNonNull.
import java.util.Objects;

/**
 * Aplica permisos y validaciones antes de delegar la gestión de canchas al DAO. El administrador
 * modifica datos; el recepcionista consulta las canchas no inactivas.
 */
// : Declara la clase y su responsabilidad dentro del módulo SportCenter.
    //    //en CanchaControlador, implementamos las operaciones para administrar las canchas.
public class CanchaControlador {

    // : Dependencia o atributo final: se asigna en el constructor y no se reemplaza después.
    private final CanchaDAO canchaDAO;
    // : Dependencia o atributo final: se asigna en el constructor y no se reemplaza después.
    private final AutenticacionControlador autenticacion;

    // : Constructor: recibe DAO de canchas y controlador de autenticación para reutilizar la sesión y comprobar permisos.
    public CanchaControlador(CanchaDAO canchaDAO, AutenticacionControlador autenticacion) {
        // : Obliga a recibir una dependencia no nula: falla pronto si está mal configurada.
        this.canchaDAO = Objects.requireNonNull(canchaDAO);
        // : Obliga a recibir una dependencia no nula: falla pronto si está mal configurada.
        this.autenticacion = Objects.requireNonNull(autenticacion);
    }

    // CLAVE PARA SUSTENTAR: listarCanchas()
    // : CONSULTAR: obtiene todas las canchas del DAO; para recepcionista oculta registros inactivos.
    public List<Cancha> listarCanchas() throws SQLException, IOException {

        // : Obtiene usuario y rol, rechazando a cualquiera que no haya iniciado sesión.
        Usuario usuario = exigirSesion();

        // : Solicita al DAO un SELECT de los registros de la tabla CANCHA.
        List<Cancha> canchas = canchaDAO.listarTodas();

        // El recepcionista no consulta las canchas inactivas.
        // : El recepcionista solo puede consultar canchas que no estén inactivas.
        if (usuario.getRol() == Rol.RECEPCIONISTA) {
            // : Filtro en Java: descarta canchas INACTIVA para la vista del recepcionista.
            return canchas.stream().filter(c -> c.getEstado() != EstadoCancha.INACTIVA).toList();
        }

        // : Devuelve el resultado de esta operación al método que la invocó.
        return canchas;
    }

    // : Obtiene canchas con cantidad de alquileres; restringe la visualización de las inactivas al recepcionista.
    public List<CanchaResumen> listarResumenAlquileres() throws SQLException, IOException {

        // : Obtiene usuario y rol, rechazando a cualquiera que no haya iniciado sesión.
        Usuario usuario = exigirSesion();

        // : Crea o recibe una colección de modelos/resultados que luego se muestran en la interfaz.
        List<CanchaResumen> resumenes = canchaDAO.listarConCantidadAlquileres();

        // : El recepcionista solo puede consultar canchas que no estén inactivas.
        if (usuario.getRol() == Rol.RECEPCIONISTA) {
            // : Devuelve el resultado de esta operación al método que la invocó.
            return resumenes.stream()
                    // : Continúa la condición/cadena de operaciones anterior y añade otra comprobación o transformación.
                    .filter(r -> r.getCancha().getEstado() != EstadoCancha.INACTIVA)
                    // : Continúa la condición/cadena de operaciones anterior y añade otra comprobación o transformación.
                    .toList();
        }

        // : Devuelve el resultado de esta operación al método que la invocó.
        return resumenes;
    }
//enum permite definir un conjunto de valores específicos que puede aceptar una variable, basicamente creamos un tipo de dao perzonalisado
    //DeporteCancha


    // odjetivo1: registrar()
    // Este método se encarga de registrar una nueva cancha deportiva.
    //  Primero verifica que el usuario tenga permisos de administrador,
    //  después valida los datos ingresados y finalmente crea el objeto Cancha y lo envía al DAO para guardarlo en MySQL.

    //BigDecimal no redondea, opera con decimales
    //ambién declaramos SQLException e IOException, que son excepciones que pueden presentarse durante las operaciones y que el método permite propagar.
    public void registrar(String nombre, DeporteCancha deporte, BigDecimal tarifaHora)
            throws SQLException, IOException {

        // : Permiso antes de modificar datos: solo ADMINISTRADOR puede añadir, actualizar o eliminar.
        //Primero verificamos que el usuario sea administrador.
        // Si no tiene permiso, no puede registrar la cancha.
        exigirAdministrador();

//Después tenemos tres validaciones
        // : Guarda texto validado u obtenido de la consulta para las comprobaciones siguientes.
        //Validamos que el nombre ingresado sea correcto.
        String nombreValidado = validarNombre(nombre);
        //Verificamos que se haya seleccionado un deporte válido.
        DeporteCancha deporteValidado = validarDeporte(deporte);
        // Validamos la tarifa por hora.
        // Utilizamos BigDecimal para manejar importes monetarios con precisión.
        //BigDecimal evita problemas de precisión de double en importes monetarios.
        BigDecimal tarifaValidada = validarTarifa(tarifaHora);

        //Luego creamos un objeto llamado nueva, utilizando la clase Cancha.
        // Le pasamos el nombre, el deporte, la tarifa y el estado inicial DISPONIBLE.
        // El cero representa el ID inicial, que posteriormente será generado por la base de datos.

        Cancha nueva =
                // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                new Cancha(
                        0,
                        nombreValidado,
                        deporteValidado,
                        tarifaValidada,
                        // : Al registrar una cancha, su estado inicial será DISPONIBLE.
                        EstadoCancha.DISPONIBLE);

        // : Envía cancha validada al DAO para el INSERT en MySQL.
        //Se llama al método
        // registrar del objeto canchaDAO, pasándole como argumento la variable nueva
        canchaDAO.registrar(nueva);
    }

    // : ACTUALIZAR: exige administrador, comprueba ID/valores y actualiza un registro existente mediante DAO.
    public void actualizar(
            int idCancha,
            String nombre,
            DeporteCancha deporte,
            BigDecimal tarifaHora,
            EstadoCancha estado)
            throws SQLException, IOException {

        // : Permiso antes de modificar datos: solo ADMINISTRADOR puede añadir, actualizar o eliminar.
        exigirAdministrador();

        // : Validación de identificador: los ID de base de datos deben ser positivos.
        if (idCancha <= 0) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("Seleccione una cancha válida.");
        }

        // : Guarda texto validado u obtenido de la consulta para las comprobaciones siguientes.
        String nombreValidado = validarNombre(nombre);
        DeporteCancha deporteValidado = validarDeporte(deporte);
        // : BigDecimal evita problemas de precisión de double en importes monetarios.
        BigDecimal tarifaValidada = validarTarifa(tarifaHora);

        // : Inactivar una cancha debe hacerse mediante Eliminar, no mediante la edición normal.
        if (estado == null || estado == EstadoCancha.INACTIVA) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException(
                    "Seleccione un estado válido. "
                            + "Para inactivar una cancha use la opción Eliminar.");
        }

        // : Crea o recibe un objeto del modelo con la información que maneja el sistema.
        Cancha actualizada =
                // : Inicializa/usa una referencia de un objeto necesario en este módulo.
                new Cancha(idCancha, nombreValidado, deporteValidado, tarifaValidada, estado);

        // : Envía la información corregida al DAO para UPDATE en MySQL.
        canchaDAO.actualizar(actualizada);
    }

    // : ELIMINACIÓN LÓGICA: exige administrador y cambia el estado de la cancha sin borrar el registro histórico.
    public boolean eliminarLogicamente(int idCancha) throws SQLException, IOException {

        // : Permiso antes de modificar datos: solo ADMINISTRADOR puede añadir, actualizar o eliminar.
        exigirAdministrador();
        validarId(idCancha);
        // : Solicita al DAO cambiar estado a inactiva sin borrar el registro.
        return canchaDAO.eliminarLogicamente(idCancha);
    }

    // : ELIMINACIÓN FÍSICA: exige administrador y solicita al DAO borrar el registro, sujeto a restricciones de datos relacionados.
    public boolean eliminarFisicamente(int idCancha) throws SQLException, IOException {

        // : Permiso antes de modificar datos: solo ADMINISTRADOR puede añadir, actualizar o eliminar.
        exigirAdministrador();
        validarId(idCancha);
        // : Solicita al DAO borrar definitivamente si no hay alquileres asociados.
        return canchaDAO.eliminarFisicamente(idCancha);
    }

    /** Obtiene el usuario de la sesión o rechaza la operación si no está autenticado. */
    // : SEGURIDAD: comprueba que exista una sesión activa antes de permitir consultar o modificar canchas.
    private Usuario exigirSesion() {
        // : Obtiene la sesión desde AutenticacionControlador, no de datos ingresados por pantalla.
        Usuario usuario = autenticacion.getUsuarioActual();

        // : Comprueba que el dato requerido no sea nulo antes de continuar.
        if (usuario == null) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new SecurityException("Debe iniciar sesión para realizar esta operación.");
        }

        // : Devuelve el resultado de esta operación al método que la invocó.
        return usuario;
    }

    /** Comprueba el rol en el controlador, además de las restricciones de los botones. */
    //  exigirAdministrador()
    // : AUTORIZACIÓN: prohíbe agregar/modificar/eliminar a recepcionistas, independientemente de los botones de la interfaz.
    private void exigirAdministrador() {
        // : Obtiene usuario y rol, rechazando a cualquiera que no haya iniciado sesión.
        Usuario usuario = exigirSesion();

        // : Se rechaza aunque un recepcionista consiguiera llamar al método por otra ruta.
        if (usuario.getRol() != Rol.ADMINISTRADOR) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new SecurityException("Solo el administrador puede modificar las canchas.");
        }
    }

    /**
     * Comprueba obligatoriedad, longitud y caracteres de control; elimina espacios de los extremos.
     */
    // : Verifica nombre de cancha: obligatorio, hasta 50 caracteres y sin caracteres de control.
    private String validarNombre(String nombre) {
        // : Comprueba que el dato requerido no sea nulo antes de continuar.
        if (nombre == null || nombre.isBlank()) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("El nombre de la cancha es obligatorio.");
        }

        // : Normaliza entrada eliminando espacios al principio y al final.
        String limpio = nombre.strip();

        // : Condición: si se cumple, aplica la validación o decisión descrita en el bloque siguiente.
        if (limpio.length() > 50) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("El nombre admite como máximo 50 caracteres.");
        }

        // : Condición: si se cumple, aplica la validación o decisión descrita en el bloque siguiente.
        if (limpio.codePoints().anyMatch(Character::isISOControl)) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("El nombre contiene caracteres no permitidos.");
        }

        // : Devuelve el resultado de esta operación al método que la invocó.
        return limpio;
    }

    // : Solo admite un DeporteCancha válido; rechaza una selección nula.
    private DeporteCancha validarDeporte(DeporteCancha deporte) {
        // : Comprueba que el dato requerido no sea nulo antes de continuar.
        if (deporte == null) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("Seleccione Fútbol o Vóley.");
        }

        // : Devuelve el resultado de esta operación al método que la invocó.
        return deporte;
    }

    /** Exige un importe entre 10 y 500 soles sin redondear decimales adicionales. */
    // : Verifica tarifa decimal (máximo dos decimales) y rango de S/ 10.00 a S/ 500.00.
    private BigDecimal validarTarifa(BigDecimal tarifaHora) {
        // : Comprueba que el dato requerido no sea nulo antes de continuar.
        if (tarifaHora == null) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("La tarifa por hora es obligatoria.");
        }

        BigDecimal tarifa;

        // : Ejecuta este conjunto de operaciones, con manejo posterior de errores si ocurren.
        try {
            // : Exige como máximo 2 decimales sin redondear silenciosamente.
            tarifa = tarifaHora.setScale(2, RoundingMode.UNNECESSARY);
            // : Maneja excepciones; permite revertir transacciones o informar el motivo del error.
        } catch (ArithmeticException e) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("La tarifa solo puede tener dos decimales.");
        }

        // : Compara decimales con BigDecimal sin perder precisión.
        if (tarifa.compareTo(new BigDecimal("10.00")) < 0
                // : Construye un decimal exacto para una tarifa o para límites de validación.
                || tarifa.compareTo(new BigDecimal("500.00")) > 0) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("La tarifa debe estar entre S/ 10.00 y S/ 500.00.");
        }

        // : Devuelve el resultado de esta operación al método que la invocó.
        return tarifa;
    }

    /** Rechaza identificadores no positivos antes de modificar datos. */
    // : Comprueba que el identificador de cancha sea positivo.
    private void validarId(int idCancha) {
        // : Validación de identificador: los ID de base de datos deben ser positivos.
        if (idCancha <= 0) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("Seleccione una cancha válida.");
        }
    }
}
