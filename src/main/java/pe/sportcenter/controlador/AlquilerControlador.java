// ============================================================================
// GUÍA COMENTADA DE SPORTCENTER — SEGUNDA CONCLUSIÓN
// TEMA: CONTROLADOR DE ALQUILERES: autorizacion, validaciones y coordinacion con el DAO
// Solo se insertaron comentarios. Los datos, instrucciones y SQL son ORIGINALES.
// Busca «CLAVE OBJETIVO 2» para llegar rápido a lo esencial de tu exposición.
// IMPORTANTE: Los reportes corresponden a la tercera conclusión.
// ============================================================================

package pe.sportcenter.controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pe.sportcenter.dao.AlquilerDAO;
import pe.sportcenter.dao.CanchaDAO;
import pe.sportcenter.dao.ClienteDAO;
// IMPORT: Modelo de una reserva registrada.
import pe.sportcenter.modelo.Alquiler;
// IMPORT: Modelo para la cancha seleccionada.
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.CanchaResumen;
// IMPORT: Modelo para el cliente que realiza la reserva.
import pe.sportcenter.modelo.Cliente;
import pe.sportcenter.modelo.EstadoCancha;
import pe.sportcenter.modelo.IngresoPorDiaSemana;
import pe.sportcenter.modelo.OcupacionCancha;
import pe.sportcenter.modelo.ResumenAlquiler;
import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;
// IMPORT: Concentra los horarios válidos y la duración mínima.
import pe.sportcenter.util.ReglasAlquiler;

import java.io.IOException;
// IMPORT: Tipo numérico exacto para el dinero: evita errores por usar double.
import java.math.BigDecimal;
import java.sql.SQLException;
// IMPORT: Fecha sin hora, útil para reservar un día concreto.
import java.time.LocalDate;
// IMPORT: Fecha+hora usada para impedir reprogramar reservas ya iniciadas.
import java.time.LocalDateTime;
// IMPORT: Hora local, útil para inicio y fin del alquiler.
import java.time.LocalTime;
// IMPORT: Define America/Lima en comparaciones con la hora actual.
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Coordina disponibilidad, alquileres y reportes para usuarios autorizados. Valida entradas,
 * utiliza la hora de Lima y registra las operaciones de alquiler en el log.
 */
// ESTA CLASE: CONTROLADOR DE ALQUILERES: autorizacion, validaciones y coordinacion con el DAO.
public class AlquilerControlador {

    private static final Logger LOG = LoggerFactory.getLogger(AlquilerControlador.class);

    // CAMPO: La zona horaria es America/Lima; controla comparaciones con la hora actual.
    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    // CAMPO: Dependencia del DAO: acceso a datos mediante una interfaz.
    private final AlquilerDAO alquilerDAO;
    // CAMPO: Dependencia del DAO: acceso a datos mediante una interfaz.
    private final CanchaDAO canchaDAO;
    // CAMPO: Dependencia del DAO: acceso a datos mediante una interfaz.
    private final ClienteDAO clienteDAO;
    // CAMPO: Referencia a la sesion para comprobar permisos.
    private final AutenticacionControlador autenticacion;

    // EXPLICACIÓN DEL MÉTODO: Recibe las dependencias DAO y autenticacion; no crea SQL ni componentes de la interfaz.
    public AlquilerControlador(
            AlquilerDAO alquilerDAO,
            CanchaDAO canchaDAO,
            ClienteDAO clienteDAO,
            AutenticacionControlador autenticacion) {

        this.alquilerDAO = Objects.requireNonNull(alquilerDAO);
        this.canchaDAO = Objects.requireNonNull(canchaDAO);
        this.clienteDAO = Objects.requireNonNull(clienteDAO);
        this.autenticacion = Objects.requireNonNull(autenticacion);
    }

