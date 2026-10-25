package pe.sportcenter.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 // [OBJETIVO 3  Representa horas reservadas y capacidad TEORICA; no acredita asistencia real.
 * Calcula horas y porcentaje de ocupación a partir de minutos reservados y capacidad teórica. No
 * representa asistencia real ni descuenta cierres o fechas de creación de canchas.
 */
// [OBJETIVO 3  Record inmutable con ID, nombre y duraciones en minutos (sin redondeo previo).
public record OcupacionCancha(
        int idCancha, String nombre, long minutosReservados, long minutosCapacidadTeorica) {

    // [OBJETIVO 3  Constructor compacto que verifica duraciones de entrada.
    public OcupacionCancha {
        // [OBJETIVO 3  Una cantidad negativa de minutos reservados no tiene sentido.
        if (minutosReservados < 0) {
            throw new IllegalArgumentException("Los minutos reservados no pueden ser negativos.");
        }

        // [OBJETIVO 3  Se necesita capacidad positiva para calcular un porcentaje.
        if (minutosCapacidadTeorica <= 0) {
            throw new IllegalArgumentException("La capacidad teórica debe ser mayor que cero.");
        }
    }

    /** Convierte minutos reservados a horas con dos decimales. */
    // [OBJETIVO 3  Horas reservadas = minutos de alquileres no cancelados / 60.
    public BigDecimal horasReservadas() {
        // [OBJETIVO 3  BigDecimal para presentar el resultado con dos decimales.
        return BigDecimal.valueOf(minutosReservados)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** Convierte la capacidad en minutos a horas con dos decimales. */
    // [OBJETIVO 3  Horas disponibles teoricas = dias inclusivos * 15 horas.
    public BigDecimal horasCapacidadTeorica() {
        return BigDecimal.valueOf(minutosCapacidadTeorica)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

//Aquí tenemos el método porcentajeOcupacion, que devuelve un valor de tipo BigDecimal.
//Este método calcula el porcentaje de ocupación de una cancha. Para ello, tomamos los minutos reservados, los
//  multiplicamos por cien y dividimos el resultado entre los minutos de capacidad teórica.
//Utilizamos BigDecimal para trabajar con precisión y RoundingMode.HALF_UP para redondear el resultado a dos decimales.
//
//De esta manera podemos conocer qué porcentaje del tiempo disponible estuvo reservado.
    public BigDecimal porcentajeOcupacion() {
        //  Primero multiplica minutos por 100 y solo luego redondea, para mayor precision.
        // Calcula con minutos para evitar redondeos intermedios.
        return BigDecimal.valueOf(minutosReservados)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(minutosCapacidadTeorica), 2, RoundingMode.HALF_UP);
    }
}


//ejm  una cancha tiene 3 horas reservadas y su capacidad teórica diaria es de 15 horas.
//- Minutos reservados: 180.
//- Minutos de capacidad teórica: 900.




//En nuestro sistema consideramos una capacidad teórica de 15 horas diarias por cancha, porque el horario de
// atención es de 8:00 a. m. a 11:00 p. m. Multiplicamos esas 15 horas por 60 para obtener 900 minutos diarios y luego por la
//  cantidad de días del periodo consultado.