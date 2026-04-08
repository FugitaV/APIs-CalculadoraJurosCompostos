package com.example.calculadora.application.service;

import com.example.calculadora.adapter.in.ExportResponse;
import com.example.calculadora.application.port.ExportSimulationUseCase;
import com.example.calculadora.application.port.GetSimulationPort;
import com.example.calculadora.domain.model.IntervalResult;
import com.example.calculadora.domain.model.Simulation;
import com.example.calculadora.domain.model.SimulationInterval;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;

/**
 * Gera uma planilha .xlsx com três abas:
 *  1. Resumo      — parâmetros + totais
 *  2. Intervalos  — aportes informados pelo usuário
 *  3. Evolução    — resultado calculado por intervalo
 */
@Service
@RequiredArgsConstructor
public class ExcelExportService implements ExportSimulationUseCase {

    private final GetSimulationPort getSimulationPort;

    // cores do tema (RGB)
    private static final byte[] C_ORANGE = {(byte) 249, (byte) 115, (byte)  22};
    private static final byte[] C_LIGHT  = {(byte) 253, (byte) 186, (byte) 116};
    private static final byte[] C_DARK   = {(byte)  28, (byte)  28, (byte)  46};

    @Override
    public ExportResponse export(String simulationId) {
        Simulation sim = getSimulationPort.getById(simulationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Simulação não encontrada: " + simulationId));

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            buildSummarySheet(wb, sim);
            buildIntervalsSheet(wb, sim);
            buildEvolutionSheet(wb, sim);

            wb.write(out);

            String b64      = Base64.getEncoder().encodeToString(out.toByteArray());
            String fileName = "simulacao-" + simulationId.substring(0, 8) + ".xlsx";

            return ExportResponse.builder()
                    .fileName(fileName)
                    .fileContent(b64)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar planilha Excel", e);
        }
    }

    // ── Sheet 1: Resumo ───────────────────────────────────────────────────────

    private void buildSummarySheet(XSSFWorkbook wb, Simulation sim) {
        XSSFSheet sheet = wb.createSheet("Resumo");
        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 38 * 256);

        Styles s = new Styles(wb);
        int r = 0;

