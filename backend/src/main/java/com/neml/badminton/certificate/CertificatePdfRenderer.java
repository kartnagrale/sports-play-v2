package com.neml.badminton.certificate;

import com.neml.badminton.certificate.CertificateModels.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.cos.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Vector A4 landscape, embedded Unicode recipient font, bounded text boxes, no HTML or remote assets. */
@Component
public class CertificatePdfRenderer {
    private final java.util.concurrent.Semaphore slots=new java.util.concurrent.Semaphore(4,true);
    public byte[] image(byte[] pdf) {
        acquire();
        try(var doc=org.apache.pdfbox.Loader.loadPDF(pdf);var out=new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0,110),"png",out);
            return out.toByteArray();
        }catch(IOException e){throw new IllegalStateException("Unable to render certificate preview",e);}finally{slots.release();}
    }
    private static final float W=841.89f,H=595.28f;
    private static final Color INK=new Color(24,43,56),MUTED=new Color(85,101,111);
    public byte[] render(Snapshot s,boolean preview) {
        acquire();try{return renderDocument(s,preview);}finally{slots.release();}
    }
    private void acquire(){if(!slots.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Certificate renderer is busy; retry shortly");}
    private byte[] renderDocument(Snapshot s,boolean preview) {
        try(var doc=new PDDocument();var output=new ByteArrayOutputStream();var input=getClass().getResourceAsStream("/certificates/DejaVuSans.ttf")) {
            if(s.templateVersion()!=1)throw new IllegalStateException("Unknown certificate template version");
            PDType0Font font=PDType0Font.load(doc,Objects.requireNonNull(input));
            PDFont serif=new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            var page=new PDPage(new PDRectangle(W,H));doc.addPage(page);
            Color accent=Color.decode(s.accent());
            try(var g=new PDPageContentStream(doc,page)) {
                g.setNonStrokingColor(new Color(252,251,247));g.addRect(0,0,W,H);g.fill();
                g.setNonStrokingColor(INK);g.addRect(0,0,22,H);g.fill();
                g.setStrokingColor(accent);g.setLineWidth(1.4f);g.addRect(38,33,W-76,H-66);g.stroke();
                g.setLineWidth(.35f);g.addRect(44,39,W-88,H-78);g.stroke();
                // Restrained corner geometry remains vector at any print resolution.
                for(float x:new float[]{55,W-81}) {g.setNonStrokingColor(accent);g.addRect(x,H-70,26,3);g.fill();}
                center(g,font,"PLAY-NEML",13,H-86,INK);
                center(g,font,"SPORT  /  COMMITMENT  /  EXCELLENCE",7,H-103,MUTED);
                String heading=s.category()==Category.WINNER?"Certificate of Championship":s.category()==Category.RUNNER_UP?"Certificate of Achievement":"Certificate of Participation";
                center(g,serif,heading,32,H-156,INK);
                String award=s.category()==Category.WINNER?"CHAMPION":s.category()==Category.RUNNER_UP?"RUNNER-UP":"PARTICIPANT";
                center(g,font,award,11,H-180,accent);
                center(g,font,"Presented to "+s.recipient().type().name().toLowerCase(Locale.ROOT),9,H-210,MUTED);
                box(g,font,s.recipient().name(),38,9,660,4,H-254,INK);
                g.setStrokingColor(accent);g.moveTo(W/2-72,H-337);g.lineTo(W/2+72,H-337);g.stroke();
                box(g,font,s.achievement(),11,10,640,2,H-365,MUTED);
                box(g,font,s.championship(),16,11,660,2,H-397,INK);
                center(g,font,s.sport()+"  |  "+s.event(),8,H-435,MUTED);
                String date=DateTimeFormatter.ofPattern("dd MMM uuuu",Locale.ENGLISH).withZone(ZoneOffset.UTC).format(s.issuedAt());
                text(g,font,"ISSUED "+date.toUpperCase(Locale.ROOT),8,70,105,MUTED);
                text(g,font,"REFERENCE "+s.reference(),6.8f,70,89,MUTED);
                right(g,font,s.issuerTitle(),8,W-70,105,MUTED);
                rightBox(g,font,s.issuer(),11,8,285,2,W-70,88,INK);
                center(g,font,preview?"PREVIEW ONLY - NOT AN ISSUED CERTIFICATE":"Issued from approved competition results · Verify the reference with the championship administrator",6.5f,56,preview?new Color(170,45,45):MUTED);
                if(preview) {text(g,font,"PREVIEW",9,65,H-55,new Color(170,45,45));}
                decorateVerification(doc,page,g,font,s);
            }
            var info=doc.getDocumentInformation();info.setTitle(headingFor(s)+" - "+s.recipient().name());info.setAuthor("Play-Neml");
            var calendar=GregorianCalendar.from(s.issuedAt().atZone(ZoneOffset.UTC));info.setCreationDate(calendar);info.setModificationDate(calendar);
            var digest=MessageDigest.getInstance("SHA-256").digest((s.toString()+preview).getBytes(StandardCharsets.UTF_8));
            var ids=new COSArray();ids.add(new COSString(digest));ids.add(new COSString(digest));doc.getDocument().getTrailer().setItem(COSName.ID,ids);
            doc.save(output);return output.toByteArray();
        } catch(IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Certificate contains characters unsupported by the embedded font; use a supported spelling before issuing");
        } catch(ResponseStatusException e){throw e;}catch(Exception e){throw new IllegalStateException("Unable to render certificate",e);}
    }
    protected void decorateVerification(PDDocument doc,PDPage page,PDPageContentStream g,PDFont font,Snapshot s)throws Exception {
        if(s.verificationUrl()==null)return;
        var matrix=new com.google.zxing.qrcode.QRCodeWriter().encode(s.verificationUrl(),com.google.zxing.BarcodeFormat.QR_CODE,0,0,
                Map.of(com.google.zxing.EncodeHintType.MARGIN,4,com.google.zxing.EncodeHintType.ERROR_CORRECTION,com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M));
        float size=60,x=W/2-size/2,y=79,unit=size/matrix.getWidth();
        g.setNonStrokingColor(Color.WHITE);g.addRect(x,y,size,size);g.fill();g.setNonStrokingColor(INK);
        for(int row=0;row<matrix.getHeight();row++)for(int col=0;col<matrix.getWidth();col++)if(matrix.get(col,row))g.addRect(x+col*unit,y+(matrix.getHeight()-row-1)*unit,unit,unit);
        g.fill();center(g,font,"VERIFY AUTHENTICITY",5.5f,73,MUTED);
        var link=new org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink();link.setRectangle(new PDRectangle(x,y,size,size));
        var action=new org.apache.pdfbox.pdmodel.interactive.action.PDActionURI();action.setURI(s.verificationUrl());link.setAction(action);
        var border=new org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary();border.setWidth(0);link.setBorderStyle(border);page.getAnnotations().add(link);
    }
    private String headingFor(Snapshot s){return s.category().name()+" certificate";}
    private static void text(PDPageContentStream g,PDFont f,String value,float size,float x,float y,Color color)throws IOException {
        g.setNonStrokingColor(color);g.beginText();g.setFont(f,size);g.newLineAtOffset(x,y);g.showText(clean(value));g.endText();
    }
    private static void center(PDPageContentStream g,PDFont f,String value,float size,float y,Color color)throws IOException {
        text(g,f,value,size,(W-width(f,value,size))/2,y,color);
    }
    private static void right(PDPageContentStream g,PDFont f,String value,float size,float x,float y,Color color)throws IOException {
        text(g,f,value,size,x-width(f,value,size),y,color);
    }
    private static float width(PDFont f,String text,float size)throws IOException{return f.getStringWidth(clean(text))/1000*size;}
    private static String clean(String v){return v==null?"":v.replaceAll("[\\p{Cntrl}]"," ").replaceAll("\\s+"," ").trim();}
    private static List<String> wrap(PDFont f,String value,float size,float max)throws IOException {
        List<String> lines=new ArrayList<>();String line="";
        for(String word:clean(value).split(" ")) {
            if(!line.isEmpty()&&width(f,line+" "+word,size)>max){lines.add(line);line="";}
            for(int cp:word.codePoints().toArray()) {
                String glyph=new String(Character.toChars(cp));
                if(width(f,line+glyph,size)>max){lines.add(line);line="";}
                line+=glyph;
            }
            line+=" ";
        }
        if(!line.isBlank())lines.add(line.trim());return lines;
    }
    private static void box(PDPageContentStream g,PDFont f,String value,float preferred,float minimum,float width,int maxLines,float y,Color color)throws IOException {
        float size=preferred;List<String> lines=wrap(f,value,size,width);
        while((lines.size()>maxLines||lines.size()*size*1.25f>80)&&size>minimum){size-=.5f;lines=wrap(f,value,size,width);}
        if(lines.size()>maxLines)throw new IllegalArgumentException("Text does not fit certificate");
        for(String line:lines){center(g,f,line.trim(),size,y,color);y-=size*1.25f;}
    }
    private static void rightBox(PDPageContentStream g,PDFont f,String value,float preferred,float minimum,float width,int maxLines,float x,float y,Color color)throws IOException {
        float size=preferred;List<String> lines=wrap(f,value,size,width);
        while(lines.size()>maxLines&&size>minimum){size-=.5f;lines=wrap(f,value,size,width);}
        if(lines.size()>maxLines)throw new IllegalArgumentException("Issuer name does not fit");
        for(String line:lines){right(g,f,line.trim(),size,x,y,color);y-=size*1.2f;}
    }
}
