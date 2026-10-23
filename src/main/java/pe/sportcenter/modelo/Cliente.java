package pe.sportcenter.modelo;

public final class Cliente {

    private final int idCliente;
    private final String nombre;
    private final String dni;
    private final String telefono;

    public Cliente(
            int idCliente,
            String nombre,
            String dni,
            String telefono) {

        this.idCliente = idCliente;
        this.nombre = nombre;
        this.dni = dni;
        this.telefono = telefono;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDni() {
        return dni;
    }

    public String getTelefono() {
        return telefono;
    }
}