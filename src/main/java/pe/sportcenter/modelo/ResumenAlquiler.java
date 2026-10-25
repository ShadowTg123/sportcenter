package pe.sportcenter.modelo;

import java.math.BigDecimal;

// [OBJETIVO 3 - EXPLICACION] Contenedor de estadisticas por ESTADO; los valores vienen agrupados desde SQL.
/**
 * Agrupa cantidad, suma, promedio, mínimo y máximo de importes por estado de alquiler. El resumen
 * también puede representar un estado sin registros.
 */
// [OBJETIVO 3 - EXPLICACION] Estado: confirmado, completado o cancelado.
public final class ResumenAlquiler {
// [OBJETIVO 3 - EXPLICACION] Numero de alquileres del estado en el periodo.

    // [OBJETIVO 3 - EXPLICACION] Suma de montos registrados del estado en el periodo.
    private final EstadoAlquiler estado;
    // [OBJETIVO 3 - EXPLICACION] Promedio de montos en ese estado, si hay registros.
    private final long cantidad;
    // [OBJETIVO 3 - EXPLICACION] Menor monto registrado en ese estado.
    private final BigDecimal montoTotal;
    // [OBJETIVO 3 - EXPLICACION] Mayor monto registrado en ese estado.
    private final BigDecimal montoPromedio;
    private final BigDecimal montoMinimo;
    // [OBJETIVO 3 - EXPLICACION] Constructor copia los valores calculados por el DAO, sin recalcular SQL.
    private final BigDecimal montoMaximo;

    public ResumenAlquiler(
            EstadoAlquiler estado,
            long cantidad,
            BigDecimal montoTotal,
            BigDecimal montoPromedio,
            BigDecimal montoMinimo,
            BigDecimal montoMaximo) {

        this.estado = estado;
        // [OBJETIVO 3 - EXPLICACION] Guarda el estado para identificar a que fila pertenece el resumen.
        this.cantidad = cantidad;
        // [OBJETIVO 3 - EXPLICACION] Cantidad de alquileres de ese estado.
        this.montoTotal = montoTotal;
        // [OBJETIVO 3 - EXPLICACION] Importe acumulado de ese estado.
        this.montoPromedio = montoPromedio;
        // [OBJETIVO 3 - EXPLICACION] Promedio del estado tal como se obtuvo del DAO.
        this.montoMinimo = montoMinimo;
        // [OBJETIVO 3 - EXPLICACION] Minimo del estado.
        this.montoMaximo = montoMaximo;
        // [OBJETIVO 3 - EXPLICACION] Maximo del estado.
    }

    public EstadoAlquiler getEstado() {
        // [OBJETIVO 3 - EXPLICACION] Permite identificar el estado en la vista.
        return estado;
    }

    // [OBJETIVO 3 - EXPLICACION] Devuelve la cantidad de reservas de ese estado.
    public long getCantidad() {
        return cantidad;
    }

    // [OBJETIVO 3 - EXPLICACION] Devuelve el total monetario.
    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    // [OBJETIVO 3 - EXPLICACION] Devuelve el promedio.
    public BigDecimal getMontoPromedio() {
        return montoPromedio;
    }

    // [OBJETIVO 3 - EXPLICACION] Devuelve el importe minimo.
    public BigDecimal getMontoMinimo() {
        return montoMinimo;
    }

    // [OBJETIVO 3 - EXPLICACION] Devuelve el importe maximo.
    public BigDecimal getMontoMaximo() {
        return montoMaximo;
    }
}