/*
 * Copyright 2006-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.excel.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.batch.item.excel.Sheet;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Sheet implementation for Apache POI.
 *
 * @author Marten Deinum
 * @since 0.5.0
 */
class PoiSheet implements Sheet {

    private final org.apache.poi.ss.usermodel.Sheet delegate;
    private final int numberOfRows;
    private final String name;

    private FormulaEvaluator evaluator;

    /**
     * Constructor which takes the delegate sheet.
     *
     * @param delegate the apache POI sheet
     */
    PoiSheet(final org.apache.poi.ss.usermodel.Sheet delegate) {
        super();
        this.delegate = delegate;
        this.numberOfRows = this.delegate.getLastRowNum() + 1;
        this.name=this.delegate.getSheetName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfRows() {
        return this.numberOfRows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getRow(final int rowNumber) {
        final Row row = this.delegate.getRow(rowNumber);
        if (row == null) {
            return null;
        }
        final List<String> cells = new LinkedList<>();
        final int numberOfColumns = row.getLastCellNum();

        for (int i = 0; i < numberOfColumns; i++) {
            Cell cell = row.getCell(i);
            CellType cellType = cell.getCellTypeEnum();
            if (cellType == CellType.FORMULA) {
                FormulaEvaluator evaluator = getFormulaEvaluator();
                if (evaluator == null) {
                    cells.add(cell.getCellFormula());
                } else {
                    cellType = evaluator.evaluateFormulaCellEnum(cell);
                }
            }

            switch (cellType) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        cells.add(String.valueOf(date.getTime()));
                    } else {
                        cells.add(String.valueOf(cell.getNumericCellValue()));
                    }
                    break;
                case BOOLEAN:
                    cells.add(String.valueOf(cell.getBooleanCellValue()));
                    break;
                case STRING:
                case BLANK:
                    cells.add(cell.getStringCellValue());
                    break;
                case ERROR:
                    cells.add(FormulaError.forInt(cell.getErrorCellValue()).getString());
                    break;
                default:
                    throw new IllegalArgumentException("Cannot handle cells of type '" + cell.getCellTypeEnum() + "'");
            }
        }
        return cells.toArray(new String[0]);
    }

    private FormulaEvaluator getFormulaEvaluator() {
        if (this.evaluator == null) {
            this.evaluator = delegate.getWorkbook().getCreationHelper().createFormulaEvaluator();
        }
        return this.evaluator;
    }
}