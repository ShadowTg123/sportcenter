package pe.sportcenter.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public final class Alquiler {

    private final int idAlquiler;
    private final int idCancha;
    private final int idCliente;
    private final int idUsuario;
    private final LocalDate fecha;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;
    private final BigDecimal montoTotal;
    private final EstadoAlquiler estado;

    public Alquiler(
            int idAlquiler,
            int idCancha,
            int idCliente,
            int idUsuario,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            BigDecimal montoTotal,
            EstadoAlquiler estado) {

        this.idAlquiler = idAlquiler;
        this.idCancha = idCancha;
        this.idCliente = idCliente;
        this.idUsuario = idUsuario;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.montoTotal = montoTotal;
        this.estado = estado;
    }

    public int getIdAlquiler() {
        return idAlquiler;
    }

    public int getIdCancha() {
        return idCancha;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public EstadoAlquiler getEstado() {
        return estado;
    }
}