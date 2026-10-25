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

// [OBJETIVO 3Utilidad que transforma los datos YA CONSULTADOS en un archivo Excel verdadero, no una tabla CSV.
/**
 * Construye las hojas Resumen y Ocupacion con Apache POI. Recibe una consulta ya realizada y
 * devuelve los bytes del archivo XLSX; la vista se encarga de elegir el destino y escribirlo.
 */
public final class ExportadorReporteExcel {

    // [OBJETIVO 3Formato de fechas que aparece en la portada de ambas hojas Excel.
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    // [OBJETIVO 3Constructor privado: no se crean instancias; sus metodos son estaticos.
    private ExportadorReporteExcel() {
        // Clase de utilidad.
    }

    //Aquí tenemos el método público y estático generar, que devuelve un arreglo de bytes, es decir, el contenido del archivo Excel.
    // Recibe un parámetro de tipo ReporteExportacion, que contiene la información del reporte que vamos a exportar.
    public static byte[] generar(ReporteExportacion reporte) throws IOException {

        //Primero validamos que exista un reporte para exportar. Si todavía no se ha consultado ninguno, el sistema no permite continuar.
        Objects.requireNonNull(reporte, "Primero debe consultar un reporte.");

        // Después creamos un libro de Excel utilizando XSSFWorkbook, que pertenece a Apache POI.
        // También utilizamos ByteArrayOutputStream para almacenar el archivo generado temporalmente en memoria.
        try (Workbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            //Aquí configuramos el estilo de los encabezados, colocando los títulos en negrita y agregando un color de
            // fondo para que el reporte sea más fácil de leer.
            CellStyle encabezado = libro.createCellStyle();
            // [OBJETIVO 3Fuente en negrita para distinguir los titulos de columnas.
            Font fuente = libro.createFont();
            fuente.setBold(true);
            encabezado.setFont(fuente);
            // [OBJETIVO 3Color de relleno para encabezados; es solo presentacion.
            encabezado.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            //Después configuramos los formatos numéricos para mostrar correctamente los importes, las horas y los porcentajes de ocupación.
            CellStyle decimal = libro.createCellStyle();
            decimal.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            // [OBJETIVO 3Excel almacena porcentajes entre 0 y 1 y muestra 0.00%.
            CellStyle porcentaje = libro.createCellStyle();
            porcentaje.setDataFormat(libro.createDataFormat().getFormat("0.00%"));

            //Esta es la parte principal del método. Aquí llamamos a crearResumen() y crearOcupacion(),
            // que se encargan de generar las dos hojas de nuestro archivo Excel, utilizando la información que obtuvimos del sistema.
            crearResumen(libro, reporte, encabezado, decimal, porcentaje);

            // [OBJETIVO 3Segunda hoja: porcentaje y horas de ocupacion por cancha.
            crearOcupacion(libro, reporte, encabezado, decimal, porcentaje);

            //Finalmente, utilizamos libro.write() para generar el contenido del archivo Excel y lo devolvemos como un arreglo de bytes.
            // Después, desde la vista de reportes, ese contenido se guarda en la ubicación seleccionada por el usuario.
            libro.write(salida);
            // [OBJETIVO 3Devuelve el byte[]; el guardado fisico corresponde a ReporteAlquilerVista.
            return salida.toByteArray();
        }
    }

