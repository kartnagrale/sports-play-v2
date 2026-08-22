package com.neml.badminton.config;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.security.*;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.*;

@Component
public class StompAuthorizationInterceptor implements ChannelInterceptor {
    private static final Pattern TOPIC = Pattern.compile("^/topic/championship/([0-9a-fA-F-]{36})/(?:auction/[0-9a-fA-F-]{36}|matches)$");
    private final JwtService jwt; private final UserRepository users; private final ChampionshipRoleRepository roles;
    public StompAuthorizationInterceptor(JwtService jwt,UserRepository users,ChampionshipRoleRepository roles){this.jwt=jwt;this.users=users;this.roles=roles;}

    @Override public Message<?> preSend(Message<?> message, MessageChannel channel){
        StompHeaderAccessor a=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class); if(a==null)return message;
        if(StompCommand.CONNECT.equals(a.getCommand())) authenticate(a);
        if(StompCommand.SUBSCRIBE.equals(a.getCommand())) authorize(a);
        return message;
    }
    private void authenticate(StompHeaderAccessor a){
        String raw=first(a.getNativeHeader("Authorization")); if(raw==null)raw=first(a.getNativeHeader("authorization"));
        if(raw==null||!raw.startsWith("Bearer "))return;
        Claims c=jwt.parse(raw.substring(7));
        if("viewer".equals(c.get("token_type",String.class))){UUID id=UUID.fromString(c.get("championship_id",String.class));a.setUser(new UsernamePasswordAuthenticationToken(new ViewerPrincipal(id),null,List.of(new SimpleGrantedAuthority("ROLE_VIEWER"))));return;}
        users.findById(UUID.fromString(c.getSubject())).ifPresent(u->a.setUser(new UsernamePasswordAuthenticationToken(u,null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole().name())))));
    }
    private void authorize(StompHeaderAccessor a){
        Matcher m=TOPIC.matcher(Objects.toString(a.getDestination(),"")); if(!m.matches())throw new MessagingException("Subscription destination is not allowed");
        UUID cid=UUID.fromString(m.group(1)); Object principal=a.getUser() instanceof UsernamePasswordAuthenticationToken t?t.getPrincipal():null;
        boolean ok=principal instanceof ViewerPrincipal v&&cid.equals(v.championshipId());
        if(principal instanceof User u)ok=u.getRole()==Role.SUPER_ADMIN||roles.findByUserIdAndChampionshipId(u.getId(),cid).isPresent();
        if(!ok)throw new MessagingException("Not authorized for championship topic");
    }
    private String first(List<String> v){return v==null||v.isEmpty()?null:v.get(0);}
}
