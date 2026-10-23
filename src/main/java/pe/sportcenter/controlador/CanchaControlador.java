package pe.sportcenter.controlador;

import pe.sportcenter.dao.CanchaDAO;
import pe.sportcenter.modelo.Cancha;
import pe.sportcenter.modelo.DeporteCancha;
import pe.sportcenter.modelo.EstadoCancha;
import pe.sportcenter.modelo.Rol;
import pe.sportcenter.modelo.Usuario;
import pe.sportcenter.modelo.CanchaResumen;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class CanchaControlador {

    private final CanchaDAO canchaDAO;
    private final AutenticacionControlador autenticacion;

    public CanchaControlador(
            CanchaDAO canchaDAO,
            AutenticacionControlador autenticacion) {
        this.canchaDAO = Objects.requireNonNull(canchaDAO);
        this.autenticacion = Objects.requireNonNull(autenticacion);
    }

    public List<Cancha> listarCanchas()
            throws SQLException, IOException {

        Usuario usuario = exigirSesion();

        List<Cancha> canchas = canchaDAO.listarTodas();

        // El recepcionista no consulta las canchas inactivas.
        if (usuario.getRol() == Rol.RECEPCIONISTA) {
            return canchas.stream()
                    .filter(c -> c.getEstado() != EstadoCancha.INACTIVA)
                    .toList();
        }

        return canchas;
    }

    public List<CanchaResumen> listarResumenAlquileres()
            throws SQLException, IOException {

        Usuario usuario = exigirSesion();

        List<CanchaResumen> resumenes =
                canchaDAO.listarConCantidadAlquileres();

        if (usuario.getRol() == Rol.RECEPCIONISTA) {
            return resumenes.stream()
                    .filter(r -> r.getCancha().getEstado()
                            != EstadoCancha.INACTIVA)
                    .toList();
        }

        return resumenes;
    }


    public void registrar(
            String nombre,
            DeporteCancha deporte,
            BigDecimal tarifaHora)
            throws SQLException, IOException {

        exigirAdministrador();

        String nombreValidado = validarNombre(nombre);
        DeporteCancha deporteValidado = validarDeporte(deporte);
        BigDecimal tarifaValidada = validarTarifa(tarifaHora);

        Cancha nueva = new Cancha(
                0,
                nombreValidado,
                deporteValidado,
                tarifaValidada,
                EstadoCancha.DISPONIBLE
        );

        canchaDAO.registrar(nueva);
    }

    public void actualizar(
            int idCancha,
            String nombre,
            DeporteCancha deporte,
            BigDecimal tarifaHora,
            EstadoCancha estado)
            throws SQLException, IOException {

        exigirAdministrador();

        if (idCancha <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione una cancha válida.");
        }

        String nombreValidado = validarNombre(nombre);
        DeporteCancha deporteValidado = validarDeporte(deporte);
        BigDecimal tarifaValidada = validarTarifa(tarifaHora);

        if (estado == null || estado == EstadoCancha.INACTIVA) {
            throw new IllegalArgumentException(
                    "Seleccione un estado válido. "
                            + "Para inactivar una cancha use la opción Eliminar.");
        }

        Cancha actualizada = new Cancha(
                idCancha,
                nombreValidado,
                deporteValidado,
                tarifaValidada,
                estado
        );

        canchaDAO.actualizar(actualizada);
    }

    public boolean eliminarLogicamente(int idCancha)
            throws SQLException, IOException {

        exigirAdministrador();
        validarId(idCancha);
        return canchaDAO.eliminarLogicamente(idCancha);
    }

    public boolean eliminarFisicamente(int idCancha)
            throws SQLException, IOException {

        exigirAdministrador();
        validarId(idCancha);
        return canchaDAO.eliminarFisicamente(idCancha);
    }

    private Usuario exigirSesion() {
        Usuario usuario = autenticacion.getUsuarioActual();

        if (usuario == null) {
            throw new SecurityException(
                    "Debe iniciar sesión para realizar esta operación.");
        }

        return usuario;
    }

    private void exigirAdministrador() {
        Usuario usuario = exigirSesion();

        if (usuario.getRol() != Rol.ADMINISTRADOR) {
            throw new SecurityException(
                    "Solo el administrador puede modificar las canchas.");
        }
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la cancha es obligatorio.");
        }

        String limpio = nombre.strip();

        if (limpio.length() > 50) {
            throw new IllegalArgumentException(
                    "El nombre admite como máximo 50 caracteres.");
        }

        if (limpio.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "El nombre contiene caracteres no permitidos.");
        }

        return limpio;
    }

    private DeporteCancha validarDeporte(DeporteCancha deporte) {
        if (deporte == null) {
            throw new IllegalArgumentException(
                    "Seleccione Fútbol o Vóley.");
        }

        return deporte;
    }

    private BigDecimal validarTarifa(BigDecimal tarifaHora) {
        if (tarifaHora == null) {
            throw new IllegalArgumentException(
                    "La tarifa por hora es obligatoria.");
        }

        BigDecimal tarifa;

        try {
            tarifa = tarifaHora.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "La tarifa solo puede tener dos decimales.");
        }

        if (tarifa.compareTo(new BigDecimal("10.00")) < 0
                || tarifa.compareTo(new BigDecimal("500.00")) > 0) {
            throw new IllegalArgumentException(
                    "La tarifa debe estar entre S/ 10.00 y S/ 500.00.");
        }

        return tarifa;
    }

    private void validarId(int idCancha) {
        if (idCancha <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione una cancha válida.");
        }
    }
}