package com.neml.badminton.certificate;

import com.neml.badminton.certificate.CertificateModels.*;
import com.neml.badminton.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@RestController
@RequestMapping("/api/championships/{championshipId}/admin/certificates/batches")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
public class CertificateBatchController {
    private final CertificateBatchService service;private final Semaphore slots=new Semaphore(2,true);
    public CertificateBatchController(CertificateBatchService service){this.service=service;}
    @PostMapping
    public Batch issue(@PathVariable UUID championshipId,@AuthenticationPrincipal User actor,@Valid @RequestBody BulkRequest request) {
        acquire();try{return service.issue(championshipId,actor,request);}finally{slots.release();}
    }
    @GetMapping("/{id}/zip")
    public ResponseEntity<byte[]> zip(@PathVariable UUID championshipId,@PathVariable UUID id) {
        acquire();try{return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip")).cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"certificates-"+id+".zip\"")
                .header("X-Content-Type-Options","nosniff").body(service.zip(service.batch(championshipId,id)));}finally{slots.release();}
    }
    private void acquire(){if(!slots.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Certificate batch capacity is busy; retry shortly with the same request");}
}
