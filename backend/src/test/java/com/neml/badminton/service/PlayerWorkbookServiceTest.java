package com.neml.badminton.service;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class PlayerWorkbookServiceTest {
    final PlayerWorkbookService service=new PlayerWorkbookService(2000,2097152,16777216);
    static byte[] workbook(String[][] rows)throws Exception{
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()){
            var sheet=book.createSheet("Players");var header=sheet.createRow(0);
            for(int i=0;i<PlayerWorkbookService.HEADERS.size();i++)header.createCell(i).setCellValue(PlayerWorkbookService.HEADERS.get(i));
            for(int i=0;i<rows.length;i++){var row=sheet.createRow(i+1);for(int j=0;j<rows[i].length;j++)row.createCell(j).setCellValue(rows[i][j]);}
            book.write(out);return out.toByteArray();
        }
    }
    static MockMultipartFile file(byte[] data){return new MockMultipartFile("file","players.xlsx","application/octet-stream",data);}
    @Test void parsesDefaultsAndSameNamesWithDifferentReferences()throws Exception{
        var rows=service.parse(file(workbook(new String[][]{{"Asha Rao","FEMALE","Advanced","","","NEML-001"},{"Asha Rao","FEMALE","","50.25","7","neml-002"}})));
        assertThat(rows).hasSize(2);assertThat(rows.get(0).errors()).isEmpty();assertThat(rows.get(0).player().basePrice()).isNull();
        assertThat(rows.get(1).player().playerReference()).isEqualTo("NEML-002");assertThat(rows.get(1).player().basePrice()).isEqualByComparingTo("50.25");
    }
    @Test void templateHasInstructionsValidationAndNoImportableExamples()throws Exception{
        try(var b=new XSSFWorkbook(new ByteArrayInputStream(service.template()))){assertThat(b.getSheet("Players").getLastRowNum()).isZero();assertThat(b.getSheet("Players").getDataValidations()).hasSize(1);assertThat(b.getSheetAt(1).getLastRowNum()).isGreaterThan(5);}
    }
    @Test void invalidFieldsHaveRowLevelErrors()throws Exception{
        var rows=service.parse(file(workbook(new String[][]{{"","OTHER","","-1","1.2","bad ref"},{"Test","MALE","","1.001","1","R2"}})));
        assertThat(rows.get(0).rowNumber()).isEqualTo(2);assertThat(rows.get(0).errors()).hasSizeGreaterThanOrEqualTo(4);assertThat(rows.get(1).errors()).anyMatch(e->e.contains("2 decimal"));
    }
    @Test void rejectsMalformedEmptyUnsupportedAndOversize(){
        assertThatThrownBy(()->service.parse(file(new byte[]{1,2,3}))).hasMessageContaining("Malformed");
        assertThatThrownBy(()->service.parse(file(new byte[2097153]))).hasMessageContaining("non-empty");
        assertThatThrownBy(()->service.parse(file(workbook(new String[][]{})))).hasMessageContaining("no player");
    }
    @Test void rejectsMissingHeadersAndFormulaCells()throws Exception{
        try(var b=new XSSFWorkbook(new ByteArrayInputStream(workbook(new String[][]{{"Name","MALE"}})));var out=new ByteArrayOutputStream()){
            b.getSheetAt(0).getRow(1).getCell(0).setCellFormula("1+1");b.write(out);
            assertThat(service.parse(file(out.toByteArray())).get(0).errors()).anyMatch(e->e.contains("Formulas"));
        }
        try(var b=new XSSFWorkbook(new ByteArrayInputStream(workbook(new String[][]{{"Name","MALE"}})));var out=new ByteArrayOutputStream()){
            b.getSheetAt(0).getRow(0).getCell(0).setCellValue("Wrong");b.write(out);
            assertThatThrownBy(()->service.parse(file(out.toByteArray()))).hasMessageContaining("Required columns");
        }
    }
    @Test void boundedLargeWorkbookAndBlankRows()throws Exception{
        String[][] rows=new String[2000][];for(int i=0;i<rows.length;i++)rows[i]=new String[]{"Player "+i,"MALE","","","","REF-"+i};
        assertThat(service.parse(file(workbook(rows)))).hasSize(2000);
        assertThat(service.parse(file(workbook(new String[][]{{"",""},{"Actual Player","FEMALE"}})))).hasSize(1);
    }
    @Test void writesReusableRealApiFixtures()throws Exception{
        var folder=Path.of("target/phase4-fixtures");Files.createDirectories(folder);
        Files.write(folder.resolve("valid.xlsx"),workbook(new String[][]{{"Asha Rao","FEMALE","Advanced","","","NEML-001"},{"Asha Rao","FEMALE","","75000.25","10","NEML-002"}}));
        Files.write(folder.resolve("invalid.xlsx"),workbook(new String[][]{{"Invalid","OTHER","","-20","","BAD-1"}}));
        Files.write(folder.resolve("duplicates.xlsx"),workbook(new String[][]{{"One","MALE","","","","DUP-1"},{"Two","FEMALE","","","","DUP-1"}}));
        Files.write(folder.resolve("orders.xlsx"),workbook(new String[][]{{"New","MALE","","","99","ORDER-1"},{"Other","FEMALE","","","100","ORDER-2"}}));
        Files.write(folder.resolve("noref.xlsx"),workbook(new String[][]{{"No Reference","MALE","","","",""}}));
        String[][] large=new String[2000][];for(int i=0;i<large.length;i++)large[i]=new String[]{"Player "+i,"MALE","","","","LARGE-"+i};Files.write(folder.resolve("large.xlsx"),workbook(large));
    }
}
