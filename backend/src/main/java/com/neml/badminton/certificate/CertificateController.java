package com.neml.badminton.certificate;

import com.neml.badminton.certificate.CertificateModels.*;
import com.neml.badminton.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** Administrative history and downloads deliberately share the existing tenant administrator boundary. */
@RestController
@RequestMapping("/api/championships/{championshipId}/admin/certificates")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
public class CertificateController {
    private final CertificateService service;
    private final CertificateEligibilityService eligibility;
    private final CertificatePdfRenderer renderer;
    public CertificateController(CertificateService service,CertificateEligibilityService eligibility,CertificatePdfRenderer renderer) {
        this.service=service;this.eligibility=eligibility;this.renderer=renderer;
    }
    @GetMapping("/eligibility")
    public Eligibility eligibility(@PathVariable UUID championshipId,@RequestParam Category category,@RequestParam RecipientType recipientType) {
        return eligibility.eligibility(championshipId,category,recipientType);
    }
    @PostMapping("/approvals")
    public Approval approve(@PathVariable UUID championshipId,@AuthenticationPrincipal User actor,@Valid @RequestBody ApproveRequest request) {
        return eligibility.approve(championshipId,actor.getId(),request.justification());
    }
    @PostMapping("/preview")
    public ResponseEntity<byte[]> preview(@PathVariable UUID championshipId,@AuthenticationPrincipal User actor,@Valid @RequestBody IssueRequest request,@RequestParam(defaultValue="pdf") String format) {
        var bytes=renderer.render(service.preview(championshipId,actor,request),true);
        return "png".equals(format)?png(renderer.image(bytes)):pdf(bytes,"preview",true);
    }
    @PostMapping
    public Issued issue(@PathVariable UUID championshipId,@AuthenticationPrincipal User actor,@Valid @RequestBody IssueRequest request) {
        return service.issue(championshipId,actor,request);
    }
    @GetMapping
    public History history(@PathVariable UUID championshipId,@RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="") String category,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page) {
        return service.history(championshipId,search,category,status,page);
    }
    @GetMapping("/{id}")
    public Issued get(@PathVariable UUID championshipId,@PathVariable UUID id){return service.get(championshipId,id);}
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable UUID championshipId,@PathVariable UUID id) {
        return png(renderer.image(renderer.render(service.get(championshipId,id).certificate(),false)));
    }
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable UUID championshipId,@PathVariable UUID id,@RequestParam(defaultValue="false") boolean inline) {
        var issued=service.get(championshipId,id);return pdf(renderer.render(issued.certificate(),false),issued.certificate().reference(),inline);
    }
    @PostMapping("/{id}/revoke")
    public Issued revoke(@PathVariable UUID championshipId,@PathVariable UUID id,@AuthenticationPrincipal User actor,@Valid @RequestBody RevokeRequest request) {
        return service.revoke(championshipId,id,actor.getId(),request.reason());
    }
    static ResponseEntity<byte[]> pdf(byte[] bytes,String name,boolean inline) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,(inline?"inline":"attachment")+"; filename=\""+name+".pdf\"")
                .header("X-Content-Type-Options","nosniff").body(bytes);
    }
    static ResponseEntity<byte[]> png(byte[] bytes) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore().cachePrivate())
                .header("X-Content-Type-Options","nosniff").body(bytes);
    }
}
