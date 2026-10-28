package com.neml.badminton.certificate;

import com.neml.badminton.certificate.CertificateModels.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;

class CertificatePdfTest {
    @Test void actualVectorPdfsHaveCorrectDataDimensionsAndStableBytes()throws Exception {
        var renderer=new CertificatePdfRenderer();var dir=Path.of("target/phase5-fixtures");Files.createDirectories(dir);
        for(var category:Category.values()) {
            var s=snapshot(category,"Élodie Fernández & Northside Aces");var bytes=renderer.render(s,false);
            assertThat(renderer.render(s,false)).isEqualTo(bytes);
            try(var doc=Loader.loadPDF(bytes)) {
                assertThat(doc.getNumberOfPages()).isEqualTo(1);
                assertThat(doc.getPage(0).getMediaBox().getWidth()).isBetween(841f,843f);
                assertThat(doc.getPage(0).getMediaBox().getHeight()).isBetween(594f,596f);
                String text=new PDFTextStripper().getText(doc).replaceAll("\\s+"," ");assertThat(text).contains(s.recipient().name(),s.reference(),"PLAY-NEML","Championship Administrator");
                assertThat(text).doesNotContain("PREVIEW ONLY");
                Files.write(dir.resolve(category+".pdf"),bytes);
                ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0,110),"png",dir.resolve(category+".png").toFile());
            }
        }
        var longName=snapshot(Category.WINNER,"Alexandria International Sporting Association ".repeat(5).substring(0,210)+" Élodie Fernández");
        try(var doc=Loader.loadPDF(renderer.render(longName,true))) {
            String text=new PDFTextStripper().getText(doc).replaceAll("\\s+"," ");
            assertThat(text).contains("PREVIEW ONLY","Alexandria International","Élodie Fernández");
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0,110),"png",dir.resolve("long-name.png").toFile());
        }
    }
    @Test void unsupportedCharactersRejectBeforeIssuance() {
        assertThatThrownBy(()->new CertificatePdfRenderer().render(snapshot(Category.PARTICIPATION,"Player 🏏"),false)).hasMessageContaining("unsupported");
    }
    static Snapshot snapshot(Category c,String name) {
        return new Snapshot(1,UUID.randomUUID(),"PN-0123456789ABCDEF0123456789ABCDEF",UUID.randomUUID(),"NEML Sports Championship 2026","BADMINTON","Championship · Finalized competition",UUID.randomUUID(),c,
                new Recipient(RecipientType.TEAM,UUID.randomUUID(),name,null,null),"In recognition of sporting excellence and championship achievement.",Instant.parse("2026-10-28T10:00:00Z"),"Tournament Operations","Championship Administrator",c==Category.WINNER?"#AF832E":c==Category.RUNNER_UP?"#67778B":"#16776C",null,null);
    }
}
