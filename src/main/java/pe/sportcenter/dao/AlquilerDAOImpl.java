// ============================================================================
// GUÍA COMENTADA DE SPORTCENTER — SEGUNDA CONCLUSIÓN
// TEMA: PERSISTENCIA: SELECT, INSERT, UPDATE; transacciones, bloqueos, cruces e importe
// Solo se insertaron comentarios. Los datos, instrucciones y SQL son ORIGINALES.
// Busca «CLAVE OBJETIVO 2» para llegar rápido a lo esencial de tu exposición.
// IMPORTANTE: Los reportes corresponden a la tercera conclusión.
// ============================================================================

package pe.sportcenter.dao;

// IMPORT: Modelo de una reserva registrada.
import pe.sportcenter.modelo.Alquiler;
// IMPORT: Modelo para la cancha seleccionada.
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.CanchaResumen;
import pe.sportcenter.modelo.DeporteCancha;
// IMPORT: Define confirmado, cancelado y completado.
import pe.sportcenter.modelo.EstadoAlquiler;
import pe.sportcenter.modelo.EstadoCancha;
import pe.sportcenter.modelo.IngresoPorDiaSemana;
import pe.sportcenter.modelo.OcupacionCancha;
import pe.sportcenter.modelo.ResumenAlquiler;
// IMPORT: Centraliza la apertura de la conexión a MySQL.
import pe.sportcenter.util.Conexion;
// IMPORT: Concentra los horarios válidos y la duración mínima.
import pe.sportcenter.util.ReglasAlquiler;

import java.io.IOException;
// IMPORT: Tipo numérico exacto para el dinero: evita errores por usar double.
import java.math.BigDecimal;
// IMPORT: Modo de redondeo, HALF_UP, al dividir monto entre minutos.
import java.math.RoundingMode;
import java.sql.*;
import java.sql.Types;
import java.time.DayOfWeek;
// IMPORT: Calcula la duración real en minutos.
import java.time.Duration;
// IMPORT: Fecha sin hora, útil para reservar un día concreto.
import java.time.LocalDate;
// IMPORT: Fecha+hora usada para impedir reprogramar reservas ya iniciadas.
import java.time.LocalDateTime;
// IMPORT: Hora local, útil para inicio y fin del alquiler.
import java.time.LocalTime;
// IMPORT: Define America/Lima en comparaciones con la hora actual.
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de alquileres y reportes. El registro y la reprogramación utilizan
 * transacciones y bloqueos de filas al comprobar los cruces de horarios. Los reportes de ingresos
 * consideran confirmados y completados, sin verificar cobros.
 */
// ESTA CLASE: PERSISTENCIA: SELECT, INSERT, UPDATE; transacciones, bloqueos, cruces e importe.
public class AlquilerDAOImpl implements AlquilerDAO {

