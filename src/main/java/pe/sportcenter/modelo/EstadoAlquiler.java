package pe.sportcenter.modelo;

/** Estados del ciclo de vida de un alquiler y su representación en MySQL y en pantalla. */
public enum EstadoAlquiler {
    CONFIRMADO("confirmado"),
    CANCELADO("cancelado"),
    COMPLETADO("completado");

    private final String valorBD;

    EstadoAlquiler(String valorBD) {
        this.valorBD = valorBD;
    }

    /** Devuelve el texto que se utiliza al guardar el valor en MySQL. */
    public String valorBD() {
        return valorBD;
    }

    /** Convierte el texto persistido al valor enumerado y rechaza valores desconocidos. */
    public static EstadoAlquiler desdeBD(String valor) {
        for (EstadoAlquiler estado : values()) {
            if (estado.valorBD.equals(valor)) {
                return estado;
            }
        }

        throw new IllegalArgumentException("Estado de alquiler desconocido: " + valor);
    }

    @Override
    public String toString() {
        return switch (this) {
            case CONFIRMADO -> "Confirmado";
            case CANCELADO -> "Cancelado";
            case COMPLETADO -> "Completado";
        };
    }
}
