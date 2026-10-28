package com.neml.badminton.controller;
import com.neml.badminton.dto.Dtos.PlayerDto;
import com.neml.badminton.entity.User;
import com.neml.badminton.service.PlayerPhotoService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;
@RestController
@RequestMapping("/api/championships/{championshipId}")
public class PlayerPhotoController {
 private final PlayerPhotoService photos;
 public PlayerPhotoController(PlayerPhotoService photos){this.photos=photos;}
 @PutMapping(value="/admin/players/{id}/photo",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
 public PlayerDto upload(@PathVariable UUID championshipId,@PathVariable UUID id,@AuthenticationPrincipal User actor,@RequestParam MultipartFile file){return photos.upload(championshipId,id,actor.getId(),file);}
 @DeleteMapping("/admin/players/{id}/photo") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
 public PlayerDto remove(@PathVariable UUID championshipId,@PathVariable UUID id,@AuthenticationPrincipal User actor){return photos.remove(championshipId,id,actor.getId());}
 @GetMapping("/players/{id}/photo") @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
 public ResponseEntity<byte[]> image(@PathVariable UUID championshipId,@PathVariable UUID id,@RequestParam String version){return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore().cachePrivate()).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=player.jpg").body(photos.read(championshipId,id,version));}
}