    // RF10: incluye canchas inactivas para consultar su historial.
    // EXPLICACIÓN DEL MÉTODO: Devuelve todas las canchas, incluidas inactivas, para conservar informacion historica en
    //     reportes.
    //  CLAVE REPORTE: permite mostrar TODAS las canchas en el selector, incluso inactivas.
    public List<Cancha> listarCanchasParaReporte() throws SQLException, IOException {

        exigirPermiso();
        // PASO (listarCanchasParaReporte): Devuelve el dato calculado o la respuesta de esta operacion.
        return canchaDAO.listarTodas();
    }

    /** Filtra canchas disponibles o alquiladas; el horario libre se comprueba por separado. */
    // EXPLICACIÓN DEL MÉTODO: Permite seleccionar canchas con estados generales disponible o alquilada; los cruces reales se
    //     verifican por horario.
    public List<Cancha> listarCanchasHabilitadas() throws SQLException, IOException {

        exigirPermiso();

        // PASO (listarCanchasHabilitadas): Devuelve el dato calculado o la respuesta de esta operacion.
        return canchaDAO.listarTodas().stream()
                .filter(
                        cancha ->
                                cancha.getEstado() == EstadoCancha.DISPONIBLE
                                        || cancha.getEstado() == EstadoCancha.ALQUILADA)
                .toList();
    }

    // RF06: consultar disponibilidad por fecha y horario.
    // EXPLICACIÓN DEL MÉTODO: Verifica sesion y horario, y pide al DAO las canchas sin superposiciones.
    public List<Cancha> consultarDisponibilidad(
            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin)
            throws SQLException, IOException {

        exigirPermiso();

        validarFecha(fecha);
        validarHorario(horaInicio, horaFin);

        // PASO (consultarDisponibilidad): Devuelve el dato calculado o la respuesta de esta operacion.
        return alquilerDAO.listarDisponibles(fecha, horaInicio, horaFin);
    }

    // EXPLICACIÓN DEL MÉTODO: Obtiene los clientes que se pueden elegir al registrar un alquiler.
    public List<Cliente> listarClientes() throws SQLException, IOException {

        exigirPermiso();
        // PASO (listarClientes): Devuelve el dato calculado o la respuesta de esta operacion.
        return clienteDAO.listarTodos();
    }

    // EXPLICACIÓN DEL MÉTODO: Obtiene alquileres de un dia, incluidos los cancelados, para seguimiento.
    public List<Alquiler> listarPorFecha(LocalDate fecha) throws SQLException, IOException {

        exigirPermiso();
        validarFecha(fecha);

        // Permite consultar también fechas pasadas.
        // PASO (listarPorFecha): Devuelve el dato calculado o la respuesta de esta operacion.
        return alquilerDAO.listarPorFecha(fecha);
    }

    // EXPLICACIÓN DEL MÉTODO: Calcula/recupera resúmenes agrupados por estado para reportes (objetivo 3).
    //  CLAVE REPORTE: atajo que consulta todas las canchas porque envia null al filtro.
    //  CLAVE REPORTE: valida fechas, ID opcional y delega resumen por estado al DAO.
    public List<ResumenAlquiler> resumirPorFechas(LocalDate desde, LocalDate hasta)
            throws SQLException, IOException {

        // PASO (resumirPorFechas): Devuelve el dato calculado o la respuesta de esta operacion.
        return resumirPorFechas(desde, hasta, null);
    }

