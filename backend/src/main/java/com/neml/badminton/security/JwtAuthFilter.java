package com.neml.badminton.security;

import com.neml.badminton.config.RequestLoggingFilter;
import com.neml.badminton.entity.User;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String COOKIE_NAME="neml_auth";
    private final TokenAuthenticationService tokens;
    public JwtAuthFilter(TokenAuthenticationService tokens){this.tokens=tokens;}
    @Override protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,@NonNull FilterChain chain) throws ServletException,IOException {
        String token=null,header=request.getHeader("Authorization");
        if(header!=null&&header.startsWith("Bearer "))token=header.substring(7);
        else if(request.getCookies()!=null)for(Cookie cookie:request.getCookies())
            if(COOKIE_NAME.equals(cookie.getName())){token=cookie.getValue();break;}
        if(token!=null){
            try {
                var auth=tokens.authenticate(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                Object principal=auth.getPrincipal();
                request.setAttribute(RequestLoggingFilter.ACTOR_ATTRIBUTE,principal instanceof User user?
                        "user:"+user.getId():"viewer:"+((ViewerPrincipal)principal).championshipId());
            }catch(JwtException|IllegalArgumentException e){SecurityContextHolder.clearContext();}
        }
        chain.doFilter(request,response);
    }
}
