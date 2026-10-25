package pe.sportcenter.modelo;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

// Conserva los filtros y resultados de una consulta exitosa.
/**
 // [OBJETIVO 3  Este record conserva una foto de los filtros y resultados del ultimo reporte consultado.
 * Conserva los filtros y resultados de la última consulta exitosa para exportarlos. Copia las
 * listas para impedir que se agreguen o eliminen elementos después de consultar.
 */
public record ReporteExportacion(
        LocalDate desde,
        // [OBJETIVO 3  Java genera automaticamente accesores desde(), hasta(), nombreFiltro(), etc.
        LocalDate hasta,
        String nombreFiltro,
        List<ResumenAlquiler> resumenes,
        List<CanchaResumen> canchasMasAlquiladas,
        List<IngresoPorDiaSemana> diasConMayoresIngresos,
        List<OcupacionCancha> ocupaciones) {

    public ReporteExportacion {
        // [OBJETIVO 3  Constructor compacto: obliga a fechas/filtro no nulos.
        Objects.requireNonNull(desde, "La fecha inicial es obligatoria.");
        Objects.requireNonNull(hasta, "La fecha final es obligatoria.");
        Objects.requireNonNull(nombreFiltro, "El filtro es obligatorio.");

        if (desde.isAfter(hasta)) {
            // [OBJETIVO 3  Valida la coherencia del rango de fechas que se va a exportar.
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final.");
        }

        // Copias que no permiten agregar ni eliminar elementos.
        // [OBJETIVO 3  List.copyOf crea listas inmodificables para no cambiar el reporte tras consultar.
        resumenes = List.copyOf(resumenes);
        // [OBJETIVO 3  Guarda ganadoras de maxima cantidad, incluidas empatadas.
        canchasMasAlquiladas = List.copyOf(canchasMasAlquiladas);
        // [OBJETIVO 3  Guarda dias de semana con mayor acumulado, incluidos empates.
        diasConMayoresIngresos = List.copyOf(diasConMayoresIngresos);
        // [OBJETIVO 3  Guarda minutos y capacidad teorica para la segunda hoja.
        ocupaciones = List.copyOf(ocupaciones);
    }
}