    // RF10: resumen por fechas y cancha.
    // EXPLICACIÓN DEL MÉTODO: Calcula/recupera resúmenes agrupados por estado para reportes (objetivo 3).
    public List<ResumenAlquiler> resumirPorFechas(
            LocalDate desde, LocalDate hasta, Integer idCancha) throws SQLException, IOException {

        exigirPermiso();

        validarFecha(desde);
        validarFecha(hasta);

        // PASO (resumirPorFechas): Comprueba una condicion de negocio o controla un caso de error.
        if (desde.isAfter(hasta)) {
            // PASO (resumirPorFechas): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        // PASO (resumirPorFechas): Rechaza IDs invalidos o cantidades no permitidas.
        if (idCancha != null && idCancha <= 0) {
            // PASO (resumirPorFechas): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una cancha válida.");
        }

        // PASO (resumirPorFechas): Devuelve el dato calculado o la respuesta de esta operacion.
        return alquilerDAO.resumirPorFechas(desde, hasta, idCancha);
    }

    // RF10: identifica las canchas con mayor cantidad de alquileres
    // confirmados y completados en el periodo. Conserva los empates.
    /** Devuelve todas las canchas empatadas en el conteo máximo del periodo. */
    // EXPLICACIÓN DEL MÉTODO: Conserva los empates de la cancha mas alquilada; corresponde al objetivo 3.
    //  CLAVE INDICADOR: busca la mayor cantidad de alquileres entre TODAS las canchas.
    public List<CanchaResumen> obtenerCanchasMasAlquiladas(LocalDate desde, LocalDate hasta)
            throws SQLException, IOException {

        exigirPermiso();
        validarFecha(desde);
        validarFecha(hasta);

        // PASO (obtenerCanchasMasAlquiladas): Comprueba una condicion de negocio o controla un caso de error.
        if (desde.isAfter(hasta)) {
            // PASO (obtenerCanchasMasAlquiladas): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        //  El DAO ya los devuelve ordenados desde el conteo mayor.
        List<CanchaResumen> resumenes = alquilerDAO.resumirAlquileresPorCancha(desde, hasta);

        // PASO (obtenerCanchasMasAlquiladas): Comprueba una condicion de negocio o controla un caso de error.
        if (resumenes.isEmpty()) {
            // PASO (obtenerCanchasMasAlquiladas): Devuelve una lista vacia cuando no existen resultados.
            return List.of();
        }

        //  El primer elemento de la lista contiene la cantidad maxima.
        long cantidadMaxima = resumenes.get(0).getCantidadAlquileres();

        // PASO (obtenerCanchasMasAlquiladas): Devuelve el dato calculado o la respuesta de esta operacion.
        return resumenes.stream()
                //  Se conservan todas las canchas empatadas en el maximo, no solo la primera.
                .filter(resumen -> resumen.getCantidadAlquileres() == cantidadMaxima)
                .toList();
    }

    // RF10: dia(s) con mayores ingresos acumulados del periodo y cancha.
    // Incluye todos los empates en el importe maximo.
    /** Conserva todos los días de la semana empatados en el importe acumulado máximo. */
    // EXPLICACIÓN DEL MÉTODO: Busca los dias con los importes acumulados mas altos; objetivo 3.
    //  CLAVE INDICADOR: recupera acumulados por dia de semana y conserva empates.
    public List<IngresoPorDiaSemana> obtenerDiasConMayoresIngresos(
            LocalDate desde, LocalDate hasta, Integer idCancha) throws SQLException, IOException {

        exigirPermiso();
        validarFecha(desde);
        validarFecha(hasta);

        // PASO (obtenerDiasConMayoresIngresos): Comprueba una condicion de negocio o controla un caso de error.
        if (desde.isAfter(hasta)) {
            // PASO (obtenerDiasConMayoresIngresos): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        // PASO (obtenerDiasConMayoresIngresos): Rechaza IDs invalidos o cantidades no permitidas.
        if (idCancha != null && idCancha <= 0) {
            // PASO (obtenerDiasConMayoresIngresos): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una cancha válida.");
        }

        //  Solicita al DAO importes acumulados por dia con el filtro de cancha.
        List<IngresoPorDiaSemana> ingresos =
                alquilerDAO.resumirIngresosPorDiaSemana(desde, hasta, idCancha);

        // PASO (obtenerDiasConMayoresIngresos): Comprueba una condicion de negocio o controla un caso de error.
        if (ingresos.isEmpty()) {
            // PASO (obtenerDiasConMayoresIngresos): Devuelve una lista vacia cuando no existen resultados.
            return List.of();
        }

        //  El primer monto es el mayor gracias a ORDER BY total DESC.
        BigDecimal montoMaximo = ingresos.get(0).montoTotal();

        // PASO (obtenerDiasConMayoresIngresos): Devuelve el dato calculado o la respuesta de esta operacion.
        return ingresos.stream()
                //  BigDecimal.compareTo compara montos sin depender de su escala decimal.
                .filter(ingreso -> ingreso.montoTotal().compareTo(montoMaximo) == 0)
                .toList();
    }

    /** Exige una sesión autorizada y un horario futuro; asigna el usuario desde la sesión. */
    //Este método se encarga de registrar un nuevo alquiler. Primero verifica que el usuario tenga permiso para realizar la operación, 
    // después valida la cancha, el cliente, la fecha y el horario. Finalmente, envía los datos al DAO, donde se verifica la disponibilidad,
    // se calcula el importe y se guarda el alquiler en MySQL.


        public Alquiler registrar(
            int idCancha, int idCliente, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin)
        //Declara que pueden producirse errores relacionados con la base de datos o con operaciones de entrada y salida.
            throws SQLException, IOException {

            //ulizamos exigirPermiso() para verificar que el usuario tenga una sesión válida y permiso para registrar alquileres. 
            // También obtenemos sus datos para identificar quién está realizando el registro.
        Usuario usuario = exigirPermiso();

        // : Rechaza IDs invalidos o cantidades no permitidas.
            //tenemos dos condiciones if que verifican que los identificadores de la cancha y del cliente sean mayores que cero.
            // Si alguno no es válido, lanzamos una excepción IllegalArgumentException con un mensaje
            // indicando qué debe seleccionar el usuario.
        if (idCancha <= 0) {
            // : Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una cancha.");
        }

        // : Rechaza IDs invalidos o cantidades no permitidas.
        if (idCliente <= 0) {
            // : Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione un cliente.");
        }

// llamamos a los métodos validarFecha y validarHorario, que comprueban que la fecha y las horas ingresadas
// cumplan con las reglas establecidas en nuestro sistema.
        validarFecha(fecha);
        validarHorario(horaInicio, horaFin);

        // : Combina fecha y hora para compararlas con el reloj de Lima.
        LocalDateTime inicio = LocalDateTime.of(fecha, horaInicio);
//Aquí utilizamos LocalDateTime para combinar la fecha y la hora de inicio del alquiler. Luego la comparamos con la fecha y hora
// actuales de Lima. Si el horario ya pasó o coincide con el momento actual, el sistema no permite registrar el alquiler.
        // : Exige un inicio posterior al momento/limite indicado.
        if (!inicio.isAfter(LocalDateTime.now(ZONA))) {
            // : Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "El alquiler debe comenzar en una fecha " + "y hora futuras.");
        }
//Una vez validados los datos, llamamos a alquilerDAO.registrar(), enviándole la información del alquiler y el ID del usuario responsable,
// que obtenemos de la sesión. El DAO se encarga de verificar la disponibilidad, calcular automáticamente el importe y registrar la reserva en MySQL.

            //Aquí el controlador llama al método registrar() de alquilerDAO.
        Alquiler registrado =
                // : Delegación: el DAO valida cruces, calcula el monto e inserta en una transacción.
                alquilerDAO.registrar(
                        idCancha, idCliente, usuario.getIdUsuario(), fecha, horaInicio, horaFin);

        // : Escribe en el log que se realizo la accion para seguimiento.
            //Después utilizamos LOG.info() para registrar un mensaje de seguimiento con los datos principales del alquiler.
            // Esto nos ayuda a identificar qué operación se realizó y quién fue el responsable.
        LOG.info(
                "Alquiler registrado: idAlquiler={}, idCancha={}, " + "idUsuario={}, montoTotal={}",
                registrado.getIdAlquiler(),
                registrado.getIdCancha(),
                usuario.getIdUsuario(),
                registrado.getMontoTotal());

        // Finalmente, devolvemos el objeto registrado, que contiene la información del alquiler guardado, incluyendo su identificador y el importe calculado.
        return registrado;
    }

    // RF07 y RF08: modificar un alquiler y recalcular su importe.
    /** Valida el nuevo horario y registra en el log al usuario que realiza la modificación. */
    //  OBJETIVO 2: valida que el nuevo inicio sea futuro y envia al DAO los nuevos datos del alquiler.
    public Alquiler reprogramar(
            int idAlquiler,
            int idCancha,
            int idCliente,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException {

        Usuario usuario = exigirPermiso();

        // PASO (reprogramar): Rechaza IDs invalidos o cantidades no permitidas.
        if (idAlquiler <= 0) {
            // PASO (reprogramar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione un alquiler válido.");
        }

        // PASO (reprogramar): Rechaza IDs invalidos o cantidades no permitidas.
        if (idCancha <= 0) {
            // PASO (reprogramar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una cancha.");
        }

        // PASO (reprogramar): Rechaza IDs invalidos o cantidades no permitidas.
        if (idCliente <= 0) {
            // PASO (reprogramar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione un cliente.");
        }

        validarFecha(fecha);
        validarHorario(horaInicio, horaFin);

        // PASO (reprogramar): Combina fecha y hora para compararlas con el reloj de Lima.
        LocalDateTime nuevoInicio = LocalDateTime.of(fecha, horaInicio);

        // PASO (reprogramar): Exige un inicio posterior al momento/limite indicado.
        if (!nuevoInicio.isAfter(LocalDateTime.now(ZONA))) {
            // PASO (reprogramar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("El nuevo horario debe comenzar en el futuro.");
        }

        // PASO (reprogramar): Recibe del DAO el alquiler actualizado; incluye el monto recalculado.
        Alquiler modificado =
                // PASO (reprogramar): Delegación: el DAO comprueba cruces, recalcula e intenta UPDATE.
                alquilerDAO.reprogramar(
                        idAlquiler, idCancha, idCliente, fecha, horaInicio, horaFin);

        // PASO (reprogramar): Escribe en el log que se realizo la accion para seguimiento.
        LOG.info(
                "Alquiler reprogramado: idAlquiler={}, "
                        + "idCancha={}, idUsuario={}, montoTotal={}",
                modificado.getIdAlquiler(),
                modificado.getIdCancha(),
                usuario.getIdUsuario(),
                modificado.getMontoTotal());

        // PASO (reprogramar): Devuelve el dato calculado o la respuesta de esta operacion.
        return modificado;
    }

    /** Solicita la cancelación y registra al usuario responsable en el log. */
    // CLAVE OBJETIVO 2: cambia a cancelado y registra en el log el usuario que hizo la operacion.
    public void cancelar(int idAlquiler) throws SQLException, IOException {

        Usuario usuario = exigirPermiso();

        // PASO (cancelar): Rechaza IDs invalidos o cantidades no permitidas.
        if (idAlquiler <= 0) {
            // PASO (cancelar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione un alquiler válido.");
        }

        // PASO (cancelar): Consulta al DAO si logro cambiar el alquiler a cancelado.
        boolean cancelado = alquilerDAO.cancelar(idAlquiler);

        // PASO (cancelar): Comprueba una condicion de negocio o controla un caso de error.
        if (!cancelado) {
            // PASO (cancelar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "No se pudo cancelar: el alquiler no existe " + "o ya no está confirmado.");
        }

        // PASO (cancelar): Escribe en el log que se realizo la accion para seguimiento.
        LOG.info(
                "Alquiler cancelado: idAlquiler={}, idUsuario={}",
                idAlquiler,
                usuario.getIdUsuario());
    }

    /** Usa la hora de Lima para solicitar la finalización del alquiler y registrar el evento. */
    // CLAVE OBJETIVO 2: usa la hora de Lima y solicita completar un alquiler confirmado ya terminado.
    public void completar(int idAlquiler) throws SQLException, IOException {

        Usuario usuario = exigirPermiso();

        // PASO (completar): Rechaza IDs invalidos o cantidades no permitidas.
        if (idAlquiler <= 0) {
            // PASO (completar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione un alquiler válido.");
        }

        // Usa la hora de Lima, sin pedirla al usuario.
        // PASO (completar): Combina fecha y hora para compararlas con el reloj de Lima.
        LocalDateTime ahora = LocalDateTime.now(ZONA);

        // PASO (completar): Consulta al DAO si logro completar el alquiler.
        boolean completado = alquilerDAO.completar(idAlquiler, ahora);

        // PASO (completar): Comprueba una condicion de negocio o controla un caso de error.
        if (!completado) {
            // PASO (completar): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "No se pudo completar el alquiler. "
                            + "Debe existir, estar confirmado "
                            + "y haber terminado su horario.");
        }

        // PASO (completar): Escribe en el log que se realizo la accion para seguimiento.
        LOG.info(
                "Alquiler completado: idAlquiler={}, idUsuario={}",
                idAlquiler,
                usuario.getIdUsuario());
    }

    /** Comprueba la sesión y los perfiles autorizados antes de acceder al módulo. */
    // EXPLICACIÓN DEL MÉTODO: Exige iniciar sesion con rol administrador o recepcionista antes de gestionar alquileres.
    private Usuario exigirPermiso() {
        Usuario usuario = autenticacion.getUsuarioActual();

        // PASO (exigirPermiso): Evita seguir si falta un dato necesario para la operacion.
        if (usuario == null) {
            // PASO (exigirPermiso): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new SecurityException("Debe iniciar sesión para gestionar alquileres.");
        }

        // PASO (exigirPermiso): Comprueba una condicion de negocio o controla un caso de error.
        if (usuario.getRol() != Rol.ADMINISTRADOR && usuario.getRol() != Rol.RECEPCIONISTA) {

            // PASO (exigirPermiso): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new SecurityException("No tiene permisos para gestionar alquileres.");
        }

        // PASO (exigirPermiso): Devuelve el dato calculado o la respuesta de esta operacion.
        return usuario;
    }

    /** Comprueba que la fecha exista y se encuentre en el rango DATE admitido por MySQL. */
    // EXPLICACIÓN DEL MÉTODO: Exige una fecha valida para los datos DATE de MySQL.
    private void validarFecha(LocalDate fecha) {
        // PASO (validarFecha): Evita seguir si falta un dato necesario para la operacion.
        if (fecha == null) {
            // PASO (validarFecha): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }

        // Rango admitido por el tipo DATE de MySQL.
        // PASO (validarFecha): Comprueba una condicion de negocio o controla un caso de error.
        if (fecha.getYear() < 1000 || fecha.getYear() > 9999) {
            // PASO (validarFecha): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("La fecha debe tener un año entre 1000 y 9999.");
        }
    }

    // EXPLICACIÓN DEL MÉTODO: Reutiliza las reglas comunes (apertura, cierre, minimo y bloques).
    private void validarHorario(LocalTime horaInicio, LocalTime horaFin) {

        ReglasAlquiler.validarHorario(horaInicio, horaFin);
    }

    // RF10: consultar la ocupación por cancha.
    // EXPLICACIÓN DEL MÉTODO: Consulta minutos reservados sobre capacidad teorica; pertenece al objetivo 3.
    //  CLAVE OCUPACION: valida filtros y consulta minutos reservados y capacidad por cancha.
    public List<OcupacionCancha> consultarOcupacion(
            LocalDate desde, LocalDate hasta, Integer idCancha) throws SQLException, IOException {

        exigirPermiso();

        validarFecha(desde);
        validarFecha(hasta);

        // PASO (consultarOcupacion): Comprueba una condicion de negocio o controla un caso de error.
        if (desde.isAfter(hasta)) {
            // PASO (consultarOcupacion): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        // PASO (consultarOcupacion): Rechaza IDs invalidos o cantidades no permitidas.
        if (idCancha != null && idCancha <= 0) {
            // PASO (consultarOcupacion): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una cancha válida.");
        }

        // PASO (consultarOcupacion): Devuelve el dato calculado o la respuesta de esta operacion.
        //  Pide al DAO minutos reservados y capacidad teorica de las canchas.
        return alquilerDAO.consultarOcupacion(desde, hasta, idCancha);
    }
}
