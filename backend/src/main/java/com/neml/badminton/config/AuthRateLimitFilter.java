package com.neml.badminton.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;

/** Bounded per-instance limiter. Use a shared edge limiter before scaling to multiple instances. */
@Component @Order(1)
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private record Window(long start,int attempts) {}
    private final Map<String,Window> windows=new HashMap<>();
    private final int maximum;
    public AuthRateLimitFilter(@Value("${app.auth.rate-limit-attempts:300}") int maximum){
        if(maximum<1||maximum>100000)throw new IllegalArgumentException("Rate limit must be between 1 and 100000");
        this.maximum=maximum;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String path=request.getRequestURI();
        if("POST".equals(request.getMethod())&&Set.of("/api/auth/login","/api/auth/activate","/api/championships/join-room").contains(path)){
            boolean allowed;
            synchronized(windows){
                long now=Instant.now().getEpochSecond();
                windows.entrySet().removeIf(e->now-e.getValue().start()>=900);
                String key=request.getRemoteAddr()+":"+path;
                Window old=windows.get(key);
                allowed=(old!=null||windows.size()<10000)&&(old==null||old.attempts()<maximum);
                if(allowed)windows.put(key,new Window(old==null?now:old.start(),old==null?1:old.attempts()+1));
            }
            if(!allowed){response.setStatus(429);response.setHeader("Retry-After","900");return;}
        }
        chain.doFilter(request,response);
    }
}
