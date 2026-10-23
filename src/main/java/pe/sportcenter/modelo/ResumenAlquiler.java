package pe.sportcenter.modelo;

import java.math.BigDecimal;

public final class ResumenAlquiler {

    private final EstadoAlquiler estado;
    private final long cantidad;
    private final BigDecimal montoTotal;
    private final BigDecimal montoPromedio;
    private final BigDecimal montoMinimo;
    private final BigDecimal montoMaximo;

    public ResumenAlquiler(
            EstadoAlquiler estado,
            long cantidad,
            BigDecimal montoTotal,
            BigDecimal montoPromedio,
            BigDecimal montoMinimo,
            BigDecimal montoMaximo) {

        this.estado = estado;
        this.cantidad = cantidad;
        this.montoTotal = montoTotal;
        this.montoPromedio = montoPromedio;
        this.montoMinimo = montoMinimo;
        this.montoMaximo = montoMaximo;
    }

    public EstadoAlquiler getEstado() {
        return estado;
    }

    public long getCantidad() {
        return cantidad;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public BigDecimal getMontoPromedio() {
        return montoPromedio;
    }

    public BigDecimal getMontoMinimo() {
        return montoMinimo;
    }

    public BigDecimal getMontoMaximo() {
        return montoMaximo;
    }
}