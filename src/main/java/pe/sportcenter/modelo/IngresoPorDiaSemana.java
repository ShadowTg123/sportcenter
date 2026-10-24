package pe.sportcenter.modelo;

import java.math.BigDecimal;
import java.time.DayOfWeek;

// RF10: representa el importe acumulado
// de alquileres para un día de la semana.
public record IngresoPorDiaSemana(
        DayOfWeek dia,
        BigDecimal montoTotal) {
}