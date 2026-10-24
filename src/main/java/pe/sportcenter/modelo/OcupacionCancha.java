package pe.sportcenter.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record OcupacionCancha(
        int idCancha,
        String nombre,
        long minutosReservados,
        long minutosCapacidadTeorica) {

    public OcupacionCancha {
        if (minutosReservados < 0) {
            throw new IllegalArgumentException(
                    "Los minutos reservados no pueden ser negativos.");
        }

        if (minutosCapacidadTeorica <= 0) {
            throw new IllegalArgumentException(
                    "La capacidad teórica debe ser mayor que cero.");
        }
    }

    public BigDecimal horasReservadas() {
        return BigDecimal.valueOf(minutosReservados)
                .divide(
                        BigDecimal.valueOf(60),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    public BigDecimal horasCapacidadTeorica() {
        return BigDecimal.valueOf(minutosCapacidadTeorica)
                .divide(
                        BigDecimal.valueOf(60),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    public BigDecimal porcentajeOcupacion() {
        // Calcula con minutos para evitar redondeos intermedios.
        return BigDecimal.valueOf(minutosReservados)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(minutosCapacidadTeorica),
                        2,
                        RoundingMode.HALF_UP
                );
    }
}