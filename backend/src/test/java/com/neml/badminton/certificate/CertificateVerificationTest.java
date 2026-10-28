package com.neml.badminton.certificate;

import org.junit.jupiter.api.Test;
import com.neml.badminton.certificate.CertificateModels.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class CertificateVerificationTest {
    @Test void disabledPublicVerificationNeverLooksUpOrDisclosesIssuedRecords() {
        var db=org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var controller=new CertificateVerificationController(db,null,new CertificateVerificationPolicy(false,""));
        assertThat(controller.verify("PN-0123456789ABCDEF0123456789ABCDEF").getBody()).isEqualTo(Verification.invalid());
        org.mockito.Mockito.verifyNoInteractions(db);
    }
    @Test void publicConfigurationIsOptInAndRejectsUntrustedOrMissingDestinations() {
        assertThat(new CertificateVerificationPolicy(false,"").url("PN-123")).isNull();
        for(String value:List.of("","javascript:alert(1)","http://public.example","https://user@example.org","https://example.org/?override=1","https://example.org/#x"))
            assertThatThrownBy(()->new CertificateVerificationPolicy(true,value)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new CertificateVerificationPolicy(true,"http://localhost:53005/").url("PN-123")).isEqualTo("http://localhost:53005/verify?reference=PN-123");
    }
    @Test void actualVectorQrDecodesToConfiguredDestinationAndHasClickableLink()throws Exception {
        Files.createDirectories(Path.of("target/phase5-fixtures"));
        var old=CertificatePdfTest.snapshot(Category.WINNER,"Northside Aces");String url="https://certificates.example.test/verify?reference="+old.reference();
        var snapshot=new Snapshot(old.templateVersion(),old.id(),old.reference(),old.championshipId(),old.championship(),old.sport(),old.event(),old.approvalId(),old.category(),old.recipient(),old.achievement(),old.issuedAt(),old.issuer(),old.issuerTitle(),old.accent(),null,url);
        var bytes=new CertificatePdfRenderer().render(snapshot,false);
        try(var doc=Loader.loadPDF(bytes)) {
            var image=new PDFRenderer(doc).renderImageWithDPI(0,216);int x=Math.round((841.89f/2-30)*3),y=Math.round((595.28f-79-60)*3);
            var crop=image.getSubimage(x-4,y-4,188,188);int[] pixels=crop.getRGB(0,0,crop.getWidth(),crop.getHeight(),null,0,crop.getWidth());
            var result=new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(crop.getWidth(),crop.getHeight(),pixels))));
            assertThat(result.getText()).isEqualTo(url);
            var link=(PDAnnotationLink)doc.getPage(0).getAnnotations().get(0);assertThat(((PDActionURI)link.getAction()).getURI()).isEqualTo(url);
            Files.write(Path.of("target/phase5-fixtures/qr-certificate.pdf"),bytes);
            javax.imageio.ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0,110),"png",Path.of("target/phase5-fixtures/qr-certificate.png").toFile());
        }
    }
    @Test void largestAllowedBatchProducesTwentyFivePdfsWithinArchiveBudget()throws Exception {
        var list=new ArrayList<Issued>();
        // Fixture references must also be unique, as enforced by the issuance table.
        for(int i=0;i<25;i++){var old=CertificatePdfTest.snapshot(Category.PARTICIPATION,"Participant "+i);var s=new Snapshot(old.templateVersion(),old.id(),"PN-"+UUID.randomUUID().toString().replace("-",""),old.championshipId(),old.championship(),old.sport(),old.event(),old.approvalId(),old.category(),old.recipient(),old.achievement(),old.issuedAt(),old.issuer(),old.issuerTitle(),old.accent(),null,null);list.add(new Issued(s,"ISSUED",null,null));}
        var service=new CertificateBatchService(null,null,null,new CertificatePdfRenderer());byte[] zip=service.zip(new Batch(UUID.randomUUID(),list));assertThat(zip.length).isLessThan(8*1024*1024);
        int count=0;try(var input=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zip))){while(input.getNextEntry()!=null){try(var pdf=Loader.loadPDF(input.readAllBytes())){assertThat(pdf.getNumberOfPages()).isEqualTo(1);}count++;}}assertThat(count).isEqualTo(25);
        new CertificatePdfRenderer().render(CertificatePdfTest.snapshot(Category.WINNER,"W".repeat(255)),false);
    }
}
