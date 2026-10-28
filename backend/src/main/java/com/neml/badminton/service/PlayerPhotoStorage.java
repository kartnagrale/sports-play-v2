package com.neml.badminton.service;
import java.io.IOException;
import java.util.UUID;
/** Private immutable objects. Implement this boundary for shared object storage in production. */
public interface PlayerPhotoStorage {
 void put(UUID key,byte[] jpeg)throws IOException;
 byte[] read(UUID key)throws IOException;
 void delete(UUID key)throws IOException;
}
