package pe.sportcenter.modelo;

import java.math.BigDecimal;
import java.time.DayOfWeek;

// [OBJETIVO 3 - EXPLICACION] Importe TOTAL acumulado por dia de semana, no ingreso promedio de una fecha aislada.
// RF10: representa el importe acumulado
// de alquileres para un día de la semana.
/**
 * Importe acumulado de alquileres para un día de la semana dentro del periodo consultado.
 * Representa una suma, no el promedio de ingresos por fecha.
 */
// [OBJETIVO 3 - EXPLICACION] Record: dia es DayOfWeek (lunes..domingo) y montoTotal es BigDecimal.
public record IngresoPorDiaSemana(DayOfWeek dia, BigDecimal montoTotal) {}
