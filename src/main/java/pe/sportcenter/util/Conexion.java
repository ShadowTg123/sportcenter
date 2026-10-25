package pe.sportcenter.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Administra la configuración JDBC mediante una instancia Singleton. Cada llamada a abrirConexion
 * crea una conexión independiente que el DAO debe cerrar. El archivo db.properties se busca en el
 * directorio desde el que se ejecuta el programa.
 */
public final class Conexion {

    // Singleton: una sola instancia administra la configuración.
    private static Conexion instancia;

    private final Properties propiedades = new Properties();

    private Conexion() throws IOException {
        Path archivo = Path.of("db.properties");

        // Lee el archivo externo usando UTF-8.
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        }

        // Comprueba que existan los datos necesarios.
        for (String campo : new String[] {"db.url", "db.usuario", "db.clave"}) {
            if (!propiedades.containsKey(campo)) {
                throw new IOException("Falta la propiedad " + campo + " en db.properties");
            }
        }
    }

    /** Crea la configuración una sola vez. synchronized evita inicializaciones simultáneas. */
    public static synchronized Conexion getInstancia() throws IOException {
        if (instancia == null) {
            instancia = new Conexion();
        }
        return instancia;
    }

    // Cada operación obtiene una conexión que debe cerrar al terminar.
    /** Abre una conexión nueva; quien la solicita es responsable de cerrarla. */
    public Connection abrirConexion() throws SQLException {
        return DriverManager.getConnection(
                propiedades.getProperty("db.url"),
                propiedades.getProperty("db.usuario"),
                propiedades.getProperty("db.clave"));
    }
}