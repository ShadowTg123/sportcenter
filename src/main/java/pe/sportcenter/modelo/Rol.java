package pe.sportcenter.modelo;

// Define los únicos perfiles permitidos en el sistema.
public enum Rol {
    ADMINISTRADOR,
    RECEPCIONISTA;

    // Convierte el rol al formato utilizado en MySQL.
    public String valorBD() {
        return switch (this) {
            case ADMINISTRADOR -> "administrador";
            case RECEPCIONISTA -> "recepcionista";
        };
    }

    // Convierte el texto de MySQL a un rol de Java.
    public static Rol desdeBD(String valor) {
        if ("administrador".equals(valor)) {
            return ADMINISTRADOR;
        }

        if ("recepcionista".equals(valor)) {
            return RECEPCIONISTA;
        }

        throw new IllegalArgumentException("Rol no reconocido.");
    }
}