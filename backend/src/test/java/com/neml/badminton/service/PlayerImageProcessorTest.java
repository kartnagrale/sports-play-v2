package com.neml.badminton.service;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class PlayerImageProcessorTest {
 final PlayerImageProcessor processor=new PlayerImageProcessor(5242880,4096,12000000);
 @TempDir Path folder;
 static byte[] image(String format,int width,int height,Color color)throws Exception{
  var img=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var g=img.createGraphics();g.setColor(color);g.fillRect(0,0,width,height);g.setColor(Color.WHITE);g.fillOval(width/4,height/8,width/2,height/2);g.fillOval(width/8,height/2,width*3/4,height/2);g.dispose();try(var out=new ByteArrayOutputStream()){ImageIO.write(img,format,out);return out.toByteArray();}finally{img.flush();}
 }
 static MockMultipartFile file(byte[] content){return new MockMultipartFile("file","../../camera.jpg","image/jpeg",content);}
 @Test void decodesNormalizesCropsAndStripsTrailingPrivateMetadata()throws Exception{
  for(String format:java.util.List.of("jpg","png")){
   byte[] source=image(format,640,480,Color.BLUE),marker="LOCATION-SECRET-METADATA".getBytes(java.nio.charset.StandardCharsets.US_ASCII);var joined=new ByteArrayOutputStream();joined.write(source);joined.write(marker);
   byte[] result=processor.normalize(file(joined.toByteArray()));var decoded=ImageIO.read(new ByteArrayInputStream(result));assertThat(decoded.getWidth()).isEqualTo(512);assertThat(decoded.getHeight()).isEqualTo(512);assertThat(new String(result,java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("LOCATION-SECRET-METADATA");assertThat(result[0]&255).isEqualTo(255);assertThat(result[1]&255).isEqualTo(216);
  }
 }
 @Test void rejectsSvgGifForgedContentOversizeDimensionsAndPixels()throws Exception{
  assertThatThrownBy(()->processor.normalize(file("<svg><script>bad()</script></svg>".getBytes()))).hasMessageContaining("Invalid image");
  assertThatThrownBy(()->processor.normalize(file(image("gif",64,64,Color.RED)))).hasMessageContaining("JPEG and PNG");
  assertThatThrownBy(()->processor.normalize(file(new byte[5242881]))).hasMessageContaining("5 MB");
  assertThatThrownBy(()->processor.normalize(file(image("png",4097,2,Color.RED)))).hasMessageContaining("dimensions");
  assertThatThrownBy(()->new PlayerImageProcessor(5242880,4096,100).normalize(file(image("png",64,64,Color.RED)))).hasMessageContaining("dimensions");
 }
 @Test void privateStorageUsesGeneratedKeysAndDeleteIsIdempotent()throws Exception{
  var storage=new LocalPlayerPhotoStorage(folder);UUID key=UUID.randomUUID();byte[] bytes=processor.normalize(file(image("png",64,64,Color.GREEN)));storage.put(key,bytes);assertThat(storage.read(key)).isEqualTo(bytes);try(var files=Files.list(folder)){assertThat(files.map(p->p.getFileName().toString()).toList()).containsExactly(key+".jpg");}storage.delete(key);storage.delete(key);assertThatThrownBy(()->storage.read(key)).isInstanceOf(IOException.class);
 }
 @Test void writesSafeSyntheticPortraitFixtures()throws Exception{
  var target=Path.of("target/phase4-fixtures");Files.createDirectories(target);Files.write(target.resolve("portrait.png"),image("png",640,480,new Color(24,130,116)));Files.write(target.resolve("replacement.jpg"),image("jpg",480,640,new Color(123,65,160)));Files.write(target.resolve("invalid.jpg"),"<svg>not a photograph</svg>".getBytes());Files.write(target.resolve("unsupported.gif"),image("gif",64,64,Color.RED));
 }
}
