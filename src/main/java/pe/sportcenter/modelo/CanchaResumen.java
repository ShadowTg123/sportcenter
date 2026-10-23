package pe.sportcenter.modelo;

public final class CanchaResumen {

    private final Cancha cancha;
    private final long cantidadAlquileres;

    public CanchaResumen(Cancha cancha, long cantidadAlquileres) {
        this.cancha = cancha;
        this.cantidadAlquileres = cantidadAlquileres;
    }

    public Cancha getCancha() {
        return cancha;
    }

    public long getCantidadAlquileres() {
        return cantidadAlquileres;
    }
}