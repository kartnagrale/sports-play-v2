package com.neml.badminton.controller;

import com.neml.badminton.entity.User;
import com.neml.badminton.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
import java.io.IOException;

@RestController
@RequestMapping("/api/championships/{championshipId}/admin/players/imports")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
public class PlayerImportController {
    private final PlayerWorkbookService workbooks;private final PlayerImportService imports;
    public PlayerImportController(PlayerWorkbookService workbooks,PlayerImportService imports){this.workbooks=workbooks;this.imports=imports;}
    @GetMapping("/template") public ResponseEntity<byte[]> template(@PathVariable UUID championshipId)throws IOException{
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=play-neml-players.xlsx").cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(workbooks.template());
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public PlayerImportService.Preview preview(@PathVariable UUID championshipId,@AuthenticationPrincipal User actor,@RequestParam MultipartFile file){return imports.preview(championshipId,actor.getId(),file);}
    @GetMapping("/{id}") public PlayerImportService.Preview status(@PathVariable UUID championshipId,@PathVariable UUID id,@AuthenticationPrincipal User actor){return imports.status(championshipId,actor.getId(),id);}
    public record Confirm(@NotBlank String checksum,boolean allowUnreferenced){}
    @PostMapping("/{id}/confirm") public PlayerImportService.Preview confirm(@PathVariable UUID championshipId,@PathVariable UUID id,@AuthenticationPrincipal User actor,@Valid @RequestBody Confirm request){return imports.confirm(championshipId,actor.getId(),id,request.checksum(),request.allowUnreferenced());}
}
