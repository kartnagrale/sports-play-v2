package com.neml.badminton.certificate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.Set;

/** Operator-owned public URL; never derived from a request Host header or recipient input. */
@Component
public class CertificateVerificationPolicy {
    private final boolean enabled;
    private final String base;
    public CertificateVerificationPolicy(@Value("${app.certificates.public-enabled:false}") boolean enabled,
            @Value("${app.certificates.public-base-url:}") String base) {
        this.enabled=enabled;this.base=base.replaceAll("/+$","");
        if(enabled) {
            try {
                URI uri=URI.create(this.base);
                if(uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||this.base.length()>300||
                        !("https".equals(uri.getScheme())||("http".equals(uri.getScheme())&&Set.of("localhost","127.0.0.1","[::1]").contains(uri.getHost()))))
                    throw new IllegalArgumentException();
            }catch(Exception e){throw new IllegalArgumentException("Certificate verification requires a trusted HTTPS public base URL (HTTP permitted only for local verification)");}
        }
    }
    public boolean enabled(){return enabled;}
    public String url(String reference){return enabled?base+"/verify?reference="+reference:null;}
}
