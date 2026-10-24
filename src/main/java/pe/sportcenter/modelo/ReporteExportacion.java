package pe.sportcenter.modelo;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

// Conserva los filtros y resultados de una consulta exitosa.
public record ReporteExportacion(
        LocalDate desde,
        LocalDate hasta,
        String nombreFiltro,
        List<ResumenAlquiler> resumenes,
        List<CanchaResumen> canchasMasAlquiladas,
        List<IngresoPorDiaSemana> diasConMayoresIngresos,
        List<OcupacionCancha> ocupaciones) {

    public ReporteExportacion {
        Objects.requireNonNull(desde, "La fecha inicial es obligatoria.");
        Objects.requireNonNull(hasta, "La fecha final es obligatoria.");
        Objects.requireNonNull(nombreFiltro, "El filtro es obligatorio.");

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        // Copias que no permiten agregar ni eliminar elementos.
        resumenes = List.copyOf(resumenes);
        canchasMasAlquiladas = List.copyOf(canchasMasAlquiladas);
        diasConMayoresIngresos = List.copyOf(diasConMayoresIngresos);
        ocupaciones = List.copyOf(ocupaciones);
    }
}