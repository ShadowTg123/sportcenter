package pe.sportcenter.modelo;

public enum EstadoCancha {
    DISPONIBLE("disponible"),
    ALQUILADA("alquilada"),
    MANTENIMIENTO("mantenimiento"),
    INACTIVA("inactiva");

    private final String valorBD;

    EstadoCancha(String valorBD) {
        this.valorBD = valorBD;
    }

    public String valorBD() {
        return valorBD;
    }

    public static EstadoCancha desdeBD(String valor) {
        for (EstadoCancha estado : values()) {
            if (estado.valorBD.equals(valor)) {
                return estado;
            }
        }

        throw new IllegalArgumentException(
                "Estado no reconocido en la base de datos: " + valor);
    }
}