package pe.sportcenter.util;

public final class Validaciones {

    private Validaciones() {
        // Clase de utilidad.
    }

    public static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio.");
        }

        String limpio = nombre.strip();

        if (limpio.length() > 80) {
            throw new IllegalArgumentException(
                    "El nombre debe tener como máximo 80 caracteres.");
        }

        if (limpio.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "El nombre contiene caracteres no permitidos.");
        }

        return limpio;
    }

    public static String validarUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException(
                    "El usuario es obligatorio.");
        }

        String limpio = usuario.strip();

        if (!limpio.matches("[a-zA-Z0-9._-]{4,30}")) {
            throw new IllegalArgumentException(
                    "El usuario debe tener entre 4 y 30 caracteres. "
                            + "Solo se permiten letras sin tilde, números, "
                            + "punto, guion y guion bajo.");
        }

        return limpio;
    }

    public static void validarClave(char[] clave) {
        if (clave == null || clave.length < 8 || clave.length > 128) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre 8 y 128 caracteres.");
        }

        boolean tieneLetra = false;
        boolean tieneNumero = false;

        for (char caracter : clave) {
            if (Character.isISOControl(caracter)) {
                throw new IllegalArgumentException(
                        "La contraseña contiene caracteres no permitidos.");
            }

            if (Character.isLetter(caracter)) {
                tieneLetra = true;
            }

            if (Character.isDigit(caracter)) {
                tieneNumero = true;
            }
        }

        if (!tieneLetra || !tieneNumero) {
            throw new IllegalArgumentException(
                    "La contraseña debe incluir letras y números.");
        }
    }
}