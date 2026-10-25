package pe.sportcenter.modelo;

// Define los únicos perfiles permitidos en el sistema.
/** Perfiles autorizados y conversión entre sus valores Java y los textos almacenados en MySQL. */
public enum Rol {
    ADMINISTRADOR,
    RECEPCIONISTA;

    // Convierte el rol al formato utilizado en MySQL.
    /** Devuelve el texto que se utiliza al guardar el valor en MySQL. */
    public String valorBD() {
        return switch (this) {
            case ADMINISTRADOR -> "administrador";
            case RECEPCIONISTA -> "recepcionista";
        };
    }

    // Convierte el texto de MySQL a un rol de Java.
    /** Convierte el texto persistido al valor enumerado y rechaza valores desconocidos. */
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