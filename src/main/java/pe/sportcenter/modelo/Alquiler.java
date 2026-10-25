
// TEMA: MODELO: un alquiler une cancha, cliente, usuario, horario, monto y estado


package pe.sportcenter.modelo;

// IMPORT: Tipo numérico exacto para el dinero: evita errores por usar double.
import java.math.BigDecimal;
// IMPORT: Fecha sin hora, útil para reservar un día concreto.
import java.time.LocalDate;
// IMPORT: Hora local, útil para inicio y fin del alquiler.
import java.time.LocalTime;

/**
 * Datos de un alquiler: referencias a cancha, cliente y usuario, horario, importe y estado. Los
 * cambios se representan mediante otro objeto con los datos actualizados.
 */
// ESTA CLASE: MODELO: un alquiler une cancha, cliente, usuario, horario, monto y estado.
public final class Alquiler {

    // CAMPO: Campo del modelo; final significa que no se reasigna tras construir el objeto.
    private final int idAlquiler;
    // CAMPO: Campo del modelo; final significa que no se reasigna tras construir el objeto.
    private final int idCancha;
    // CAMPO: Campo del modelo; final significa que no se reasigna tras construir el objeto.
    private final int idCliente;
    // CAMPO: Campo del modelo; final significa que no se reasigna tras construir el objeto.
    private final int idUsuario;
    // CAMPO: Campo del modelo; final significa que no se reasigna tras construir el objeto.
    private final LocalDate fecha;
    // CAMPO: Campo del modelo; final significa que no se reasigna tras construir el objeto.
    private final LocalTime horaInicio;
    // CAMPO: Campo del modelo; final significa que no se reasigna tras construir el objeto.
    private final LocalTime horaFin;
    // CAMPO: Campo del modelo; final significa que no se reasigna tras construir el objeto.
    private final BigDecimal montoTotal;
    // CAMPO: Campo del modelo; final significa que no se reasigna tras construir el objeto.
    private final EstadoAlquiler estado;

    // EXPLICACIÓN DEL MÉTODO: Recibe los datos de la reserva y los conserva en campos final. No guarda por si mismo en
    //     MySQL.
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

    // EXPLICACIÓN DEL MÉTODO: Entrega el ID unico de la reserva.
    public int getIdAlquiler() {
        // PASO (getIdAlquiler): Devuelve el dato calculado o la respuesta de esta operacion.
        return idAlquiler;
    }

    // EXPLICACIÓN DEL MÉTODO: Entrega el ID de la cancha reservada.
    public int getIdCancha() {
        // PASO (getIdCancha): Devuelve el dato calculado o la respuesta de esta operacion.
        return idCancha;
    }

    // EXPLICACIÓN DEL MÉTODO: Entrega el ID del cliente.
    public int getIdCliente() {
        // PASO (getIdCliente): Devuelve el dato calculado o la respuesta de esta operacion.
        return idCliente;
    }

    // EXPLICACIÓN DEL MÉTODO: Entrega el ID del usuario que registro inicialmente.
    public int getIdUsuario() {
        // PASO (getIdUsuario): Devuelve el dato calculado o la respuesta de esta operacion.
        return idUsuario;
    }

    // EXPLICACIÓN DEL MÉTODO: Entrega la fecha de uso de la cancha.
    public LocalDate getFecha() {
        // PASO (getFecha): Devuelve el dato calculado o la respuesta de esta operacion.
        return fecha;
    }

    // EXPLICACIÓN DEL MÉTODO: Entrega la hora inicial.
    public LocalTime getHoraInicio() {
        // PASO (getHoraInicio): Devuelve el dato calculado o la respuesta de esta operacion.
        return horaInicio;
    }

    // EXPLICACIÓN DEL MÉTODO: Entrega la hora final.
    public LocalTime getHoraFin() {
        // PASO (getHoraFin): Devuelve el dato calculado o la respuesta de esta operacion.
        return horaFin;
    }

    // EXPLICACIÓN DEL MÉTODO: Entrega el monto que fue calculado en el DAO.
    public BigDecimal getMontoTotal() {
        // PASO (getMontoTotal): Devuelve el dato calculado o la respuesta de esta operacion.
        return montoTotal;
    }

    // EXPLICACIÓN DEL MÉTODO: Entrega confirmado, cancelado o completado.
    public EstadoAlquiler getEstado() {
        // PASO (getEstado): Devuelve el dato calculado o la respuesta de esta operacion.
        return estado;
    }
}
