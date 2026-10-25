package pe.sportcenter.modelo;

/** Estados operativos de una cancha. INACTIVA representa la eliminación lógica. */
public enum EstadoCancha {
    DISPONIBLE("disponible"),
    ALQUILADA("alquilada"),
    MANTENIMIENTO("mantenimiento"),
    INACTIVA("inactiva");

    private final String valorBD;

    EstadoCancha(String valorBD) {
        this.valorBD = valorBD;
    }

    /** Devuelve el texto que se utiliza al guardar el valor en MySQL. */
    public String valorBD() {
        return valorBD;
    }

    /** Convierte el texto persistido al valor enumerado y rechaza valores desconocidos. */
    public static EstadoCancha desdeBD(String valor) {
        for (EstadoCancha estado : values()) {
            if (estado.valorBD.equals(valor)) {
                return estado;
            }
        }

        throw new IllegalArgumentException("Estado no reconocido en la base de datos: " + valor);
    }
}
