package pe.sportcenter.modelo;

/**
 * Asocia una cancha con un conteo de alquileres. El periodo y los estados incluidos dependen de la
 // [OBJETIVO 3 - EXPLICACION] Contenedor de una cancha con el NUMERO de alquileres confirmados/completados.
 * consulta DAO que construye el resumen.
 */
public final class CanchaResumen {

    // [OBJETIVO 3 - EXPLICACION] Referencia al objeto Cancha para leer su ID y nombre.
    private final Cancha cancha;
    // [OBJETIVO 3 - EXPLICACION] Conteo de alquileres de esa cancha.
    private final long cantidadAlquileres;

    // [OBJETIVO 3 - EXPLICACION] Constructor utilizado al materializar la consulta SQL por cancha.
    public CanchaResumen(Cancha cancha, long cantidadAlquileres) {
        // [OBJETIVO 3 - EXPLICACION] Conserva el objeto con datos descriptivos de cancha.
        this.cancha = cancha;
        // [OBJETIVO 3 - EXPLICACION] Conserva el conteo calculado.
        this.cantidadAlquileres = cantidadAlquileres;
    }

    // [OBJETIVO 3 - EXPLICACION] Devuelve el objeto cancha.
    public Cancha getCancha() {
        return cancha;
    }

    // [OBJETIVO 3 - EXPLICACION] Devuelve la cantidad de reservas validas.
    public long getCantidadAlquileres() {
        return cantidadAlquileres;
    }
}
