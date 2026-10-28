package com.neml.badminton.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;
/** Bounded per-peer upload limiter; shared edge limits are still required for multi-instance deployments. */
@Component @Order(2)
public class PlayerUploadRateLimitFilter extends OncePerRequestFilter {
 private record Window(long start,int attempts){}
 private final Map<String,Window> windows=new HashMap<>();private final int maximum;
 public PlayerUploadRateLimitFilter(@Value("${app.players.upload-attempts-per-minute:30}") int maximum){this.maximum=maximum;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  if(("POST".equals(req.getMethod())||"PUT".equals(req.getMethod()))&&req.getRequestURI().matches("/api/championships/[^/]+/admin/players/(imports|[^/]+/photo)")){
   boolean allowed;synchronized(windows){long now=System.currentTimeMillis();windows.entrySet().removeIf(e->now-e.getValue().start>=60000);String key=req.getRemoteAddr();var old=windows.get(key);allowed=(old!=null||windows.size()<10000)&&(old==null||old.attempts<maximum);if(allowed)windows.put(key,new Window(old==null?now:old.start,old==null?1:old.attempts+1));}
   if(!allowed){res.setStatus(429);res.setHeader("Retry-After","60");res.setContentType("application/json");res.getWriter().write("{\"message\":\"Too many uploads. Please retry in a minute.\"}");return;}
  }chain.doFilter(req,res);
 }
}
