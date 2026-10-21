package pe.sportcenter.modelo;

public enum DeporteCancha {
    FUTBOL("Fútbol"),
    VOLEY("Vóley");

    private final String valorBD;

    DeporteCancha(String valorBD) {
        this.valorBD = valorBD;
    }

    public String valorBD() {
        return valorBD;
    }

    public static DeporteCancha desdeBD(String valor) {
        for (DeporteCancha deporte : values()) {
            if (deporte.valorBD.equals(valor)) {
                return deporte;
            }
        }

        throw new IllegalArgumentException(
                "Deporte no reconocido en la base de datos: " + valor);
    }

    @Override
    public String toString() {
        return valorBD;
    }
}
