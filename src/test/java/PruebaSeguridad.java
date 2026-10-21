import pe.sportcenter.util.Seguridad;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class PruebaSeguridad {

    public static void main(String[] args)
            throws GeneralSecurityException {

        // Son contraseñas ficticias para esta prueba.
        char[] correcta = "Prueba12345".toCharArray();
        char[] incorrecta = "OtraClave99".toCharArray();

        try {
            String hash1 = Seguridad.generarHash(correcta);
            String hash2 = Seguridad.generarHash(correcta);

            System.out.println("Clave correcta: "
                    + Seguridad.verificarClave(correcta, hash1));

            System.out.println("Clave incorrecta: "
                    + Seguridad.verificarClave(incorrecta, hash1));

            System.out.println("Hashes diferentes: "
                    + !hash1.equals(hash2));

            System.out.println("Formato inválido: "
                    + Seguridad.verificarClave(correcta, "texto-invalido"));

        } finally {
            Arrays.fill(correcta, '\0');
            Arrays.fill(incorrecta, '\0');
        }
    }
}