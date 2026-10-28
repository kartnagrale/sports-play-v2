package com.neml.badminton.service;

import com.neml.badminton.entity.Gender;
import com.neml.badminton.service.PlayerRegistrationService.Registration;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.openxml4j.opc.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.util.XMLHelper;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.*;

/** Bounded ZIP preflight plus SAX worksheet parsing: no workbook DOM or formula evaluation. */
@Service
public class PlayerWorkbookService {
    public static final List<String> HEADERS=List.of("Full Name","Gender","Skill Level","Base Price","Auction Order","Player Reference");
    public record Row(int rowNumber,Registration player,List<String> errors,boolean duplicate) {}
    private final int maxRows;
    private final long maxBytes, maxExpanded;
    public PlayerWorkbookService(@Value("${app.players.import.max-rows:2000}") int maxRows,
            @Value("${app.players.import.max-bytes:2097152}") long maxBytes,
            @Value("${app.players.import.max-expanded-bytes:16777216}") long maxExpanded) {
        this.maxRows=maxRows;this.maxBytes=maxBytes;this.maxExpanded=maxExpanded;
    }
    public byte[] template() throws IOException {
        try(var book=new XSSFWorkbook();var output=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("Players");var header=sheet.createRow(0);
            var style=book.createCellStyle();var font=book.createFont();font.setBold(true);style.setFont(font);
            for(int i=0;i<HEADERS.size();i++){header.createCell(i).setCellValue(HEADERS.get(i));header.getCell(i).setCellStyle(style);sheet.setColumnWidth(i,26*256);}
            sheet.createFreezePane(0,1);
            var helper=sheet.getDataValidationHelper();var genders=Arrays.stream(Gender.values()).map(Enum::name).toArray(String[]::new);
            var validation=helper.createValidation(helper.createExplicitListConstraint(genders),new CellRangeAddressList(1,maxRows,1,1));
            validation.setShowErrorBox(true);validation.createErrorBox("Gender","Select MALE or FEMALE");sheet.addValidationData(validation);
            var instructions=book.createSheet("Instructions and examples");
            String[] lines={"Play-Neml player registration — enter records on the Players sheet. Examples below are NOT imported.",
                "Full Name and Gender are required. Gender: "+String.join(", ",genders)+". Maximum "+maxRows+" players.",
                "Skill Level: optional text, maximum 255 characters. Full Name: maximum 255 characters.",
                "Base Price: optional positive number, maximum 17 whole digits and 2 decimal places. Blank uses championship default at confirmation.",
                "Auction Order: optional positive whole number; blank assigns automatically. Explicit orders must be unique in the auction.",
                "Player Reference: recommended stable ID (1–64 letters/digits/_/-). Compared case-insensitively, stored uppercase.",
                "Existing references are skipped without changing their player, team, price or photograph. Duplicate references in the file are errors.",
                "Same names may identify different people. Missing references require explicit acknowledgement; changed files may create duplicates.",
                "Exact workbook retries reuse the import. No formulas, macros, external links or embedded files. Only .xlsx files.",
                "Preview expires after 15 minutes by default. Any invalid row prevents the entire import. Blank rows are ignored.",
                "Example: Asha Rao | FEMALE | Advanced | 50000.00 | (blank) | NEML-001",
                "Example: Ravi Rao | MALE | Intermediate | (blank) | (blank) | NEML-002"};
            for(int i=0;i<lines.length;i++)instructions.createRow(i).createCell(0).setCellValue(lines[i]);
            instructions.setColumnWidth(0,120*256);book.write(output);return output.toByteArray();
        }
    }
    public List<Row> parse(MultipartFile file) {
        if(file.isEmpty()||file.getSize()>maxBytes)throw bad("Select a non-empty .xlsx workbook up to "+maxBytes/1024+" KB");
        Path path=null;
        try {
            path=Files.createTempFile("player-workbook-",".xlsx");
            try(InputStream in=file.getInputStream();OutputStream out=Files.newOutputStream(path)){
                byte[] buffer=new byte[8192];long size=0;int n;
                while((n=in.read(buffer))!=-1){size+=n;if(size>maxBytes)throw bad("Workbook exceeds the file size limit");out.write(buffer,0,n);}
            }
            try(var zip=new ZipFile(path.toFile())) {
                long expanded=0;int count=0;var entries=zip.entries();Set<String> names=new HashSet<>();
                while(entries.hasMoreElements()) {
                    var entry=entries.nextElement();String name=entry.getName();
                    if(++count>128||!names.add(name)||name.contains("..")||name.startsWith("/")||name.contains("\\"))throw bad("Unsupported workbook archive");
                    if(name.contains("vbaProject")||name.startsWith("xl/externalLinks/")||name.startsWith("xl/embeddings/"))throw bad("Macros, external links and embedded files are not supported");
                    try(var in=zip.getInputStream(entry)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){expanded+=n;if(expanded>maxExpanded)throw bad("Workbook expanded content is too large");}}
                }
            }
            try(var pkg=OPCPackage.open(path.toFile(),PackageAccess.READ)) {
                if(pkg.getPartsByContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml").size()!=1)throw bad("Only standard .xlsx workbooks are supported");
                var reader=new XSSFReader(pkg);var strings=new ReadOnlySharedStringsTable(pkg);
                var sheets=(XSSFReader.SheetIterator)reader.getSheetsData();List<Row> result=null;
                while(sheets.hasNext())try(var in=sheets.next()){
                    if(!"Players".equals(sheets.getSheetName()))continue;
                    if(result!=null)throw bad("Only one Players sheet is allowed");
                    var handler=new SheetHandler(strings);XMLReader parser=XMLHelper.newXMLReader();parser.setContentHandler(handler);parser.parse(new InputSource(in));result=handler.rows;
                    if(!handler.headerSeen)throw bad("Required column headers are missing from row 1");
                }
                if(result==null)throw bad("Workbook must contain a sheet named Players");
                if(result.isEmpty())throw bad("The Players sheet contains no player records");
                return result;
            }
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw bad("Malformed or unsupported .xlsx workbook. Download the template and try again.");}
        finally{if(path!=null)try{Files.deleteIfExists(path);}catch(IOException ignored){}}
    }
    private class SheetHandler extends DefaultHandler {
        final ReadOnlySharedStringsTable strings;final List<Row> rows=new ArrayList<>();
        final Map<Integer,String> values=new HashMap<>();Map<String,Integer> columns=new HashMap<>();
        int rowNumber,column,cellCount,physicalRows,lastRow;String type;boolean capture,formula,headerSeen;StringBuilder value=new StringBuilder();List<String> cellErrors=new ArrayList<>();
        SheetHandler(ReadOnlySharedStringsTable strings){this.strings=strings;}
        @Override public void startElement(String uri,String local,String q,Attributes a){
            if(local.equals("row")){rowNumber=Integer.parseInt(a.getValue("r"));if(++physicalRows>maxRows+1||rowNumber>maxRows+1)throw bad("Workbook exceeds "+maxRows+" rows");if(rowNumber<=lastRow)throw bad("Worksheet row numbers must be unique and increasing");lastRow=rowNumber;values.clear();cellErrors.clear();}
            if(local.equals("c")){if(++cellCount>(maxRows+1)*HEADERS.size())throw bad("Too many cells in workbook");String ref=a.getValue("r");column=0;for(char c:ref.toCharArray()){if(!Character.isLetter(c))break;column=column*26+(c-'A'+1);}column--;if(column>=HEADERS.size())throw bad("Unexpected extra columns in Players sheet");type=a.getValue("t");formula=false;value.setLength(0);}
            if(local.equals("f"))formula=true;
            if(local.equals("v")||local.equals("t"))capture=true;
        }
        @Override public void characters(char[] ch,int start,int length){if(capture){if(value.length()+length>1024)throw bad("Cell text exceeds the limit");value.append(ch,start,length);}}
        @Override public void endElement(String uri,String local,String q){
            if(local.equals("v")||local.equals("t"))capture=false;
            if(local.equals("c")){
                String text=value.toString();if("s".equals(type)&&!text.isEmpty())text=strings.getItemAt(Integer.parseInt(text)).getString();
                if(text.length()>255){cellErrors.add("Cell text exceeds 255 characters");text=text.substring(0,255);}
                if(formula||"e".equals(type)||"b".equals(type))cellErrors.add("Formulas, boolean and error cells are not supported");
                values.put(column,text.trim());
            }
            if(local.equals("row")){
                if(rowNumber==1){for(var e:values.entrySet()){if(columns.put(e.getValue(),e.getKey())!=null)throw bad("Duplicate column header");}if(!cellErrors.isEmpty()||!columns.keySet().containsAll(HEADERS))throw bad("Required columns: "+String.join(", ",HEADERS));headerSeen=true;return;}
                if(values.values().stream().allMatch(String::isBlank)&&cellErrors.isEmpty())return;
                if(!headerSeen)throw bad("Required column headers are missing from row 1");
                List<String> errors=new ArrayList<>(cellErrors);Gender gender=null;BigDecimal price=null;Integer order=null;
                try{gender=Gender.valueOf(get("Gender").toUpperCase(Locale.ROOT));}catch(Exception e){/* shared validator supplies error */}
                try{if(!get("Base Price").isBlank())price=new BigDecimal(get("Base Price"));}catch(Exception e){errors.add("Base Price must be a positive decimal number");}
                try{if(!get("Auction Order").isBlank())order=new BigDecimal(get("Auction Order")).intValueExact();}catch(Exception e){errors.add("Auction Order must be a positive whole number");}
                var r=new Registration(get("Full Name"),gender,price,get("Skill Level"),order,PlayerRegistrationService.reference(get("Player Reference")));
                errors.addAll(PlayerRegistrationService.validate(r));rows.add(new Row(rowNumber,r,errors,false));
            }
        }
        String get(String name){return values.getOrDefault(columns.get(name),"");}
    }
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
