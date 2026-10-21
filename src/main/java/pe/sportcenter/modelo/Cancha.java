package pe.sportcenter.modelo;

import java.math.BigDecimal;

public class Cancha {

    private final int idCancha;
    private final String nombre;
    private final DeporteCancha deporte;
    private final BigDecimal tarifaHora;
    private final EstadoCancha estado;

    public Cancha(
            int idCancha,
            String nombre,
            DeporteCancha deporte,
            BigDecimal tarifaHora,
            EstadoCancha estado) {

        this.idCancha = idCancha;
        this.nombre = nombre;
        this.deporte = deporte;
        this.tarifaHora = tarifaHora;
        this.estado = estado;
    }

    public int getIdCancha() {
        return idCancha;
    }

    public String getNombre() {
        return nombre;
    }

    public DeporteCancha getDeporte() {
        return deporte;
    }

    public BigDecimal getTarifaHora() {
        return tarifaHora;
    }

    public EstadoCancha getEstado() {
        return estado;
    }
}