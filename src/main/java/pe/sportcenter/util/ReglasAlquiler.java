
// TEMA: REGLAS DE NEGOCIO: 08:00-23:00, una hora minima, bloques de 30 minutos


package pe.sportcenter.util;

// IMPORT: Calcula la duración real en minutos.
import java.time.Duration;
// IMPORT: Hora local, útil para inicio y fin del alquiler.
import java.time.LocalTime;

/**
 * Reglas de horario compartidas por el controlador y la persistencia de alquileres. Permite
 * reservas dentro de 08:00 a 23:00, de al menos una hora y en bloques de 30 minutos.
 */
// ESTA CLASE: REGLAS DE NEGOCIO: 08:00-23:00, una hora minima, bloques de 30 minutos.
public final class ReglasAlquiler {

    // CAMPO: Constante publica del horario de atencion.
    public static final LocalTime HORA_APERTURA = LocalTime.of(8, 0);

    // CAMPO: Constante publica del horario de atencion.
    public static final LocalTime HORA_CIERRE = LocalTime.of(23, 0);

    // CAMPO: La duracion minima es de 60 minutos.
    public static final long DURACION_MINIMA_MINUTOS = 60;
    // CAMPO: Las horas se eligen en multiplos de 30 minutos.
    public static final int BLOQUE_MINUTOS = 30;

    // EXPLICACIÓN DEL MÉTODO: Constructor privado: no se crean objetos de esta clase; sus reglas son estaticas.
    private ReglasAlquiler() {
        // Clase de utilidad.
    }

    // RF07: valida el horario permitido para un alquiler.
    // También se utiliza al consultar disponibilidad (RF06).
    // CLAVE OBJETIVO 2: valida existencia, orden, precision, horario 08-23, franjas :00/:30 y duracion >=60 min.
    public static void validarHorario(LocalTime horaInicio, LocalTime horaFin) {

        // PASO (validarHorario): Evita seguir si falta un dato necesario para la operacion.
        if (horaInicio == null || horaFin == null) {
            // PASO (validarHorario): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Las horas de inicio y fin son obligatorias.");
        }

        // PASO (validarHorario): Exige un inicio posterior al momento/limite indicado.
        if (!horaFin.isAfter(horaInicio)) {
            // PASO (validarHorario): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "La hora final debe ser posterior a la inicial " + "dentro del mismo día.");
        }

        // PASO (validarHorario): Comprueba una condicion de negocio o controla un caso de error.
        if (horaInicio.getSecond() != 0
                || horaFin.getSecond() != 0
                || horaInicio.getNano() != 0
                || horaFin.getNano() != 0) {

            // PASO (validarHorario): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Ingrese las horas sin segundos.");
        }

        // PASO (validarHorario): Comprueba una condicion de negocio o controla un caso de error.
        if (horaInicio.isBefore(HORA_APERTURA) || horaFin.isAfter(HORA_CIERRE)) {

            // PASO (validarHorario): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException(
                    "El horario de atención es de 08:00 a 23:00. "
                            + "El alquiler debe estar dentro de ese horario.");
        }

        // PASO (validarHorario): Comprueba que la hora termine en :00 o :30.
        if (horaInicio.getMinute() % BLOQUE_MINUTOS != 0
                || horaFin.getMinute() % BLOQUE_MINUTOS != 0) {

            // PASO (validarHorario): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("Las horas deben terminar en :00 o :30.");
        }

        // PASO (validarHorario): Calcula la duracion del alquiler entre hora inicial y hora final.
        long minutos = Duration.between(horaInicio, horaFin).toMinutes();

        // PASO (validarHorario): Rechaza alquileres menores a una hora.
        if (minutos < DURACION_MINIMA_MINUTOS) {
            // PASO (validarHorario): Detiene la operacion cuando no se cumple una regla; el mensaje ayuda al usuario.
            throw new IllegalArgumentException("La duración mínima del alquiler es de 1 hora.");
        }
    }
}