    // CAMPO: La zona horaria es America/Lima; controla comparaciones con la hora actual.
    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    @Override
    // EXPLICACIÓN DEL MÉTODO: Usa SELECT para reconstruir los alquileres guardados del dia.
    public List<Alquiler> listarPorFecha(LocalDate fecha) throws SQLException, IOException {

        // PASO (listarPorFecha Evita seguir si falta un dato necesario para la operacion.
        if (fecha == null) {
            // PASO (listarPorFecha Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una fecha.");
        }

        // PASO (listarPorFecha Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (listarPorFecha SELECT por fecha; ORDER BY ordena por hora y cancha. El ? se reemplaza por setDate.
        String sql =
                """
                SELECT idAlquiler, idCancha, idCliente, idUsuario,
                       fecha, horaInicio, horaFin, montoTotal, estado
                FROM ALQUILER
                WHERE fecha = ?
                ORDER BY horaInicio, idCancha, idAlquiler
                """;

        // PASO (listarPorFecha Crea la lista que se llenara leyendo filas de MySQL.
        List<Alquiler> alquileres = new ArrayList<>();

        // PASO (listarPorFecha Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            // PASO (listarPorFecha Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(1, Date.valueOf(fecha));

            // PASO (listarPorFecha Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            try (ResultSet resultado = consulta.executeQuery()) {
                // PASO (listarPorFecha Lee resultados hasta que ya no queden filas.
                while (resultado.next()) {
                    alquileres.add(convertirAlquiler(resultado));
                }
            }
        }

        // PASO (listarPorFecha Devuelve el dato calculado o la respuesta de esta operacion.
        return alquileres;
    }

    // RF06: consultar canchas habilitadas que no tengan un alquiler superpuesto.
    @Override
    //objetivo 1.
    //permite saber qué canchas están disponibles en una fecha y un horario determinados, evitando que se crucen las reservas.
    //Este método se encarga de consultar las canchas disponibles.
    // Recibe una fecha, una hora de inicio y una hora de fin. Con esos datos realiza una consulta SQL a nuestra base de datos MySQL,
    // verifica que las canchas no tengan reservas que se crucen con ese horario y devuelve una lista de las canchas disponibles.


    public List<Cancha> listarDisponibles(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin)
            throws SQLException, IOException {
//SQLException	 lanzar errores relacionados con operaciones de la base de datos.
//IOException	 propagar errores de entrada y salida, por ejemplo, al leer la configuración de conexión

//validaciones
        validarFecha(fecha);
        ReglasAlquiler.validarHorario(horaInicio, horaFin);


        //se guarda en text blocks (las triples comillas """)

        //Crea un texto con una consulta SQL que busca todas las canchas que estén activas y que
        // NO tengan ningún alquiler cruzado o choque de horarios para una fecha y hora específicas, ordenándolas por su ID

        //NOT EXISTS significa que no debe existir ningún registro que cumpla las condiciones dentro de la subconsulta.
        //WHERE c.estado IN ('disponible', 'alquilada') Filtra para que solo tome en cuenta canchas que estén en un estado válido para ser mostradas
        String sql =
                """
                SELECT c.idCancha, c.nombre, c.deporte,
                       c.tarifaHora, c.estado
                FROM CANCHA c
                WHERE c.estado IN ('disponible', 'alquilada')
                  AND NOT EXISTS (
                      SELECT 1
                      FROM ALQUILER a
                      WHERE a.idCancha = c.idCancha
                        AND a.fecha = ?
                        AND a.estado IN ('confirmado', 'completado')
                        AND a.horaInicio < ?
                        AND a.horaFin > ?
                  )
                ORDER BY c.idCancha
                """;

        //  Crea la lista que se llenara leyendo filas de MySQL.
        //Aquí creamos una lista de tipo Cancha, donde vamos a almacenar los registros disponibles que obtengamos de la consulta.
        List<Cancha> disponibles = new ArrayList<>();

        //Luego abrimos una conexión con MySQL y utilizamos PreparedStatement para preparar la consulta SQL.
        //Esto nos permite enviar los parámetros de forma segura. Además, utilizamos try-with-resources para cerrar los recursos automáticamente.

        //Connection , representa la conexión con MySQL.
        //Conexion.getInstancia().abrirConexion() obtiene y abre la conexión
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {


 //Aquí utilizamos setDate y setTime para enviar la fecha y las horas a los parámetros de nuestra consulta SQL.
            // Estos valores permiten comparar el horario solicitado con los alquileres existentes.
            //  Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(1, Date.valueOf(fecha));
            //  Envia la hora al parametro indicado de la consulta SQL.
            consulta.setTime(2, Time.valueOf(horaFin));
            //  Envia la hora al parametro indicado de la consulta SQL.
            consulta.setTime(3, Time.valueOf(horaInicio));


//Después ejecutamos la consulta utilizando executeQuery(), que nos devuelve los resultados mediante un ResultSet.
//
//Con un ciclo while recorremos cada registro obtenido, creamos un objeto de tipo Cancha y lo agregamos a nuestra lista de disponibles.
            //  Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            //consulta.executeQuery(), Ejecuta el SELECT en MySQL.
            //ResultSet resultado, Recibe los registros obtenidos de la consulta.
            try (ResultSet resultado = consulta.executeQuery()) {
                //  Lee resultados hasta que ya no queden filas.
                while (resultado.next()) {
                    //Crea un objeto Cancha con los datos de cada fila y lo agrega a la lista.
                    disponibles.add(
                            new Cancha(
                                    resultado.getInt("idCancha"),
                                    //Obtiene el nombre de la cancha desde MySQL.
                                    resultado.getString("nombre"),
                                    //Convierte el texto almacenado en MySQL al tipo enumerado DeporteCancha
                                    DeporteCancha.desdeBD(resultado.getString("deporte")),
                                    resultado.getBigDecimal("tarifaHora"),
                                    EstadoCancha.desdeBD(resultado.getString("estado"))));
                }
            }
        }

        //  Finalmente, el
        //método devuelve la lista construida.
        //Finalmente, utilizamos return disponibles para devolver la lista de canchas que cumplen las condiciones de disponibilidad.
        // Esa información puede ser utilizada por el controlador y mostrada en la interfaz.
        return disponibles;
    }

    /**
     * Bloquea la cancha, comprueba cruces y calcula tarifa por minutos dentro de una transacción.
     */
    @Override
    //En este método realizamos el registro del alquiler en la base de datos.
    // Primero validamos los datos, después verificamos la disponibilidad de la cancha y obtenemos su tarifa por hora. Con esa
    // información calculamos automáticamente el importe del alquiler y finalmente guardamos el registro en MySQL.
    //Además, utilizamos una transacción para evitar que se guarden operaciones incompletas.
    public Alquiler registrar(
            int idCancha,
            int idCliente,
            int idUsuario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException {
//Primero llamamos a validarDatos(), donde verificamos los parámetros recibidos para evitar registrar información incorrecta.
        validarDatos(idCancha, idCliente, idUsuario, fecha, horaInicio, horaFin);

        //  Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        //Después abrimos una conexión con nuestra base de datos MySQL mediante la clase Conexion.
        // Utilizamos un try-with-resources para que la conexión se cierre automáticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion()) {

            // Comienza la transacción.
            // Normalmente, cuando se realiza una operación SQL, puede confirmarse automáticamente.
            //
            //Con setAutoCommit(false) desactivamos esa confirmación automática para controlar cuándo se guardan definitivamente los cambios..
            conexion.setAutoCommit(false);

            //  Comienza un bloque protegido: permite controlar errores y liberar recursos.
            try {
                // llamamos a obtenerTarifaYBloquearCancha(). Este método obtiene la tarifa actual de la cancha y bloquea temporalmente su
                //  registro mientras realizamos la reserva. Esto ayuda a evitar conflictos cuando dos usuarios intentan registrar un alquiler al mismo tiempo.
                BigDecimal tarifaHora = obtenerTarifaYBloquearCancha(conexion, idCancha);

                //  Comprueba que otro alquiler no ocupe una parte del mismo intervalo.
                comprobarHorario(conexion, idCancha, fecha, horaInicio, horaFin);

                //  Calcula la duracion del alquiler entre hora inicial y hora final.
                //Aquí aparecen dos elementos:
                //- Duration.between() calcula la duración entre dos horas.
                //- .toMinutes() convierte esa duración a minutos.
                long minutos = Duration.between(horaInicio, horaFin).toMinutes();

                //  monto = tarifaHora × minutos / 60. BigDecimal evita errores de redondeo binario.
                // montoTotal = tarifaHora * duracionMinutos / 60.
                // EJEMPLO: Si la cancha cuesta S/ 60 por hora y el cliente la reserva durante 90 minutos: => 60 * 90 / 60 = S/ 90.00.
                //Esta es la parte donde solucionamos el problema del cálculo manual de tarifas. Utilizamos BigDecimal para trabajar con importes monetarios.
                // Primero multiplicamos la tarifa por hora por los minutos reservados y después dividimos entre 60 para obtener el monto total.
                // Finalmente, utilizamos RoundingMode.HALF_UP para manejar el redondeo a dos decimales.
                BigDecimal montoTotal =
                        tarifaHora
                                .multiply(BigDecimal.valueOf(minutos))
                                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

                //  Inserta el alquiler y conserva el identificador generado por MySQL.
                //Después llamamos al método insertar(), donde enviamos todos los datos del alquiler,
                // incluyendo el importe calculado. Este método ejecuta el INSERT en MySQL y nos devuelve el ID del nuevo registro.
                int idAlquiler =
                        insertar(
                                conexion,
                                idCancha,
                                idCliente,
                                idUsuario,
                                fecha,
                                horaInicio,
                                horaFin,
                                montoTotal);

                // Luego creamos un objeto Alquiler con los datos de la reserva, el importe calculado y el ID generado por MySQL. Además,
                // le asignamos el estado inicial CONFIRMADO
                Alquiler registrado =
                        new Alquiler(
                                idAlquiler,
                                idCancha,
                                idCliente,
                                idUsuario,
                                fecha,
                                horaInicio,
                                horaFin,
                                montoTotal,
                                EstadoAlquiler.CONFIRMADO);

                //Una vez que todas las operaciones terminan correctamente, utilizamos commit() para confirmar los cambios en la base de datos.
                conexion.commit();

                // devolvemos el objeto registrado, que contiene toda la información del alquiler, incluyendo su importe final.
                return registrado;

                //Este bloque se ejecuta si ocurre una excepción SQL o una excepción de ejecución dentro del registro.
                //- catch: captura la excepción.
                //- rollback(): deshace los cambios pendientes de esa transacción.
                //- addSuppressed(): conserva información sobre un posible error adicional al intentar deshacer los cambios.
                //- throw e: vuelve a lanzar la excepción para que otra parte del sistema pueda manejarla.

            } catch (SQLException | RuntimeException e) {
                // Si falla alguna operación, deshace el registro.
                //  Comienza un bloque protegido: permite controlar errores y liberar recursos.
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                throw e;
            }
        }
    }

    // RF07: reprogramar un alquiler confirmado que aún no comienza.
    // RF08: calcular el nuevo monto al cambiar cancha o duración.
    /**
     * Modifica un alquiler confirmado que aún no empieza, conserva su ID y usuario original y
     * recalcula el importe con la tarifa vigente de la cancha seleccionada.
     */
    @Override
    // CLAVE OBJETIVO 2: bloquea cancha y alquiler, valida que sea confirmado y futuro, excluye su propio ID, recalcula
    //     y actualiza.
    public Alquiler reprogramar(
            int idAlquiler,
            int idCancha,
            int idCliente,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException, IOException {

        //  Rechaza IDs invalidos o cantidades no permitidas.
        if (idAlquiler <= 0) {
            //  Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione un alquiler válido.");
        }
        //  Rechaza IDs invalidos o cantidades no permitidas.
        if (idCancha <= 0 || idCliente <= 0) {
            //  Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una cancha y un cliente válidos.");
        }
        validarFecha(fecha);
        ReglasAlquiler.validarHorario(horaInicio, horaFin);

        //  Combina fecha y hora para compararlas con el reloj de Lima.
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        //  Combina fecha y hora para compararlas con el reloj de Lima.
        LocalDateTime nuevoInicio = LocalDateTime.of(fecha, horaInicio);
        //  Exige un inicio posterior al momento/limite indicado.
        if (!nuevoInicio.isAfter(ahora)) {
            //  Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("El nuevo horario debe comenzar en el futuro.");
        }

        //  Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion()) {
            //  Comienza una transaccion manual: no se confirma nada hasta hacer commit.
            conexion.setAutoCommit(false);

            //  Comienza un bloque protegido: permite controlar errores y liberar recursos.
            try {
                // Mantiene el bloqueo de la cancha durante toda la operación.
                //  Bloquea la cancha y lee su precio vigente; ese precio entra en el cálculo final.
                BigDecimal tarifaHora = obtenerTarifaYBloquearCancha(conexion, idCancha);

                // Bloquea el alquiler para evitar que se modifique simultáneamente.
                //  Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
                // SQL EXPLICADO (reprogramar SELECT ... FOR UPDATE bloquea el alquiler; UPDATE cambia cancha, cliente, fecha, horas y monto manteniendo el ID.
                // SELECT original: su FOR UPDATE bloquea la fila y evita una edición paralela.
                String sqlOriginal =
                        """
                        SELECT idUsuario, fecha, horaInicio, estado
                        FROM ALQUILER
                        WHERE idAlquiler = ?
                        FOR UPDATE
                        """;
                int idUsuarioOriginal;

                //  Prepara una consulta parametrizada y libera recursos al terminar.
                try (PreparedStatement consulta = conexion.prepareStatement(sqlOriginal)) {
                    //  Envia un entero al parametro ? correspondiente del PreparedStatement.
                    consulta.setInt(1, idAlquiler);

                    //  Lee filas devueltas por MySQL y cierra el cursor al finalizar.
                    try (ResultSet resultado = consulta.executeQuery()) {
                        //  Comprueba una condicion de negocio o controla un caso de error.
                        if (!resultado.next()) {
                            //  Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                            throw new IllegalArgumentException("El alquiler no existe.");
                        }
                        //  Comprueba una condicion de negocio o controla un caso de error.
                        if (!"confirmado".equals(resultado.getString("estado"))) {
                            //  Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                            throw new IllegalArgumentException(
                                    "Solo se pueden modificar alquileres confirmados.");
                        }

                        //  Combina fecha y hora para compararlas con el reloj de Lima.
                        LocalDateTime inicioOriginal =
                                LocalDateTime.of(
                                        resultado.getDate("fecha").toLocalDate(),
                                        resultado.getTime("horaInicio").toLocalTime());
                        //  Exige un inicio posterior al momento/limite indicado.
                        if (!inicioOriginal.isAfter(ahora)) {
                            //  Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                            throw new IllegalArgumentException(
                                    "No se puede modificar un alquiler que ya comenzó.");
                        }
                        idUsuarioOriginal = resultado.getInt("idUsuario");
                    }
                }

                // El alquiler que se modifica no debe chocar consigo mismo.
                //  Comprueba cruces excluyendo el propio alquiler que se edita.
                comprobarHorarioReprogramacion(
                        conexion, idAlquiler, idCancha, fecha, horaInicio, horaFin);

                //  Calcula la duracion del alquiler entre hora inicial y hora final.
                long minutos = Duration.between(horaInicio, horaFin).toMinutes();
                //  CLAVE: monto = tarifaHora × minutos / 60. BigDecimal evita errores de redondeo binario.
                // FORMULA : montoTotal = tarifaHora * duracionMinutos / 60.
                // EJEMPLO: S/ 60 por hora y 90 minutos => 60 * 90 / 60 = S/ 90.00.
                BigDecimal montoTotal =
                        tarifaHora
                                .multiply(BigDecimal.valueOf(minutos))
                                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

                //  Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
                // SQL EXPLICADO (reprogramar SELECT ... FOR UPDATE bloquea el alquiler; UPDATE cambia cancha, cliente, fecha, horas y monto manteniendo el ID.
                // UPDATE: conserva el identificador y sustituye fecha, horas, cancha, cliente y precio.
                String sqlActualizar =
                        """
                        UPDATE ALQUILER
                        SET idCancha = ?, idCliente = ?, fecha = ?,
                            horaInicio = ?, horaFin = ?, montoTotal = ?
                        WHERE idAlquiler = ? AND estado = 'confirmado'
                        """;

                //  Prepara una consulta parametrizada y libera recursos al terminar.
                try (PreparedStatement actualizar = conexion.prepareStatement(sqlActualizar)) {
                    //  Envia un entero al parametro ? correspondiente del PreparedStatement.
                    actualizar.setInt(1, idCancha);
                    //  Envia un entero al parametro ? correspondiente del PreparedStatement.
                    actualizar.setInt(2, idCliente);
                    //  Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
                    actualizar.setDate(3, Date.valueOf(fecha));
                    //  Envia la hora al parametro indicado de la consulta SQL.
                    actualizar.setTime(4, Time.valueOf(horaInicio));
                    //  Envia la hora al parametro indicado de la consulta SQL.
                    actualizar.setTime(5, Time.valueOf(horaFin));
                    //  Envia el importe monetario como decimal preciso.
                    actualizar.setBigDecimal(6, montoTotal);
                    //  Envia un entero al parametro ? correspondiente del PreparedStatement.
                    actualizar.setInt(7, idAlquiler);

                    //  Comprueba una condicion de negocio o controla un caso de error.
                    if (actualizar.executeUpdate() != 1) {
                        //  Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                        throw new SQLException("No se pudo modificar el alquiler.");
                    }
                }

                //  COMMIT: confirma de forma definitiva las modificaciones de esta transaccion.
                conexion.commit();
                //  Devuelve el dato calculado o la respuesta de esta operacion.
                return new Alquiler(
                        idAlquiler,
                        idCancha,
                        idCliente,
                        idUsuarioOriginal,
                        fecha,
                        horaInicio,
                        horaFin,
                        montoTotal,
                        EstadoAlquiler.CONFIRMADO);

            } catch (SQLException | RuntimeException e) {
                //  Comienza un bloque protegido: permite controlar errores y liberar recursos.
                try {
                    //  ROLLBACK: revierte todos los cambios de una transaccion que fallo.
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                throw e;
            }
        }
    }

    /** Lee la tarifa vigente y bloquea la cancha hasta que termine la transacción. */
    // EXPLICACIÓN DEL MÉTODO: Lee tarifa vigente y estado de la cancha con SELECT ... FOR UPDATE para evitar conflictos
    //     simultaneos.
    private BigDecimal obtenerTarifaYBloquearCancha(Connection conexion, int idCancha)
            throws SQLException {

        // PASO (obtenerTarifaYBloquearCancha Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (obtenerTarifaYBloquearCancha SELECT ... FOR UPDATE bloquea una fila CANCHA; comprueba estado y precio actual.
        String sql =
                """
                SELECT tarifaHora, estado
                FROM CANCHA
                WHERE idCancha = ?
                FOR UPDATE
                """;

        // PASO (obtenerTarifaYBloquearCancha Prepara una consulta parametrizada y libera recursos al terminar.
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {

            // PASO (obtenerTarifaYBloquearCancha Envia un entero al parametro ? correspondiente del PreparedStatement.
            consulta.setInt(1, idCancha);

            // PASO (obtenerTarifaYBloquearCancha Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            try (ResultSet resultado = consulta.executeQuery()) {

                // PASO (obtenerTarifaYBloquearCancha Comprueba una condicion de negocio o controla un caso de error.
                if (!resultado.next()) {
                    // PASO (obtenerTarifaYBloquearCancha Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                    throw new IllegalArgumentException("La cancha seleccionada ya no existe.");
                }

                String estado = resultado.getString("estado");

                // PASO (obtenerTarifaYBloquearCancha Comprueba una condicion de negocio o controla un caso de error.
                if (!"disponible".equals(estado) && !"alquilada".equals(estado)) {
                    // PASO (obtenerTarifaYBloquearCancha Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                    throw new IllegalArgumentException(
                            "La cancha no está habilitada para alquileres. "
                                    + "Puede estar en mantenimiento o inactiva.");
                }

                BigDecimal tarifa = resultado.getBigDecimal("tarifaHora");

                // PASO (obtenerTarifaYBloquearCancha Comprueba una condicion de negocio o controla un caso de error.
                if (tarifa == null
                        || tarifa.compareTo(new BigDecimal("10.00")) < 0
                        || tarifa.compareTo(new BigDecimal("500.00")) > 0) {

                    // PASO (obtenerTarifaYBloquearCancha Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                    throw new IllegalArgumentException("La cancha tiene una tarifa inválida.");
                }

                // PASO (obtenerTarifaYBloquearCancha Devuelve el dato calculado o la respuesta de esta operacion.
                return tarifa;
            }
        }
    }

    /**
     * Rechaza cruces: inicio existente menor que fin nuevo y fin existente mayor que inicio nuevo.
     * Permite reservas consecutivas que comparten únicamente el instante de inicio o fin.
     */
    // EXPLICACIÓN DEL MÉTODO: Impide insertar un alquiler si existe otro confirmado/completado que se superpone en esa
    //     cancha y fecha.
    private void comprobarHorario(
            Connection conexion,
            int idCancha,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException {

        // PASO (comprobarHorario Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (comprobarHorario SELECT detecta cualquier solapamiento; LIMIT 1 basta para rechazar una nueva reserva.
        String sql =
                """
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

        // PASO (comprobarHorario Prepara una consulta parametrizada y libera recursos al terminar.
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {

            // PASO (comprobarHorario Envia un entero al parametro ? correspondiente del PreparedStatement.
            consulta.setInt(1, idCancha);
            // PASO (comprobarHorario Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(2, Date.valueOf(fecha));
            // PASO (comprobarHorario Envia la hora al parametro indicado de la consulta SQL.
            consulta.setTime(3, Time.valueOf(horaFin));
            // PASO (comprobarHorario Envia la hora al parametro indicado de la consulta SQL.
            consulta.setTime(4, Time.valueOf(horaInicio));

            // PASO (comprobarHorario Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            try (ResultSet resultado = consulta.executeQuery()) {
                // PASO (comprobarHorario Comprueba una condicion de negocio o controla un caso de error.
                if (resultado.next()) {
                    // PASO (comprobarHorario Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                    throw new IllegalArgumentException(
                            "La cancha ya tiene un alquiler " + "que se cruza con ese horario.");
                }
            }
        }
    }

    // RF06: comprueba cruces excluyendo el alquiler que se está editando.
    /** Comprueba cruces excluyendo el propio alquiler que se está modificando. */
    // EXPLICACIÓN DEL MÉTODO: Hace la comprobacion anterior excluyendo el mismo alquiler que se esta editando.
    private void comprobarHorarioReprogramacion(
            Connection conexion,
            int idAlquiler,
            int idCancha,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin)
            throws SQLException {

        // PASO (comprobarHorarioReprogramacion Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (comprobarHorarioReprogramacion Incluye idAlquiler <> ? para no detectar un conflicto de la reserva consigo misma.
        String sql =
                """
                SELECT idAlquiler
                FROM ALQUILER
                WHERE idCancha = ?
                  AND fecha = ?
                  AND estado IN ('confirmado', 'completado')
                  AND horaInicio < ?
                  AND horaFin > ?
                  AND idAlquiler <> ?
                LIMIT 1
                FOR UPDATE
                """;

        // PASO (comprobarHorarioReprogramacion Prepara una consulta parametrizada y libera recursos al terminar.
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            // PASO (comprobarHorarioReprogramacion Envia un entero al parametro ? correspondiente del PreparedStatement.
            consulta.setInt(1, idCancha);
            // PASO (comprobarHorarioReprogramacion Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(2, Date.valueOf(fecha));
            // PASO (comprobarHorarioReprogramacion Envia la hora al parametro indicado de la consulta SQL.
            consulta.setTime(3, Time.valueOf(horaFin));
            // PASO (comprobarHorarioReprogramacion Envia la hora al parametro indicado de la consulta SQL.
            consulta.setTime(4, Time.valueOf(horaInicio));
            // PASO (comprobarHorarioReprogramacion Envia un entero al parametro ? correspondiente del PreparedStatement.
            consulta.setInt(5, idAlquiler);

            // PASO (comprobarHorarioReprogramacion Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            try (ResultSet resultado = consulta.executeQuery()) {
                // PASO (comprobarHorarioReprogramacion Comprueba una condicion de negocio o controla un caso de error.
                if (resultado.next()) {
                    // PASO (comprobarHorarioReprogramacion Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                    throw new IllegalArgumentException(
                            "La cancha ya tiene otro alquiler que se cruza " + "con ese horario.");
                }
            }
        }
    }

    /** Inserta un alquiler confirmado y recupera el identificador generado por MySQL. */
    // EXPLICACIÓN DEL MÉTODO: INSERT parametrizado en ALQUILER; guarda monto calculado y estado confirmado; recupera ID
    //     generado.
    private int insertar(
            Connection conexion,
            int idCancha,
            int idCliente,
            int idUsuario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            BigDecimal montoTotal)
            throws SQLException {

        // PASO (insertar Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (insertar INSERT INTO ALQUILER crea estado confirmado y almacena la cantidad exacta en montoTotal.
        String sql =
                """
                INSERT INTO ALQUILER
                    (idCancha, idCliente, idUsuario, fecha,
                     horaInicio, horaFin, montoTotal, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'confirmado')
                """;

        // PASO (insertar Prepara una consulta parametrizada y libera recursos al terminar.
        try (PreparedStatement consulta =
                     conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // PASO (insertar Envia un entero al parametro ? correspondiente del PreparedStatement.
            consulta.setInt(1, idCancha);
            // PASO (insertar Envia un entero al parametro ? correspondiente del PreparedStatement.
            consulta.setInt(2, idCliente);
            // PASO (insertar Envia un entero al parametro ? correspondiente del PreparedStatement.
            consulta.setInt(3, idUsuario);
            // PASO (insertar Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(4, Date.valueOf(fecha));
            // PASO (insertar Envia la hora al parametro indicado de la consulta SQL.
            consulta.setTime(5, Time.valueOf(horaInicio));
            // PASO (insertar Envia la hora al parametro indicado de la consulta SQL.
            consulta.setTime(6, Time.valueOf(horaFin));
            // PASO (insertar Envia el importe monetario como decimal preciso.
            consulta.setBigDecimal(7, montoTotal);

            // PASO (insertar Comprueba una condicion de negocio o controla un caso de error.
            if (consulta.executeUpdate() != 1) {
                // PASO (insertar Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
                throw new SQLException("No se pudo registrar el alquiler.");
            }

            // PASO (insertar Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            try (ResultSet claves = consulta.getGeneratedKeys()) {
                // PASO (insertar Comprueba una condicion de negocio o controla un caso de error.
                if (claves.next()) {
                    // PASO (insertar Devuelve el dato calculado o la respuesta de esta operacion.
                    return claves.getInt(1);
                }
            }

            // PASO (insertar Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new SQLException("No se pudo obtener el ID del alquiler.");
        }
    }

    /** Cambia a cancelado únicamente un registro confirmado; devuelve si se actualizó una fila. */
    @Override
    // CLAVE OBJETIVO 2: UPDATE del estado a cancelado solo si estaba confirmado; NO elimina la fila.
    public boolean cancelar(int idAlquiler) throws SQLException, IOException {

        // PASO (cancelar Rechaza IDs invalidos o cantidades no permitidas.
        if (idAlquiler <= 0) {
            // PASO (cancelar Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione un alquiler válido.");
        }

        // PASO (cancelar Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (cancelar UPDATE estado=cancelado conserva los datos y libera el bloque temporal para nuevas reservas.
        String sql =
                """
                UPDATE ALQUILER
                SET estado = 'cancelado'
                WHERE idAlquiler = ?
                  AND estado = 'confirmado'
                """;

        // PASO (cancelar Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            // PASO (cancelar Envia un entero al parametro ? correspondiente del PreparedStatement.
            consulta.setInt(1, idAlquiler);

            // PASO (cancelar Devuelve el dato calculado o la respuesta de esta operacion.
            return consulta.executeUpdate() == 1;
        }
    }

    /** Completa un alquiler confirmado solo si su fecha y hora de fin ya transcurrieron. */
    @Override
    // CLAVE OBJETIVO 2: marca completado solo si estaba confirmado y termino el horario.
    public boolean completar(int idAlquiler, LocalDateTime ahora) throws SQLException, IOException {

        // PASO (completar Rechaza IDs invalidos o cantidades no permitidas.
        if (idAlquiler <= 0) {
            // PASO (completar Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione un alquiler válido.");
        }

        // PASO (completar Evita seguir si falta un dato necesario para la operacion.
        if (ahora == null) {
            // PASO (completar Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("La fecha y hora de comprobación son obligatorias.");
        }

        // PASO (completar Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (completar UPDATE a completado verifica que la fecha/hora del alquiler ya haya transcurrido.
        String sql =
                """
            UPDATE ALQUILER
            SET estado = 'completado'
            WHERE idAlquiler = ?
              AND estado = 'confirmado'
              AND (
                    fecha < ?
                    OR (fecha = ? AND horaFin <= ?)
                  )
            """;

        // PASO (completar Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            Date fechaActual = Date.valueOf(ahora.toLocalDate());

            // PASO (completar Envia un entero al parametro ? correspondiente del PreparedStatement.
            consulta.setInt(1, idAlquiler);
            // PASO (completar Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(2, fechaActual);
            // PASO (completar Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(3, fechaActual);
            // PASO (completar Envia la hora al parametro indicado de la consulta SQL.
            consulta.setTime(4, Time.valueOf(ahora.toLocalTime()));

            // PASO (completar Devuelve el dato calculado o la respuesta de esta operacion.
            return consulta.executeUpdate() == 1;
        }
    }

    // RF07: los mismos horarios se validan en el controlador y en el DAO.
    // EXPLICACIÓN DEL MÉTODO: Comprueba IDs positivos, fecha y horario antes de tocar MySQL.
    private void validarDatos(
            int idCancha,
            int idCliente,
            int idUsuario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin) {

        // PASO (validarDatos Rechaza IDs invalidos o cantidades no permitidas.
        if (idCancha <= 0 || idCliente <= 0 || idUsuario <= 0) {
            // PASO (validarDatos Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La cancha, el cliente y el usuario deben ser válidos.");
        }

        validarFecha(fecha);
        ReglasAlquiler.validarHorario(horaInicio, horaFin);
    }

    /** Comprueba que la fecha exista y se encuentre en el rango DATE admitido por MySQL. */
    // EXPLICACIÓN DEL MÉTODO: Rechaza fechas nulas o fuera del rango util de MySQL.
    private void validarFecha(LocalDate fecha) {
        // PASO (validarFecha Evita seguir si falta un dato necesario para la operacion.
        if (fecha == null) {
            // PASO (validarFecha Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
        // PASO (validarFecha Comprueba una condicion de negocio o controla un caso de error.
        if (fecha.getYear() < 1000 || fecha.getYear() > 9999) {
            // PASO (validarFecha Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("La fecha debe tener un año entre 1000 y 9999.");
        }
    }

    @Override
    // Obtiene el historial agrupado por estado; corresponde al objetivo 3.
    //  CONSULTA RF10: sobrecarga para todas las canchas del periodo.
    //  CONSULTA RF10: agrupa por estado y calcula conteos, total, promedio, minimo y maximo.
    public List<ResumenAlquiler> resumirPorFechas(LocalDate desde, LocalDate hasta)
            throws SQLException, IOException {

        //  Devuelve el dato calculado o la respuesta de esta operacion.
        return resumirPorFechas(desde, hasta, null);
    }
//Este método se encarga de generar un resumen de los alquileres registrados en un periodo determinado. Consulta nuestra base de datos MySQL
// y obtiene la cantidad de alquileres, el importe total, el promedio, el mínimo y el máximo, agrupándolos según su estado: confirmado,
// completado o cancelado.
    @Override
    //Obtiene el historial agrupado por estado; corresponde al objetivo 3.
    public List<ResumenAlquiler> resumirPorFechas(
            LocalDate desde, LocalDate hasta, Integer idCancha) throws SQLException, IOException {

        validarFecha(desde);
        validarFecha(hasta);

        //  Rechaza IDs invalidos o cantidades no permitidas.
        if (idCancha != null && idCancha <= 0) {
            //  Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una cancha válida.");
        }

        //  Comprueba una condicion de negocio o controla un caso de error.
        if (desde.isAfter(hasta)) {
            //  Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        //  SUM/AVG/MIN/MAX, COUNT(*) agrupan montos y cantidades por estado; es una consulta sobre ALQUILER.
        //  BETWEEN incluye ambos extremos del periodo y (? IS NULL OR idCancha = ?) permite cancha opcional.
        //  Se agrupa por estado: confirmado, completado y cancelado; la VISTA decide que entra en ingresos.
        //  Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        String sql =
                """
            SELECT estado,
           COUNT(*) AS cantidad,
           SUM(montoTotal) AS total,
           AVG(montoTotal) AS promedio,
           MIN(montoTotal) AS minimo,
           MAX(montoTotal) AS maximo
    FROM ALQUILER
    WHERE fecha BETWEEN ? AND ?
      AND (? IS NULL OR idCancha = ?)
    GROUP BY estado
    """;

        //  EnumMap indexa estadisticas por estado para completar filas sin datos.
        java.util.Map<EstadoAlquiler, ResumenAlquiler> porEstado =
                new java.util.EnumMap<>(EstadoAlquiler.class);

        //  Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            //  Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(1, Date.valueOf(desde));
            //  Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(2, Date.valueOf(hasta));
            //  Evita seguir si falta un dato necesario para la operacion.
            if (idCancha == null) {
                //  Guarda SQL NULL cuando el dato opcional no tiene valor.
                consulta.setNull(3, Types.INTEGER);
                //  Guarda SQL NULL cuando el dato opcional no tiene valor.
                consulta.setNull(4, Types.INTEGER);
            } else {
                //  Envia un entero al parametro ? correspondiente del PreparedStatement.
                consulta.setInt(3, idCancha);
                //  Envia un entero al parametro ? correspondiente del PreparedStatement.
                consulta.setInt(4, idCancha);
            }

            //  Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            try (ResultSet resultado = consulta.executeQuery()) {
                //  Lee resultados hasta que ya no queden filas.
                while (resultado.next()) {
                    EstadoAlquiler estado = EstadoAlquiler.desdeBD(resultado.getString("estado"));

                    ResumenAlquiler resumen =
                            new ResumenAlquiler(
                                    estado,
                                    resultado.getLong("cantidad"),
                                    resultado.getBigDecimal("total"),
                                    resultado
                                            .getBigDecimal("promedio")
                                            .setScale(2, RoundingMode.HALF_UP),
                                    resultado.getBigDecimal("minimo"),
                                    resultado.getBigDecimal("maximo"));

                    porEstado.put(estado, resumen);
                }
            }
        }

        //  Crea la lista que se llenara leyendo filas de MySQL.
        List<ResumenAlquiler> resumenes = new ArrayList<>();
        BigDecimal cero = new BigDecimal("0.00");

        //  Recorre los elementos de una lista para agregarlos o compararlos.
        //  Incluye tambien estados sin datos con cantidad cero.
        for (EstadoAlquiler estado : EstadoAlquiler.values()) {
            ResumenAlquiler resumen = porEstado.get(estado);

            //  Evita seguir si falta un dato necesario para la operacion.
            if (resumen == null) {
                resumen = new ResumenAlquiler(estado, 0, cero, cero, cero, cero);
            }

            resumenes.add(resumen);
        }

        //  Devuelve el dato calculado o la respuesta de esta operacion.
        return resumenes;
    }

    // RF10: canchas con mas alquileres confirmados y completados en el periodo.
    /** Cuenta confirmados y completados por cancha y ordena de mayor a menor cantidad. */
    @Override
    // EXPLICACIÓN DEL MÉTODO: Cuenta alquileres confirmados/completados por cancha; objetivo 3.
    //  INDICADOR RF10: cuenta alquileres confirmados/completados por cancha del periodo.
    public List<CanchaResumen> resumirAlquileresPorCancha(LocalDate desde, LocalDate hasta)
            throws SQLException, IOException {

        validarFecha(desde);
        validarFecha(hasta);

        // PASO (resumirAlquileresPorCancha Comprueba una condicion de negocio o controla un caso de error.
        if (desde.isAfter(hasta)) {
            // PASO (resumirAlquileresPorCancha Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        //  INNER JOIN combina ALQUILER con CANCHA para conocer nombre e ID de cada cancha.
        //  COUNT(*) cuenta solo los alquileres confirmados y completados; los cancelados se excluyen.
        //  NO se filtra idCancha: el ganador general compara todas las canchas del periodo.
        //  ORDER BY cantidad DESC garantiza que el primer resumen sea la cancha con mas reservas.
        // PASO (resumirAlquileresPorCancha Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        String sql =
                """
                SELECT c.idCancha,
                       c.nombre,
                       c.deporte,
                       c.tarifaHora,
                       c.estado,
                       COUNT(*) AS cantidad
                FROM ALQUILER a
                INNER JOIN CANCHA c ON c.idCancha = a.idCancha
                WHERE a.fecha BETWEEN ? AND ?
                  AND a.estado IN ('confirmado', 'completado')
                GROUP BY c.idCancha, c.nombre, c.deporte,
                         c.tarifaHora, c.estado
                ORDER BY cantidad DESC, c.idCancha ASC
                """;

        // PASO (resumirAlquileresPorCancha Crea la lista que se llenara leyendo filas de MySQL.
        List<CanchaResumen> resumenes = new ArrayList<>();

        // PASO (resumirAlquileresPorCancha Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            // PASO (resumirAlquileresPorCancha Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(1, Date.valueOf(desde));
            // PASO (resumirAlquileresPorCancha Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(2, Date.valueOf(hasta));

            // PASO (resumirAlquileresPorCancha Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            try (ResultSet resultado = consulta.executeQuery()) {
                // PASO (resumirAlquileresPorCancha Lee resultados hasta que ya no queden filas.
                while (resultado.next()) {
                    Cancha cancha =
                            new Cancha(
                                    resultado.getInt("idCancha"),
                                    resultado.getString("nombre"),
                                    DeporteCancha.desdeBD(resultado.getString("deporte")),
                                    resultado.getBigDecimal("tarifaHora"),
                                    EstadoCancha.desdeBD(resultado.getString("estado")));

                    resumenes.add(new CanchaResumen(cancha, resultado.getLong("cantidad")));
                }
            }
        }

        // PASO (resumirAlquileresPorCancha Devuelve el dato calculado o la respuesta de esta operacion.
        return resumenes;
    }

    // RF10: ingresos acumulados por dia de la semana en el periodo y cancha filtrada.
    // Incluye confirmados y completados, excluye cancelados.
    /** Suma importes por día de la semana de la fecha del servicio; excluye cancelados. */
    @Override
    // EXPLICACIÓN DEL MÉTODO: Agrupa montos por dia de la semana para indicadores; objetivo 3.
    //  INDICADOR RF10: suma importes de todos los lunes, todos los martes, etc.
    public List<IngresoPorDiaSemana> resumirIngresosPorDiaSemana(
            LocalDate desde, LocalDate hasta, Integer idCancha) throws SQLException, IOException {

        validarFecha(desde);
        validarFecha(hasta);

        // PASO (resumirIngresosPorDiaSemana Comprueba una condicion de negocio o controla un caso de error.
        if (desde.isAfter(hasta)) {
            // PASO (resumirIngresosPorDiaSemana Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        // PASO (resumirIngresosPorDiaSemana Rechaza IDs invalidos o cantidades no permitidas.
        if (idCancha != null && idCancha <= 0) {
            // PASO (resumirIngresosPorDiaSemana Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una cancha valida.");
        }

        //  WEEKDAY(fecha)+1 asigna lunes=1 hasta domingo=7, compatible con java.time.DayOfWeek.
        //  SUM(montoTotal) acumula importes de todos los lunes, etc. segun la fecha del SERVICIO.
        //  Respeta el filtro opcional de cancha, descarta cancelados y ordena el total DESC.
        // PASO (resumirIngresosPorDiaSemana Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        String sql =
                """
                SELECT WEEKDAY(fecha) + 1 AS diaSemana,
                       SUM(montoTotal) AS total
                FROM ALQUILER
                WHERE fecha BETWEEN ? AND ?
                  AND estado IN ('confirmado', 'completado')
                  AND (? IS NULL OR idCancha = ?)
                GROUP BY WEEKDAY(fecha) + 1
                ORDER BY total DESC, diaSemana ASC
                """;

        // PASO (resumirIngresosPorDiaSemana Crea la lista que se llenara leyendo filas de MySQL.
        List<IngresoPorDiaSemana> ingresos = new ArrayList<>();

        // PASO (resumirIngresosPorDiaSemana Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            // PASO (resumirIngresosPorDiaSemana Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(1, Date.valueOf(desde));
            // PASO (resumirIngresosPorDiaSemana Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(2, Date.valueOf(hasta));

            // PASO (resumirIngresosPorDiaSemana Evita seguir si falta un dato necesario para la operacion.
            if (idCancha == null) {
                // PASO (resumirIngresosPorDiaSemana Guarda SQL NULL cuando el dato opcional no tiene valor.
                consulta.setNull(3, Types.INTEGER);
                // PASO (resumirIngresosPorDiaSemana Guarda SQL NULL cuando el dato opcional no tiene valor.
                consulta.setNull(4, Types.INTEGER);
            } else {
                // PASO (resumirIngresosPorDiaSemana Envia un entero al parametro ? correspondiente del PreparedStatement.
                consulta.setInt(3, idCancha);
                // PASO (resumirIngresosPorDiaSemana Envia un entero al parametro ? correspondiente del PreparedStatement.
                consulta.setInt(4, idCancha);
            }

            // PASO (resumirIngresosPorDiaSemana Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            try (ResultSet resultado = consulta.executeQuery()) {
                // PASO (resumirIngresosPorDiaSemana Lee resultados hasta que ya no queden filas.
                while (resultado.next()) {
                    ingresos.add(
                            new IngresoPorDiaSemana(
                                    DayOfWeek.of(resultado.getInt("diaSemana")),
                                    resultado.getBigDecimal("total")));
                }
            }
        }

        // PASO (resumirIngresosPorDiaSemana Devuelve el dato calculado o la respuesta de esta operacion.
        return ingresos;
    }

    /** Convierte los tipos SQL y los textos de estado a los tipos del modelo Java. */
    // EXPLICACIÓN DEL MÉTODO: Convierte una fila ResultSet de MySQL en un objeto Alquiler del modelo.
    private Alquiler convertirAlquiler(ResultSet resultado) throws SQLException {

        // PASO (convertirAlquiler Devuelve el dato calculado o la respuesta de esta operacion.
        return new Alquiler(
                resultado.getInt("idAlquiler"),
                resultado.getInt("idCancha"),
                resultado.getInt("idCliente"),
                resultado.getInt("idUsuario"),
                resultado.getDate("fecha").toLocalDate(),
                resultado.getTime("horaInicio").toLocalTime(),
                resultado.getTime("horaFin").toLocalTime(),
                resultado.getBigDecimal("montoTotal"),
                EstadoAlquiler.desdeBD(resultado.getString("estado")));
    }

    // RF10: ocupación por cancha durante un periodo.
    /**
     * Incluye canchas sin reservas mediante LEFT JOIN y calcula la capacidad teórica con todos los
     * días del rango, incluidos los extremos.
     */
    @Override
    // EXPLICACIÓN DEL MÉTODO: Agrega minutos reservados de cada cancha y calcula capacidad teorica; objetivo 3.
    //  OCUPACION RF10: calcula minutos realmente reservados y capacidad TEORICA.
    public List<OcupacionCancha> consultarOcupacion(
            LocalDate desde, LocalDate hasta, Integer idCancha) throws SQLException, IOException {

        validarFecha(desde);
        validarFecha(hasta);

        // PASO (consultarOcupacion Comprueba una condicion de negocio o controla un caso de error.
        if (desde.isAfter(hasta)) {
            // PASO (consultarOcupacion Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        // PASO (consultarOcupacion Rechaza IDs invalidos o cantidades no permitidas.
        if (idCancha != null && idCancha <= 0) {
            // PASO (consultarOcupacion Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Seleccione una cancha válida.");
        }

        // Incluye tanto la fecha inicial como la fecha final.
        //  Incluye fecha de inicio y fecha de fin; por eso se suma uno.
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;

        // Horario de SportCenter: 08:00 a 23:00.
        long minutosDiarios =
                // PASO (consultarOcupacion Calcula la duracion del alquiler entre hora inicial y hora final.
                //  Horario 08:00 a 23:00 = 15 horas = 900 minutos diarios por cancha.
                Duration.between(ReglasAlquiler.HORA_APERTURA, ReglasAlquiler.HORA_CIERRE)
                        .toMinutes();

        // Capacidad teórica de una cancha para todo el periodo.
        //  Capacidad por cancha = dias del periodo x 900 min diarios (15 h).
        long capacidadTeorica = dias * minutosDiarios;

        //  LEFT JOIN empieza por CANCHA para no perder canchas que no tuvieron alquileres.
        //  La condicion de estados validos esta dentro de ON para conservar ceros en el LEFT JOIN.
        //  TIMESTAMPDIFF(MINUTE, inicio, fin) mide duracion reservada, NO asistencia comprobada.
        //  COALESCE(SUM(...),0) registra cero minutos si no existen alquileres validos.
        //  Capacidad teorica = (dias inclusivos) * (23:00 - 08:00) * 60 minutos por hora.
        //  Se incluyen canchas inactivas; NO se descuentan cierres por mantenimiento o inactividad.
        // PASO (consultarOcupacion Prepara la consulta SQL; debajo explicamos su efecto sin cambiar la cadena.
        // SQL EXPLICADO (consultarOcupacion LEFT JOIN conserva canchas sin reservas y suma tiempo reservado para ocupacion teorica.
        String sql =
                """
            SELECT c.idCancha,
                   c.nombre,
                   COALESCE(
                       SUM(
                           TIMESTAMPDIFF(
                               MINUTE,
                               TIMESTAMP(a.fecha, a.horaInicio),
                               TIMESTAMP(a.fecha, a.horaFin)
                           )
                       ),
                       0
                   ) AS minutosReservados
            FROM CANCHA c
            LEFT JOIN ALQUILER a
              ON a.idCancha = c.idCancha
             AND a.fecha BETWEEN ? AND ?
             AND a.estado IN ('confirmado', 'completado')
            WHERE (? IS NULL OR c.idCancha = ?)
            GROUP BY c.idCancha, c.nombre
            ORDER BY c.idCancha
            """;

        // PASO (consultarOcupacion Crea la lista que se llenara leyendo filas de MySQL.
        List<OcupacionCancha> ocupaciones = new ArrayList<>();

        // PASO (consultarOcupacion Abre una conexion JDBC; try-with-resources la cierra automaticamente.
        try (Connection conexion = Conexion.getInstancia().abrirConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {

            // PASO (consultarOcupacion Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(1, Date.valueOf(desde));
            // PASO (consultarOcupacion Convierte LocalDate a DATE de MySQL y lo asigna al parametro indicado.
            consulta.setDate(2, Date.valueOf(hasta));

            // null = todas las canchas.
            // PASO (consultarOcupacion Evita seguir si falta un dato necesario para la operacion.
            if (idCancha == null) {

                // PASO (consultarOcupacion Guarda SQL NULL cuando el dato opcional no tiene valor.
                consulta.setNull(3, Types.INTEGER);
                // PASO (consultarOcupacion Guarda SQL NULL cuando el dato opcional no tiene valor.
                consulta.setNull(4, Types.INTEGER);

            } else {

                // PASO (consultarOcupacion Envia un entero al parametro ? correspondiente del PreparedStatement.
                consulta.setInt(3, idCancha);
                // PASO (consultarOcupacion Envia un entero al parametro ? correspondiente del PreparedStatement.
                consulta.setInt(4, idCancha);
            }

            // PASO (consultarOcupacion Lee filas devueltas por MySQL y cierra el cursor al finalizar.
            try (ResultSet resultado = consulta.executeQuery()) {

                // PASO (consultarOcupacion Lee resultados hasta que ya no queden filas.
                while (resultado.next()) {

                    ocupaciones.add(
                            //  Crea un registro con ID, nombre, minutos reservados y capacidad del periodo.
                            new OcupacionCancha(
                                    resultado.getInt("idCancha"),
                                    resultado.getString("nombre"),
                                    resultado.getLong("minutosReservados"),
                                    capacidadTeorica));
                }
            }
        }

        // PASO (consultarOcupacion Devuelve el dato calculado o la respuesta de esta operacion.
        return ocupaciones;
    }
}