    /** Escribe importes por estado e indicadores; los ingresos excluyen alquileres cancelados. */
    // [OBJETIVO 3METODO CLAVE: genera la pestana Resumen e ingresos en formato XLSX.
    private static void crearResumen(
            Workbook libro,
            ReporteExportacion reporte,
            CellStyle encabezado,
            CellStyle decimal,
            CellStyle porcentaje) {

        // [OBJETIVO 3Crea la hoja de Excel llamada Resumen.
        Sheet hoja = libro.createSheet("Resumen");
        // [OBJETIVO 3Escribe periodo y filtro usado para que la hoja sea interpretable sin la aplicacion.
        escribirDatosGenerales(hoja, reporte);

        // [OBJETIVO 3Aclara que los importes son de reservas, no cobros verificados.
        filaTexto(hoja, 4, "Importes de alquileres; no son cobros verificados.");

        // [OBJETIVO 3Define las seis columnas de los resultados agrupados por estado.
        Row titulos =
                filaTexto(
                        hoja,
                        6,
                        "Estado",
                        "Cantidad",
                        "Total S/",
                        "Promedio S/",
                        "Mínimo S/",
                        "Máximo S/");
        // [OBJETIVO 3Resalta las cabeceras de la tabla.
        aplicarEstilo(titulos, encabezado);

        // [OBJETIVO 3Primera fila util para datos despues de los encabezados.
        int fila = 7;
        // [OBJETIVO 3Cuenta total de alquileres, incluyendo cancelados.
        long cantidadTotal = 0;
        // [OBJETIVO 3Numero de confirmados y completados, denominador del ticket promedio.
        long cantidadConIngreso = 0;
        // [OBJETIVO 3Numero de alquileres cancelados.
        long cantidadCancelados = 0;
        // [OBJETIVO 3Suma monetaria de importes con BigDecimal.
        BigDecimal ingresos = new BigDecimal("0.00");

        // [OBJETIVO 3Recorre resumenes por estado que provienen de MySQL.
        for (ResumenAlquiler resumen : reporte.resumenes()) {
            // [OBJETIVO 3Crea una fila para el estado actual.
            Row registro = hoja.createRow(fila++);

            // [OBJETIVO 3Coloca el texto del estado en la primera columna.
            texto(registro, 0, resumen.getEstado().toString());
            // [OBJETIVO 3Escribe la cantidad como valor NUMERICO, no texto.
            registro.createCell(1).setCellValue(resumen.getCantidad());
            // [OBJETIVO 3Escribe monto total como numero decimal en Excel.
            numero(registro, 2, resumen.getMontoTotal(), decimal);

            // [OBJETIVO 3Si existen alquileres muestra promedio, minimo y maximo reales.
            if (resumen.getCantidad() > 0) {
                numero(registro, 3, resumen.getMontoPromedio(), decimal);
                numero(registro, 4, resumen.getMontoMinimo(), decimal);
                numero(registro, 5, resumen.getMontoMaximo(), decimal);
            } else {
                // [OBJETIVO 3Sin registros, deja valores como raya para no inventar un promedio.
                texto(registro, 3, "—");
                texto(registro, 4, "—");
                texto(registro, 5, "—");
            }

            // [OBJETIVO 3Incluye el estado en el denominador del indicador cancelaciones.
            cantidadTotal += resumen.getCantidad();

            // [OBJETIVO 3Excluye cancelados del calculo de ingresos.
            if (resumen.getEstado() == EstadoAlquiler.CONFIRMADO
                    || resumen.getEstado() == EstadoAlquiler.COMPLETADO) {

                // [OBJETIVO 3Suma importes solamente confirmados/completados.
                ingresos = ingresos.add(resumen.getMontoTotal());
                cantidadConIngreso += resumen.getCantidad();

                // [OBJETIVO 3Cuenta cancelados para calcular el porcentaje.
            } else if (resumen.getEstado() == EstadoAlquiler.CANCELADO) {
                cantidadCancelados += resumen.getCantidad();
            }
        }

        fila++;

        // [OBJETIVO 3Inicia la seccion de indicadores del reporte.
        Row tituloIndicadores = filaTexto(hoja, fila++, "Indicador", "Valor");
        aplicarEstilo(tituloIndicadores, encabezado);

        // [OBJETIVO 3Muestra la suma de importes no cancelados.
        Row total = filaTexto(hoja, fila++, "Ingresos (confirmados + completados)");
        numero(total, 1, ingresos, decimal);

        // [OBJETIVO 3Agrega el indicador promedio por alquiler.
        Row promedio = filaTexto(hoja, fila++, "Importe promedio por alquiler");

        // [OBJETIVO 3Evita dividir si no existen confirmados/completados.
        if (cantidadConIngreso > 0) {
            // [OBJETIVO 3Promedio = ingresos / cantidad de reservas no canceladas.
            BigDecimal valor =
                    ingresos.divide(
                            BigDecimal.valueOf(cantidadConIngreso), 2, RoundingMode.HALF_UP);
            numero(promedio, 1, valor, decimal);
        } else {
            texto(promedio, 1, "—");
        }

        // [OBJETIVO 3Prepara la fila de porcentaje de cancelaciones.
        Row cancelaciones = filaTexto(hoja, fila++, "Porcentaje de cancelaciones");

        // [OBJETIVO 3No divide cuando la lista de alquileres esta vacia.
        if (cantidadTotal > 0) {
            // Primero redondeamos igual que en pantalla.
            // [OBJETIVO 3Tasa de cancelaciones = 100 * cancelados / todos.
            BigDecimal valor =
                    BigDecimal.valueOf(cantidadCancelados)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(cantidadTotal), 2, RoundingMode.HALF_UP);

            // [OBJETIVO 3Para Excel 25% debe guardarse como 0.25; se mueve la coma dos lugares.
            // Excel representa 25 % mediante el número 0.25.
            numero(cancelaciones, 1, valor.movePointLeft(2), porcentaje);
        } else {
            texto(cancelaciones, 1, "—");
        }

        fila++;

