package com.neml.badminton.certificate;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Bound JSON parsing before bean validation, including requests without Content-Length. */
@Component
public class CertificateRequestLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT=16*1024;
    @Override protected boolean shouldNotFilter(HttpServletRequest r){return !r.getRequestURI().matches("/api/championships/[^/]+/admin/certificates(?:/.*)?")||!"POST".equals(r.getMethod());}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        if(request.getContentLengthLong()>LIMIT){response.sendError(413,"Certificate request exceeds 16 KiB");return;}
        var wrapped=new HttpServletRequestWrapper(request) {
            private ServletInputStream bounded;
            @Override public ServletInputStream getInputStream()throws IOException {
                if(bounded!=null)return bounded;var source=super.getInputStream();
                bounded=new ServletInputStream() {
                    private int count;
                    @Override public int read()throws IOException {int value=source.read();if(value!=-1&&++count>LIMIT)throw new IOException("Certificate request exceeds 16 KiB");return value;}
                    @Override public boolean isFinished(){return source.isFinished();}
                    @Override public boolean isReady(){return source.isReady();}
                    @Override public void setReadListener(ReadListener listener){source.setReadListener(listener);}
                };return bounded;
            }
        };
        chain.doFilter(wrapped,response);
    }
}
