package com.neml.badminton.certificate;

import com.neml.badminton.certificate.CertificateModels.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.time.ZoneOffset;
import java.util.Locale;

@RestController
@RequestMapping("/api/certificate-verification")
public class CertificateVerificationController {
    private final JdbcTemplate db;private final CertificateEligibilityService json;private final CertificateVerificationPolicy policy;
    public CertificateVerificationController(JdbcTemplate db,CertificateEligibilityService json,CertificateVerificationPolicy policy){this.db=db;this.json=json;this.policy=policy;}
    @GetMapping("/{reference}")
    public ResponseEntity<Verification> verify(@PathVariable String reference) {
        String normalized=reference.toUpperCase(Locale.ROOT);
        Verification result=Verification.invalid();
        if(policy.enabled()&&normalized.matches("PN-[0-9A-F]{32}")) {
            result=db.query("select snapshot,status from play_neml.certificates where public_reference=?",(rs,n)->{
                var s=json.read(rs.getString(1),Snapshot.class);
                return new Verification("ISSUED".equals(rs.getString(2))?"VALID":"REVOKED",s.reference(),s.recipient().name(),s.championship(),s.sport(),s.category(),s.issuedAt().atZone(ZoneOffset.UTC).toLocalDate().toString());
            },normalized).stream().findFirst().orElse(result);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Robots-Tag","noindex, nofollow")
                .header("X-Content-Type-Options","nosniff").body(result);
    }
}
