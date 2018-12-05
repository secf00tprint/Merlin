package de.micromata.merlin.excel.i18n;

import de.micromata.merlin.excel.*;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class I18nExcelConverter {
    private static Logger log = LoggerFactory.getLogger(I18nExcelConverter.class);

    @Getter
    private Translations translations;

    public I18nExcelConverter() {
        this.translations = new Translations();
    }

    public I18nExcelConverter(Translations translations) {
        this.translations = translations;
    }

    /**
     * @param inputStream
     * @param workbookName Only for logging purposes.
     * @throws IOException
     */
    public void importTranslations(InputStream inputStream, String workbookName) throws IOException {
        ExcelWorkbook workbook = new ExcelWorkbook(inputStream, workbookName);
        ExcelSheet sheet = workbook.getSheet("Translations");
        if (sheet == null) {
            log.error("Can't read translations from Excel workbook '" + workbookName + "'. No sheet named 'translations' founde.");
            return;
        }
        sheet.registerColumn("key", new ExcelColumnValidator().setRequired());

        Set<String> languages = new TreeSet<>();
        Row headRow = sheet.getHeadRow();
        for (int cellnum = 1; cellnum < headRow.getLastCellNum(); cellnum++) {
            String lang = PoiHelper.getValueAsString(headRow.getCell(cellnum)).trim();
            if (lang.length() > 3 || !StringUtils.isAlpha(lang)) {
                log.error("Ignoring column named '" + lang + "'. It's not seemed to be a language column.");
                continue;
            }
            languages.add(lang);
            sheet.registerColumn(lang, new ExcelColumnValidator());
        }
        sheet.reset();
        Iterator<Row> it = sheet.getDataRowIterator();
        while (it.hasNext()) {
            Row row = it.next();
            String key = sheet.getCellString(row,"key");
            for (String lang : languages) {
                String translation = sheet.getCellString(row, lang);
                translations.addTranslation(lang, key, translation);
            }
        }
        workbook.close();
    }

    public void write(OutputStream outputStream) throws IOException {
        Workbook poiWorkbook = new XSSFWorkbook();
        ExcelWorkbook workbook = new ExcelWorkbook(poiWorkbook);
        ExcelSheet sheet = workbook.createOrGetSheet("Translations");
        ExcelRow row = sheet.createRow();
        row.createCells("key");
        for (String lang : translations.getUsedLangs()) {
            row.createCells(lang);
        }
        for (String key : translations.getKeys()) {
            row = sheet.createRow();
            row.createCells(key);
            TranslationEntry entry = translations.getEntry(key);
            if (entry == null) continue; // Shouldn't occur.
            for (String lang : translations.getUsedLangs()) {
                row.createCells(StringUtils.defaultString(entry.getTranslation(lang)));
            }
        }
        workbook.getPOIWorkbook().write(outputStream);
        workbook.close();
    }
}