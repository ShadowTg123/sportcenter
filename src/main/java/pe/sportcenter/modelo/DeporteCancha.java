package pe.sportcenter.modelo;

/** Deportes admitidos y sus nombres persistidos en la base de datos. */
public enum DeporteCancha {
    FUTBOL("Fútbol"),
    VOLEY("Vóley");

    private final String valorBD;

    DeporteCancha(String valorBD) {
        this.valorBD = valorBD;
    }

    /** Devuelve el texto que se utiliza al guardar el valor en MySQL. */
    public String valorBD() {
        return valorBD;
    }

    /** Convierte el texto persistido al valor enumerado y rechaza valores desconocidos. */
    public static DeporteCancha desdeBD(String valor) {
        for (DeporteCancha deporte : values()) {
            if (deporte.valorBD.equals(valor)) {
                return deporte;
            }
        }

        throw new IllegalArgumentException("Deporte no reconocido en la base de datos: " + valor);
    }

    @Override
    public String toString() {
        return valorBD;
    }
}
