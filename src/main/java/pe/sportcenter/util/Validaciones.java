// : Paquete de esta clase: coincide con su ubicación en src/main/java y permite que otras clases la importen.
package pe.sportcenter.util;

/**
 * Reglas compartidas para nombres, identificadores de usuario y contraseñas. Devuelve texto
 * normalizado o lanza IllegalArgumentException ante entradas inválidas.
 */
// : Declara la clase y su responsabilidad dentro del módulo SportCenter.
//Este método recorre la contraseña y va marcando si encuentra una minúscula,
// una mayúscula, un número y un símbolo. Antes comprueba que tenga entre 8 y 128 caracteres.
// Si falta cualquiera de los requisitos, genera un mensaje de error y no permite registrar al usuario
public final class Validaciones {

    // : Constructor privado: impide crear objetos de una clase que solo contiene funciones estáticas.
    private Validaciones() {
        // Clase de utilidad.
    }

    /**
     * Comprueba obligatoriedad, longitud y caracteres de control; elimina espacios de los extremos.
     */
    // : Verifica nombre obligatorio, sin espacios extremos, máximo 80 caracteres, sin caracteres de control.
    public static String validarNombre(String nombre) {
        // : Comprueba que el dato requerido no sea nulo antes de continuar.
        if (nombre == null || nombre.isBlank()) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }

        // : Elimina espacios al principio y al final antes de guardar/mostrar el nombre.
        String limpio = nombre.strip();

        // : Condición: si se cumple, aplica la validación o decisión descrita en el bloque siguiente.
        if (limpio.length() > 80) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("El nombre debe tener como máximo 80 caracteres.");
        }

        // : Condición: si se cumple, aplica la validación o decisión descrita en el bloque siguiente.
        if (limpio.codePoints().anyMatch(Character::isISOControl)) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("El nombre contiene caracteres no permitidos.");
        }

        // : Devuelve el dato normalizado para usarlo en el controlador y el DAO.
        return limpio;
    }

    /** Admite entre 4 y 30 caracteres: letras sin tilde, números, punto, guion y guion bajo. */
    // : Verifica nombre de cuenta: de 4 a 30 caracteres ASCII permitidos mediante expresión regular.
    public static String validarUsuario(String usuario) {
        // : Comprueba que el dato requerido no sea nulo antes de continuar.
        if (usuario == null || usuario.isBlank()) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }

        // : Elimina espacios extremos del nombre de usuario antes de validarlo.
        String limpio = usuario.strip();

        // : Expresión regular: 4 a 30 caracteres; letras ASCII, números, punto, guion y guion bajo.
        if (!limpio.matches("[a-zA-Z0-9._-]{4,30}")) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException(
                    "El usuario debe tener entre 4 y 30 caracteres. "
                            + "Solo se permiten letras sin tilde, números, "
                            + "punto, guion y guion bajo.");
        }

        // : Devuelve el dato normalizado para usarlo en el controlador y el DAO.
        return limpio;
    }

    /** Exige longitud válida y presencia de minúscula, mayúscula, número y símbolo. */
    // CLAVE PARA SUSTENTAR: validarClave()
    // : LAS 5 REGLAS DEL PROFESOR: longitud de 8 a 128 + al menos minúscula + mayúscula + dígito + símbolo permitido.
    public static void validarClave(char[] clave) {
        // : Regla 1: obliga entre 8 y 128 caracteres. Null representa ausencia de contraseña.
        if (clave == null || clave.length < 8 || clave.length > 128) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre 8 y 128 caracteres.");
        }

        // : Marca si se encontró al menos una letra minúscula.
        boolean tieneMinuscula = false;
        // : Marca si se encontró al menos una letra mayúscula.
        boolean tieneMayuscula = false;
        // : Marca si se encontró al menos un dígito.
        boolean tieneNumero = false;
        // : Marca si se encontró al menos uno de los símbolos admitidos.
        boolean tieneSimbolo = false;

        // Símbolos que se consideran válidos para cumplir el requisito.
        // : Guarda texto validado u obtenido de la consulta para las comprobaciones siguientes.
        String simbolos = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

        // : Recorre cada carácter introducido para comprobar los cuatro tipos exigidos.
        for (char caracter : clave) {
            // : Descarta caracteres de control (por ejemplo saltos de línea) por seguridad y consistencia.
            if (Character.isISOControl(caracter)) {
                // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
                throw new IllegalArgumentException(
                        "La contraseña contiene caracteres no permitidos.");
            }

            // : Regla 2: reconoce letras minúsculas.
            if (Character.isLowerCase(caracter)) {
                // : Ya se cumplió el requisito de al menos una minúscula.
                tieneMinuscula = true;
            }

            // : Regla 3: reconoce letras mayúsculas.
            if (Character.isUpperCase(caracter)) {
                // : Ya se cumplió el requisito de al menos una mayúscula.
                tieneMayuscula = true;
            }

            // : Regla 4: reconoce dígitos numéricos.
            if (Character.isDigit(caracter)) {
                // : Ya se cumplió el requisito de al menos un número.
                tieneNumero = true;
            }

            // : Regla 5: busca el carácter en el conjunto de símbolos definidos.
            if (simbolos.indexOf(caracter) >= 0) {
                // : Ya se encontró un símbolo de la lista admitida.
                tieneSimbolo = true;
            }
        }

        // : Si FALTA cualquiera de las cuatro clases de caracteres, rechaza la contraseña completa.
        if (!tieneMinuscula || !tieneMayuscula || !tieneNumero || !tieneSimbolo) {
            // : Interrumpe el procedimiento con un mensaje de error específico para entrada/estado inválido.
            throw new IllegalArgumentException(
                    "La contraseña debe incluir al menos una letra minúscula, "
                            + "una letra mayúscula, un número y un símbolo "
                            + "especial (por ejemplo: !, @, # o $).");
        }
    }
}