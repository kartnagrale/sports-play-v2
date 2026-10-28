package com.neml.badminton.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
@Service
public class PlayerImageProcessor {
 private final long maxBytes,maxPixels;private final int maxDimension;
 public PlayerImageProcessor(@Value("${app.players.photo.max-bytes:5242880}") long maxBytes,
   @Value("${app.players.photo.max-dimension:4096}") int maxDimension,@Value("${app.players.photo.max-pixels:12000000}") long maxPixels){this.maxBytes=maxBytes;this.maxDimension=maxDimension;this.maxPixels=maxPixels;}
 public byte[] normalize(MultipartFile file){
  if(file.isEmpty()||file.getSize()>maxBytes)throw bad("Select a non-empty JPEG or PNG up to 5 MB");
  try(var input=file.getInputStream();var bounded=new ByteArrayOutputStream()){
   byte[] buffer=new byte[8192];int n;long size=0;while((n=input.read(buffer))!=-1){size+=n;if(size>maxBytes)throw bad("Photo exceeds the file size limit");bounded.write(buffer,0,n);}
   try(var stream=new MemoryCacheImageInputStream(new ByteArrayInputStream(bounded.toByteArray()))){
    var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw bad("Invalid image. Use a JPEG or PNG photograph");var reader=readers.next();
    try{
     String format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!format.equals("jpeg")&&!format.equals("png"))throw bad("Only decoded JPEG and PNG images are supported");
     reader.setInput(stream,true,true);int width=reader.getWidth(0),height=reader.getHeight(0);
     if(width<1||height<1||width>maxDimension||height>maxDimension||(long)width*height>maxPixels)throw bad("Photo dimensions exceed the limit (4096 per side, 12 megapixels)");
     BufferedImage source=reader.read(0);if(source==null)throw bad("Image could not be decoded");
     BufferedImage normalized=new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);Graphics2D g=normalized.createGraphics();
     try{g.setColor(Color.WHITE);g.fillRect(0,0,512,512);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);int side=Math.min(width,height),x=(width-side)/2,y=(height-side)/2;g.drawImage(source,0,0,512,512,x,y,x+side,y+side,null);}finally{g.dispose();source.flush();}
     // Write only fresh pixels: no source EXIF, comments, profiles, paths or appended data survive.
     try(var output=new ByteArrayOutputStream();var imageOutput=new MemoryCacheImageOutputStream(output)){
      var writer=ImageIO.getImageWritersByFormatName("jpeg").next();
      try{writer.setOutput(imageOutput);var options=writer.getDefaultWriteParam();options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);options.setCompressionQuality(.85f);writer.write(null,new IIOImage(normalized,null,null),options);imageOutput.flush();return output.toByteArray();}finally{writer.dispose();normalized.flush();}
     }
    }finally{reader.dispose();}
   }
  }catch(ResponseStatusException e){throw e;}catch(IOException|RuntimeException e){throw bad("Invalid or damaged image. Use a JPEG or PNG photograph");}
 }
 private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