        // Título
        Row title = sheet.createRow(r++);
        title.setHeightInPoints(26);
        Cell tc = title.createCell(0);
        tc.setCellValue("Simulação de Juros Compostos");
        tc.setCellStyle(s.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        r++; // espaço

        // Seção: Parâmetros
        sectionHeader(sheet, r++, "Parâmetros da Simulação", s.sectionHeader);
        labelText(sheet, r++, "ID da Simulação",   sim.getSimulationId(),                   s);
        labelText(sheet, r++, "Data",               formatDate(sim.getCreatedAt()),           s);
        labelText(sheet, r++, "Usuário",             sim.getUserId(),                          s);
        labelCurrency(sheet, r++, "Valor Inicial",   sim.getInitialValue(),                    s);
        labelPct(sheet,     r++, "Taxa Anual",        sim.getAnnualRate(),                      s);
        labelText(sheet, r++, "Nº de Intervalos",
                String.valueOf(sim.getIntervals() != null ? sim.getIntervals().size() : 0),  s);

        r++; // espaço

        // Seção: Resultado
        sectionHeader(sheet, r++, "Resultado", s.sectionHeader);
        labelCurrency(sheet, r++, "Total Investido", sim.getTotalInvested(),  s);
        labelCurrency(sheet, r++, "Total em Juros",  sim.getTotalInterest(),  s);
        labelCurrency(sheet, r++, "Valor Final",      sim.getFinalValue(),     s);

        if (sim.getTotalInvested() != null
                && sim.getTotalInvested().compareTo(BigDecimal.ZERO) > 0
                && sim.getTotalInterest() != null) {
            BigDecimal rendimento = sim.getTotalInterest()
                    .divide(sim.getTotalInvested(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            labelPct(sheet, r, "Rendimento Total", rendimento, s);
        }
    }

    // ── Sheet 2: Intervalos de Entrada ───────────────────────────────────────

    private void buildIntervalsSheet(XSSFWorkbook wb, Simulation sim) {
        XSSFSheet sheet = wb.createSheet("Intervalos");
        sheet.setColumnWidth(0,  8 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 24 * 256);

        Styles s = new Styles(wb);

        Row header = sheet.createRow(0);
        header.setHeightInPoints(18);
        headerCell(header, 0, "#",               s.colHeader);
        headerCell(header, 1, "Aporte Mensal",   s.colHeader);
        headerCell(header, 2, "Período (meses)", s.colHeader);
        headerCell(header, 3, "Aporte Extra",    s.colHeader);

        List<SimulationInterval> intervals = sim.getIntervals();
        if (intervals == null) return;

        for (int i = 0; i < intervals.size(); i++) {
            SimulationInterval iv = intervals.get(i);
            Row row = sheet.createRow(i + 1);

            numCell(row, 0, i + 1,                           s.cell);
            currencyCell(row, 1, iv.getMonthlyContribution(), s.currency);
            numCell(row, 2, iv.getPeriodInMonths(),           s.cell);

            if (iv.getExtraContribution() != null
                    && iv.getExtraContribution().compareTo(BigDecimal.ZERO) > 0) {
                currencyCell(row, 3, iv.getExtraContribution(), s.currency);
            } else {
                textCell(row, 3, "—", s.cell);
            }
        }
    }

    // ── Sheet 3: Evolução por Intervalo ──────────────────────────────────────

    private void buildEvolutionSheet(XSSFWorkbook wb, Simulation sim) {
        XSSFSheet sheet = wb.createSheet("Evolução");
        sheet.setColumnWidth(0,  8 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        sheet.setColumnWidth(3, 24 * 256);

        Styles s = new Styles(wb);

        Row header = sheet.createRow(0);
        header.setHeightInPoints(18);
        headerCell(header, 0, "#",              s.colHeader);
        headerCell(header, 1, "Total Aportado", s.colHeader);
        headerCell(header, 2, "Juros Gerados",  s.colHeader);
        headerCell(header, 3, "Saldo Final",    s.colHeader);

        List<IntervalResult> results = sim.getIntervalResults();
        if (results == null) return;

        BigDecimal sumContrib = BigDecimal.ZERO;
        BigDecimal sumJuros   = BigDecimal.ZERO;

        for (int i = 0; i < results.size(); i++) {
            IntervalResult res = results.get(i);
            Row row = sheet.createRow(i + 1);

            numCell(row, 0, i + 1,                          s.cell);
            currencyCell(row, 1, res.getTotalContributed(), s.currency);
            currencyCell(row, 2, res.getInterestEarned(),   s.currency);
            currencyCell(row, 3, res.getFinalBalance(),      s.currency);

            if (res.getTotalContributed() != null) sumContrib = sumContrib.add(res.getTotalContributed());
            if (res.getInterestEarned()   != null) sumJuros   = sumJuros.add(res.getInterestEarned());
        }

        // Linha de totais
        Row totals = sheet.createRow(results.size() + 1);
        textCell(totals, 0, "Total", s.totalLabel);
        currencyCell(totals, 1, sumContrib,           s.totalValue);
        currencyCell(totals, 2, sumJuros,             s.totalValue);
        currencyCell(totals, 3, sim.getFinalValue(),  s.totalValue);
    }

    // ── Helpers de linha ─────────────────────────────────────────────────────

    private void sectionHeader(XSSFSheet sheet, int rowIdx, String text, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        row.setHeightInPoints(16);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
    }

    private void labelText(XSSFSheet sheet, int rowIdx, String label, String value, Styles s) {
        Row row = sheet.createRow(rowIdx);
        Cell lc = row.createCell(0); lc.setCellValue(label);           lc.setCellStyle(s.label);
        Cell vc = row.createCell(1); vc.setCellValue(value != null ? value : "—"); vc.setCellStyle(s.cell);
    }

    private void labelCurrency(XSSFSheet sheet, int rowIdx, String label, BigDecimal value, Styles s) {
        Row row = sheet.createRow(rowIdx);
        Cell lc = row.createCell(0); lc.setCellValue(label); lc.setCellStyle(s.label);
        Cell vc = row.createCell(1);
        if (value != null) { vc.setCellValue(value.doubleValue()); vc.setCellStyle(s.currency); }
        else { vc.setCellValue("—"); vc.setCellStyle(s.cell); }
    }

    private void labelPct(XSSFSheet sheet, int rowIdx, String label, BigDecimal value, Styles s) {
        Row row = sheet.createRow(rowIdx);
        Cell lc = row.createCell(0); lc.setCellValue(label); lc.setCellStyle(s.label);
        Cell vc = row.createCell(1);
        if (value != null) { vc.setCellValue(value.doubleValue()); vc.setCellStyle(s.pct); }
        else { vc.setCellValue("—"); vc.setCellStyle(s.cell); }
    }

    // ── Helpers de célula ─────────────────────────────────────────────────────

    private void headerCell(Row row, int col, String text, CellStyle style) {
        Cell c = row.createCell(col); c.setCellValue(text); c.setCellStyle(style);
    }

    private void textCell(Row row, int col, String text, CellStyle style) {
        Cell c = row.createCell(col); c.setCellValue(text); c.setCellStyle(style);
    }

    private void numCell(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col); c.setCellValue(value); c.setCellStyle(style);
    }

    private void currencyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        if (value != null) { c.setCellValue(value.doubleValue()); c.setCellStyle(style); }
        else { c.setCellValue("—"); }
    }

    private String formatDate(String iso) {
        if (iso == null) return "—";
        return iso.length() >= 19 ? iso.substring(0, 19).replace("T", " ") : iso;
    }

    // ── Estilos ───────────────────────────────────────────────────────────────

    /**
     * Agrupa os estilos necessários para não recriar workbook styles repetidamente.
     * O Excel tem limite de 64 000 estilos por workbook — criar um objeto reutilizável é boa prática.
     */
    private class Styles {
        final CellStyle title, sectionHeader, colHeader;
        final CellStyle label, cell, currency, pct;
        final CellStyle totalLabel, totalValue;

        Styles(XSSFWorkbook wb) {
            title         = buildTitle(wb);
            sectionHeader = buildSectionHeader(wb);
            colHeader     = buildColHeader(wb);
            label         = buildLabel(wb);
            cell          = buildCell(wb);
            currency      = buildCurrency(wb);
            pct           = buildPct(wb);
            totalLabel    = buildTotalLabel(wb);
            totalValue    = buildTotalValue(wb);
        }

        private CellStyle buildTitle(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 16);
            f.setColor(new XSSFColor(C_ORANGE, null));
            cs.setFont(f);
            return cs;
        }

        private CellStyle buildSectionHeader(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setFillForegroundColor(new XSSFColor(C_ORANGE, null));
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont f = wb.createFont();
            f.setBold(true);
            f.setColor(IndexedColors.WHITE.getIndex());
            cs.setFont(f);
            cs.setAlignment(HorizontalAlignment.LEFT);
            return cs;
        }

        private CellStyle buildColHeader(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setFillForegroundColor(new XSSFColor(C_DARK, null));
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont f = wb.createFont();
            f.setBold(true);
            f.setColor(IndexedColors.WHITE.getIndex());
            cs.setFont(f);
            cs.setAlignment(HorizontalAlignment.CENTER);
            cs.setBorderBottom(BorderStyle.MEDIUM);
            return cs;
        }

        private CellStyle buildLabel(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setBold(true);
            cs.setFont(f);
            return cs;
        }

        private CellStyle buildCell(XSSFWorkbook wb) {
            return wb.createCellStyle();
        }

        private CellStyle buildCurrency(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setDataFormat(wb.createDataFormat().getFormat("\"R$\" #,##0.00"));
            cs.setAlignment(HorizontalAlignment.RIGHT);
            return cs;
        }

        private CellStyle buildPct(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            // 0.00"%" — o % é literal, não multiplica por 100
            cs.setDataFormat(wb.createDataFormat().getFormat("0.00\"%\""));
            cs.setAlignment(HorizontalAlignment.LEFT);
            return cs;
        }

        private CellStyle buildTotalLabel(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setFillForegroundColor(new XSSFColor(C_LIGHT, null));
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cs.setBorderTop(BorderStyle.MEDIUM);
            XSSFFont f = wb.createFont();
            f.setBold(true);
            cs.setFont(f);
            return cs;
        }

        private CellStyle buildTotalValue(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setFillForegroundColor(new XSSFColor(C_LIGHT, null));
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cs.setBorderTop(BorderStyle.MEDIUM);
            cs.setDataFormat(wb.createDataFormat().getFormat("\"R$\" #,##0.00"));
            cs.setAlignment(HorizontalAlignment.RIGHT);
            XSSFFont f = wb.createFont();
            f.setBold(true);
            cs.setFont(f);
            return cs;
        }
    }
}
