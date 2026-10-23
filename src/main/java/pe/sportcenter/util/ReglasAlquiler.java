package pe.sportcenter.util;

import java.time.Duration;
import java.time.LocalTime;

public final class ReglasAlquiler {

    public static final LocalTime HORA_APERTURA =
            LocalTime.of(8, 0);

    public static final LocalTime HORA_CIERRE =
            LocalTime.of(23, 0);

    public static final long DURACION_MINIMA_MINUTOS = 60;
    public static final int BLOQUE_MINUTOS = 30;

    private ReglasAlquiler() {
        // Clase de utilidad.
    }

    // RF07: valida el horario permitido para un alquiler.
    // Se reutilizará en la consulta de disponibilidad (RF06).
    public static void validarHorario(
            LocalTime horaInicio,
            LocalTime horaFin) {

        if (horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException(
                    "Las horas de inicio y fin son obligatorias.");
        }

        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                    "La hora final debe ser posterior a la inicial "
                            + "dentro del mismo día.");
        }

        if (horaInicio.getSecond() != 0
                || horaFin.getSecond() != 0
                || horaInicio.getNano() != 0
                || horaFin.getNano() != 0) {

            throw new IllegalArgumentException(
                    "Ingrese las horas sin segundos.");
        }

        if (horaInicio.isBefore(HORA_APERTURA)
                || horaFin.isAfter(HORA_CIERRE)) {

            throw new IllegalArgumentException(
                    "El horario de atención es de 08:00 a 23:00. "
                            + "El alquiler debe estar dentro de ese horario.");
        }

        if (horaInicio.getMinute() % BLOQUE_MINUTOS != 0
                || horaFin.getMinute() % BLOQUE_MINUTOS != 0) {

            throw new IllegalArgumentException(
                    "Las horas deben terminar en :00 o :30.");
        }

        long minutos =
                Duration.between(horaInicio, horaFin).toMinutes();

        if (minutos < DURACION_MINIMA_MINUTOS) {
            throw new IllegalArgumentException(
                    "La duración mínima del alquiler es de 1 hora.");
        }
    }
}