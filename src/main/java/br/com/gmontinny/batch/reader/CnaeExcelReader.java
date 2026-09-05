package br.com.gmontinny.batch.reader;

import br.com.gmontinny.batch.CnaeRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.NonTransientResourceException;
import org.springframework.batch.infrastructure.item.ParseException;
import org.springframework.batch.infrastructure.item.UnexpectedInputException;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class CnaeExcelReader implements ItemReader<CnaeRow>, ItemStream {

    private static final String FILE_PATH = "data/CNAE20_EstruturaDetalhada.xls";
    private static final int HEADER_ROWS = 3; // linha 0=título, 1=continua, 2=cabeçalho

    private Iterator<CnaeRow> iterator;

    @Override
    public void open(ExecutionContext executionContext) {
        iterator = loadRows().iterator();
    }

    @Override
    public void close() {
        iterator = null;
    }

    @Override
    public CnaeRow read() throws UnexpectedInputException, ParseException, NonTransientResourceException {
        if (iterator == null) {
            iterator = loadRows().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<CnaeRow> loadRows() {
        List<CnaeRow> rows = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(FILE_PATH);
             Workbook workbook = new HSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = HEADER_ROWS; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                CnaeRow cnaeRow = new CnaeRow(
                        cellValue(row, 0), // secao
                        cellValue(row, 1), // divisao
                        cellValue(row, 2), // grupo
                        cellValue(row, 3), // classe
                        null,              // subclasse (não existe neste arquivo)
                        cellValue(row, 4), // denominacao
                        cellValue(row, 5)  // observacoes
                );

                if (cnaeRow.getDenominacao() != null && !cnaeRow.getDenominacao().isBlank()) {
                    rows.add(cnaeRow);
                }
            }
            log.info("Excel lido com sucesso: {} registros encontrados", rows.size());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo Excel: " + FILE_PATH, e);
        }
        return rows;
    }

    private String cellValue(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BLANK   -> null;
            default      -> null;
        };
    }
}
