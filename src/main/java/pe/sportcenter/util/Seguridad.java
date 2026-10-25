package pe.sportcenter.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Genera y verifica hashes PBKDF2 con sal aleatoria. El texto guardado contiene algoritmo,
 * iteraciones, sal y hash; no la contraseña original.
 */
//función de derivación de claves estandarizada en RFC 8018 que transforma contraseñas en claves criptográficas seguras mediante iteraciones y sales aleatorias
public final class Seguridad {

    private static final int ITERACIONES = 600_000;
    private static final int LONGITUD_HASH = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Seguridad() {
        // Clase de utilidad: no necesita crear objetos.
    }

    /** Genera una sal nueva para que la misma contraseña produzca hashes distintos. */
    public static String generarHash(char[] clave) throws GeneralSecurityException {

        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);

        byte[] hash = calcularHash(clave, sal, ITERACIONES);

        // Guarda algoritmo, iteraciones, sal y hash en un solo texto.
        return "pbkdf2_sha256:"
                + ITERACIONES
                + ":"
                + Base64.getEncoder().encodeToString(sal)
                + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Reconstruye el hash con la sal guardada y compara los bytes; rechaza formatos inválidos. */
    public static boolean verificarClave(char[] clave, String almacenado)
            throws GeneralSecurityException {

        if (clave == null || almacenado == null) {
            return false;
        }

        try {
            String[] partes = almacenado.split(":", -1);

            if (partes.length != 4 || !"pbkdf2_sha256".equals(partes[0])) {
                return false;
            }

            int iteraciones = Integer.parseInt(partes[1]);

            // Evita procesar valores inválidos o excesivos.
            if (iteraciones < 600_000 || iteraciones > 2_000_000) {
                return false;
            }

            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] hashGuardado = Base64.getDecoder().decode(partes[3]);

            if (sal.length != 16 || hashGuardado.length != 32) {
                return false;
            }

            byte[] hashCalculado = calcularHash(clave, sal, iteraciones);

            return MessageDigest.isEqual(hashGuardado, hashCalculado);

        } catch (IllegalArgumentException e) {
            // El texto almacenado no tiene un formato válido.
            return false;
        }
    }

    private static byte[] calcularHash(char[] clave, byte[] sal, int iteraciones)
            throws GeneralSecurityException {

        PBEKeySpec especificacion = new PBEKeySpec(clave, sal, iteraciones, LONGITUD_HASH);

        try {
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            return fabrica.generateSecret(especificacion).getEncoded();

        } finally {
            especificacion.clearPassword();
        }
    }
}