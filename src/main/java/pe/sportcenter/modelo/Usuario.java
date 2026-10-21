package pe.sportcenter.modelo;

public class Usuario {

    private final int idUsuario;
    private final String nombre;
    private final String usuario;
    private final Rol rol;

    public Usuario(int idUsuario, String nombre,
                   String usuario, Rol rol) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.usuario = usuario;
        this.rol = rol;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public Rol getRol() {
        return rol;
    }
}