        // [OBJETIVO 3La cancha mas alquilada se calculo entre TODAS las canchas del periodo.
        Row tituloCanchas =
                filaTexto(
                        hoja,
                        fila++,
                        "Canchas más alquiladas: todas las canchas del periodo",
                        "Cantidad");
        aplicarEstilo(tituloCanchas, encabezado);

        // [OBJETIVO 3Si no hay reservas validas se registra un mensaje, no una cancha inventada.
        if (reporte.canchasMasAlquiladas().isEmpty()) {
            filaTexto(hoja, fila++, "Sin confirmados ni completados", "—");
        } else {
            // [OBJETIVO 3Muestra TODAS las canchas empatadas en el maximo del periodo.
            for (CanchaResumen resumen : reporte.canchasMasAlquiladas()) {
                Row registro =
                        filaTexto(
                                hoja,
                                fila++,
                                resumen.getCancha().getNombre()
                                        + " [ID "
                                        + resumen.getCancha().getIdCancha()
                                        + "]");

                registro.createCell(1).setCellValue(resumen.getCantidadAlquileres());
            }
        }

        fila++;

        // [OBJETIVO 3Encabezado del indicador de dia de la semana con mayor importe acumulado.
        Row tituloDias =
                filaTexto(hoja, fila++, "Días de la semana con mayores ingresos", "Total S/");
        aplicarEstilo(tituloDias, encabezado);

        // [OBJETIVO 3Aclara que esta seccion SI respeta el filtro opcional de cancha.
        filaTexto(hoja, fila++, "Respeta el filtro de cancha; suma por fecha del servicio.");

        if (reporte.diasConMayoresIngresos().isEmpty()) {
            filaTexto(hoja, fila, "Sin confirmados ni completados", "—");
        } else {
            // [OBJETIVO 3Escribe todos los dias empatados con el mayor acumulado.
            for (IngresoPorDiaSemana ingreso : reporte.diasConMayoresIngresos()) {

                // [OBJETIVO 3Da formato legible en español al enum DayOfWeek.
                String dia =
                        ingreso.dia()
                                .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"));

                Row registro = filaTexto(hoja, fila++, dia);
                // [OBJETIVO 3Importe total del dia de la semana como numero decimal.
                numero(registro, 1, ingreso.montoTotal(), decimal);
            }
        }

        // [OBJETIVO 3Ajusta el ancho de las columnas para evitar textos recortados.
        hoja.setColumnWidth(0, 65 * 256);

        for (int columna = 1; columna < 6; columna++) {
            hoja.setColumnWidth(columna, 20 * 256);
        }

