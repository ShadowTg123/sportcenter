package pe.sportcenter.modelo;

public enum EstadoAlquiler {

    CONFIRMADO("confirmado"),
    CANCELADO("cancelado"),
    COMPLETADO("completado");

    private final String valorBD;

    EstadoAlquiler(String valorBD) {
        this.valorBD = valorBD;
    }

    public String valorBD() {
        return valorBD;
    }

    public static EstadoAlquiler desdeBD(String valor) {
        for (EstadoAlquiler estado : values()) {
            if (estado.valorBD.equals(valor)) {
                return estado;
            }
        }

        throw new IllegalArgumentException(
                "Estado de alquiler desconocido: " + valor);
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