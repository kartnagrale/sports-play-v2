package com.neml.badminton.service;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
public class LocalPlayerPhotoStorage implements PlayerPhotoStorage {
 private final Path root;
 public LocalPlayerPhotoStorage(Path root){this.root=root.toAbsolutePath().normalize();}
 private Path object(UUID key){Path target=root.resolve(key+".jpg").normalize();if(!target.getParent().equals(root))throw new IllegalArgumentException("Invalid photo key");return target;}
 public void put(UUID key,byte[] jpeg)throws IOException{
  Files.createDirectories(root);Path temp=Files.createTempFile(root,"upload-",".tmp");
  try{Files.write(temp,jpeg);Files.move(temp,object(key),StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temp);}
 }
 public byte[] read(UUID key)throws IOException{return Files.readAllBytes(object(key));}
 public void delete(UUID key)throws IOException{Files.deleteIfExists(object(key));}
}
