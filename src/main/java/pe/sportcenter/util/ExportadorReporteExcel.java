package pe.sportcenter.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import pe.sportcenter.modelo.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

public final class ExportadorReporteExcel {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private ExportadorReporteExcel() {
        // Clase de utilidad.
    }

    // Genera el contenido del Excel.
    // La pantalla elegirá después dónde guardarlo.
    public static byte[] generar(ReporteExportacion reporte)
            throws IOException {

        Objects.requireNonNull(
                reporte, "Primero debe consultar un reporte.");

        try (Workbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida =
                     new ByteArrayOutputStream()) {

            CellStyle encabezado = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            encabezado.setFont(fuente);
            encabezado.setFillForegroundColor(
                    IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle decimal = libro.createCellStyle();
            decimal.setDataFormat(
                    libro.createDataFormat().getFormat("#,##0.00"));

            CellStyle porcentaje = libro.createCellStyle();
            porcentaje.setDataFormat(
                    libro.createDataFormat().getFormat("0.00%"));

            crearResumen(
                    libro, reporte, encabezado, decimal, porcentaje);

            crearOcupacion(
                    libro, reporte, encabezado, decimal, porcentaje);

            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private static void crearResumen(
            Workbook libro,
            ReporteExportacion reporte,
            CellStyle encabezado,
            CellStyle decimal,
            CellStyle porcentaje) {

        Sheet hoja = libro.createSheet("Resumen");
        escribirDatosGenerales(hoja, reporte);

        filaTexto(hoja, 4,
                "Importes de alquileres; no son cobros verificados.");

        Row titulos = filaTexto(
                hoja, 6,
                "Estado", "Cantidad", "Total S/",
                "Promedio S/", "Mínimo S/", "Máximo S/");
        aplicarEstilo(titulos, encabezado);

        int fila = 7;
        long cantidadTotal = 0;
        long cantidadConIngreso = 0;
        long cantidadCancelados = 0;
        BigDecimal ingresos = new BigDecimal("0.00");

        for (ResumenAlquiler resumen : reporte.resumenes()) {
            Row registro = hoja.createRow(fila++);

            texto(registro, 0, resumen.getEstado().toString());
            registro.createCell(1).setCellValue(resumen.getCantidad());
            numero(registro, 2, resumen.getMontoTotal(), decimal);

            if (resumen.getCantidad() > 0) {
                numero(registro, 3, resumen.getMontoPromedio(), decimal);
                numero(registro, 4, resumen.getMontoMinimo(), decimal);
                numero(registro, 5, resumen.getMontoMaximo(), decimal);
            } else {
                texto(registro, 3, "—");
                texto(registro, 4, "—");
                texto(registro, 5, "—");
            }

            cantidadTotal += resumen.getCantidad();

            if (resumen.getEstado() == EstadoAlquiler.CONFIRMADO
                    || resumen.getEstado() == EstadoAlquiler.COMPLETADO) {

                ingresos = ingresos.add(resumen.getMontoTotal());
                cantidadConIngreso += resumen.getCantidad();

            } else if (resumen.getEstado() == EstadoAlquiler.CANCELADO) {
                cantidadCancelados += resumen.getCantidad();
            }
        }

        fila++;

        Row tituloIndicadores = filaTexto(
                hoja, fila++, "Indicador", "Valor");
        aplicarEstilo(tituloIndicadores, encabezado);

        Row total = filaTexto(
                hoja, fila++, "Ingresos (confirmados + completados)");
        numero(total, 1, ingresos, decimal);

        Row promedio = filaTexto(
                hoja, fila++, "Importe promedio por alquiler");

        if (cantidadConIngreso > 0) {
            BigDecimal valor = ingresos.divide(
                    BigDecimal.valueOf(cantidadConIngreso),
                    2, RoundingMode.HALF_UP);
            numero(promedio, 1, valor, decimal);
        } else {
            texto(promedio, 1, "—");
        }

        Row cancelaciones = filaTexto(
                hoja, fila++, "Porcentaje de cancelaciones");

        if (cantidadTotal > 0) {
            // Primero redondeamos igual que en pantalla.
            BigDecimal valor = BigDecimal.valueOf(cantidadCancelados)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            BigDecimal.valueOf(cantidadTotal),
                            2, RoundingMode.HALF_UP);

            // Excel representa 25 % mediante el número 0.25.
            numero(cancelaciones, 1, valor.movePointLeft(2), porcentaje);
        } else {
            texto(cancelaciones, 1, "—");
        }

        fila++;

        Row tituloCanchas = filaTexto(
                hoja, fila++,
                "Canchas más alquiladas: todas las canchas del periodo",
                "Cantidad");
        aplicarEstilo(tituloCanchas, encabezado);

        if (reporte.canchasMasAlquiladas().isEmpty()) {
            filaTexto(hoja, fila++, "Sin confirmados ni completados", "—");
        } else {
            for (CanchaResumen resumen : reporte.canchasMasAlquiladas()) {
                Row registro = filaTexto(
                        hoja, fila++,
                        resumen.getCancha().getNombre()
                                + " [ID "
                                + resumen.getCancha().getIdCancha()
                                + "]");

                registro.createCell(1).setCellValue(
                        resumen.getCantidadAlquileres());
            }
        }

        fila++;

        Row tituloDias = filaTexto(
                hoja, fila++,
                "Días de la semana con mayores ingresos",
                "Total S/");
        aplicarEstilo(tituloDias, encabezado);

        filaTexto(hoja, fila++,
                "Respeta el filtro de cancha; suma por fecha del servicio.");

        if (reporte.diasConMayoresIngresos().isEmpty()) {
            filaTexto(hoja, fila, "Sin confirmados ni completados", "—");
        } else {
            for (IngresoPorDiaSemana ingreso
                    : reporte.diasConMayoresIngresos()) {

                String dia = ingreso.dia().getDisplayName(
                        TextStyle.FULL, Locale.forLanguageTag("es-PE"));

                Row registro = filaTexto(hoja, fila++, dia);
                numero(registro, 1, ingreso.montoTotal(), decimal);
            }
        }

        hoja.setColumnWidth(0, 65 * 256);

        for (int columna = 1; columna < 6; columna++) {
            hoja.setColumnWidth(columna, 20 * 256);
        }

        hoja.createFreezePane(0, 7);
    }

    private static void crearOcupacion(
            Workbook libro,
            ReporteExportacion reporte,
            CellStyle encabezado,
            CellStyle decimal,
            CellStyle porcentaje) {

        Sheet hoja = libro.createSheet("Ocupacion");
        escribirDatosGenerales(hoja, reporte);

        filaTexto(hoja, 4,
                "Horas reservadas: alquileres confirmados y completados.");
        filaTexto(hoja, 5,
                "Capacidad teórica: 15 horas diarias por cancha.");
        filaTexto(hoja, 6,
                "No descuenta cierres ni fechas de creación; no mide asistencia.");

        Row titulos = filaTexto(
                hoja, 8,
                "ID", "Cancha", "Horas reservadas",
                "Capacidad teórica (h)", "Ocupación");
        aplicarEstilo(titulos, encabezado);

        int fila = 9;

        for (OcupacionCancha ocupacion : reporte.ocupaciones()) {
            Row registro = hoja.createRow(fila++);

            registro.createCell(0).setCellValue(ocupacion.idCancha());
            texto(registro, 1, ocupacion.nombre());
            numero(registro, 2, ocupacion.horasReservadas(), decimal);
            numero(registro, 3, ocupacion.horasCapacidadTeorica(), decimal);

            numero(
                    registro, 4,
                    ocupacion.porcentajeOcupacion().movePointLeft(2),
                    porcentaje);
        }

        if (reporte.ocupaciones().isEmpty()) {
            filaTexto(hoja, fila, "No hay canchas para este filtro.");
        }

        hoja.setColumnWidth(0, 12 * 256);
        hoja.setColumnWidth(1, 35 * 256);
        hoja.setColumnWidth(2, 22 * 256);
        hoja.setColumnWidth(3, 25 * 256);
        hoja.setColumnWidth(4, 20 * 256);

        hoja.createFreezePane(0, 9);
    }

    private static void escribirDatosGenerales(
            Sheet hoja,
            ReporteExportacion reporte) {

        filaTexto(hoja, 0, "SportCenter - Reporte de alquileres");
        filaTexto(hoja, 1,
                "Periodo: " + reporte.desde().format(FECHA)
                        + " al " + reporte.hasta().format(FECHA));
        filaTexto(hoja, 2, "Cancha: " + reporte.nombreFiltro());
    }

    private static Row filaTexto(
            Sheet hoja,
            int numeroFila,
            String... valores) {

        Row fila = hoja.createRow(numeroFila);

        for (int columna = 0; columna < valores.length; columna++) {
            texto(fila, columna, valores[columna]);
        }

        return fila;
    }

    private static void texto(Row fila, int columna, String valor) {
        // Escribe texto, incluso si comienza con "=".
        fila.createCell(columna).setCellValue(valor);
    }

    private static void numero(
            Row fila,
            int columna,
            BigDecimal valor,
            CellStyle estilo) {

        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor.doubleValue());
        celda.setCellStyle(estilo);
    }

    private static void aplicarEstilo(Row fila, CellStyle estilo) {
        for (Cell celda : fila) {
            celda.setCellStyle(estilo);
        }
    }
}