        // [OBJETIVO 3Deja fijos los encabezados al desplazarse por la hoja.
        hoja.createFreezePane(0, 7);
    }

    /** Escribe horas reservadas, capacidad teórica y porcentajes de ocupación por cancha. */
    // [OBJETIVO 3METODO CLAVE: escribe la hoja de ocupacion por cancha.
    private static void crearOcupacion(
            Workbook libro,
            ReporteExportacion reporte,
            CellStyle encabezado,
            CellStyle decimal,
            CellStyle porcentaje) {

        // [OBJETIVO 3Crea una segunda hoja llamada Ocupacion.
        Sheet hoja = libro.createSheet("Ocupacion");
        // [OBJETIVO 3Incluye de nuevo el periodo y filtro de consulta.
        escribirDatosGenerales(hoja, reporte);

        // [OBJETIVO 3Solo se contabilizan minutos de alquileres confirmados y completados.
        filaTexto(hoja, 4, "Horas reservadas: alquileres confirmados y completados.");
        // [OBJETIVO 3La capacidad es fija: 15 horas teoricas diarias para cada cancha.
        filaTexto(hoja, 5, "Capacidad teórica: 15 horas diarias por cancha.");
        // [OBJETIVO 3No mide asistencia; tampoco resta cierres por mantenimiento.
        filaTexto(hoja, 6, "No descuenta cierres ni fechas de creación; no mide asistencia.");

        // [OBJETIVO 3Define columnas para cancha, horas y porcentaje.
        Row titulos =
                filaTexto(
                        hoja,
                        8,
                        "ID",
                        "Cancha",
                        "Horas reservadas",
                        "Capacidad teórica (h)",
                        "Ocupación");
        aplicarEstilo(titulos, encabezado);

        // [OBJETIVO 3Empieza la tabla de ocupacion tras las notas.
        int fila = 9;

        // [OBJETIVO 3Recorre las canchas y sus ocupaciones entregadas por la consulta.
        for (OcupacionCancha ocupacion : reporte.ocupaciones()) {
            Row registro = hoja.createRow(fila++);

            // [OBJETIVO 3Guarda ID como numero para poder ordenar o filtrar en Excel.
            registro.createCell(0).setCellValue(ocupacion.idCancha());
            // [OBJETIVO 3La segunda columna contiene el nombre visible de la cancha.
            texto(registro, 1, ocupacion.nombre());
            // [OBJETIVO 3Convierte minutos a horas antes de escribirlas.
            numero(registro, 2, ocupacion.horasReservadas(), decimal);
            // [OBJETIVO 3Escribe el denominador del porcentaje en horas de capacidad teorica.
            numero(registro, 3, ocupacion.horasCapacidadTeorica(), decimal);

            // [OBJETIVO 3La fraccion 0..1 se escribe con estilo de porcentaje de Excel.
            numero(registro, 4, ocupacion.porcentajeOcupacion().movePointLeft(2), porcentaje);
        }

        // [OBJETIVO 3No inventa canchas cuando la lista viene vacia.
        if (reporte.ocupaciones().isEmpty()) {
            filaTexto(hoja, fila, "No hay canchas para este filtro.");
        }

        // [OBJETIVO 3Dimensiona las columnas del reporte de ocupacion.
        hoja.setColumnWidth(0, 12 * 256);
        hoja.setColumnWidth(1, 35 * 256);
        hoja.setColumnWidth(2, 22 * 256);
        hoja.setColumnWidth(3, 25 * 256);
        hoja.setColumnWidth(4, 20 * 256);

        // [OBJETIVO 3Fija los encabezados de la tabla al desplazarse.
        hoja.createFreezePane(0, 9);
    }

    /** Coloca el periodo y el filtro que acompañan a los resultados exportados. */
    // [OBJETIVO 3Metodo auxiliar: titulo del reporte, rango de fechas y filtro de cancha.
    private static void escribirDatosGenerales(Sheet hoja, ReporteExportacion reporte) {

        // [OBJETIVO 3Identifica el libro como reporte de SportCenter.
        filaTexto(hoja, 0, "SportCenter - Reporte de alquileres");
        // [OBJETIVO 3Escribe el periodo en el formato dd/MM/aaaa.
        filaTexto(
                hoja,
                1,
                "Periodo: "
                        + reporte.desde().format(FECHA)
                        + " al "
                        + reporte.hasta().format(FECHA));
        // [OBJETIVO 3Indica la cancha (o todas) a la que pertenecen los datos exportados.
        filaTexto(hoja, 2, "Cancha: " + reporte.nombreFiltro());
    }

    /** Crea una fila de celdas de texto y devuelve la fila para aplicar estilos. */
    // [OBJETIVO 3Metodo auxiliar: crea una fila y varias celdas de texto.
    private static Row filaTexto(Sheet hoja, int numeroFila, String... valores) {

        // [OBJETIVO 3Crea la fila solicitada.
        Row fila = hoja.createRow(numeroFila);

        // [OBJETIVO 3Recorre los textos recibidos y los ubica en columnas consecutivas.
        for (int columna = 0; columna < valores.length; columna++) {
            texto(fila, columna, valores[columna]);
        }

        // [OBJETIVO 3Devuelve la fila para poder aplicarle un estilo.
        return fila;
    }

    /** Escribe contenido como texto para que Excel no lo interprete como una fórmula. */
    // [OBJETIVO 3Crea una celda de texto incluso si comienza por un signo igual.
    private static void texto(Row fila, int columna, String valor) {
        // Escribe texto, incluso si comienza con "=".
        // [OBJETIVO 3setCellValue(String) hace que Excel lo trate como texto, no formula.
        fila.createCell(columna).setCellValue(valor);
    }

    /** Escribe una celda numérica y aplica el formato de importe o porcentaje. */
    // [OBJETIVO 3Metodo auxiliar: escribe un numero y aplica estilo decimal o porcentual.
    private static void numero(Row fila, int columna, BigDecimal valor, CellStyle estilo) {

        // [OBJETIVO 3Crea la celda correspondiente.
        Cell celda = fila.createCell(columna);
        // [OBJETIVO 3Apache POI recibe un double de Excel a partir de BigDecimal.
        celda.setCellValue(valor.doubleValue());
        // [OBJETIVO 3Aplica formato numerico sin convertir el valor en texto.
        celda.setCellStyle(estilo);
    }

    /** Aplica el estilo compartido a las celdas existentes de la fila. */
    // [OBJETIVO 3Metodo auxiliar: recorre y da formato a las celdas de una cabecera.
    private static void aplicarEstilo(Row fila, CellStyle estilo) {
        // [OBJETIVO 3Aplica el estilo a cada celda de esa fila.
        for (Cell celda : fila) {
            celda.setCellStyle(estilo);
        }
    }
}
