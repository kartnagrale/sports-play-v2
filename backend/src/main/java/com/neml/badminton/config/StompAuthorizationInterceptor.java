package com.neml.badminton.config;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.security.*;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;

@Component
public class StompAuthorizationInterceptor implements ChannelInterceptor {
    private static final String UUID_PATTERN="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Pattern TOPIC=Pattern.compile("^/topic/championship/("+UUID_PATTERN+")/(?:auction/("+UUID_PATTERN+")|matches)$");
    private final TokenAuthenticationService tokens;
    private final UserRepository users;
    private final ChampionshipRoleRepository roles;
    private final AuctionRepository auctions;
    private final Map<String,Authentication> sessions=new ConcurrentHashMap<>();
    public StompAuthorizationInterceptor(TokenAuthenticationService tokens,UserRepository users,
            ChampionshipRoleRepository roles,AuctionRepository auctions){
        this.tokens=tokens;this.users=users;this.roles=roles;this.auctions=auctions;
    }
    @Override public Message<?> preSend(Message<?> message,MessageChannel channel){
        var headers=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
        if(headers==null)return message;
        if(StompCommand.SEND.equals(headers.getCommand()))throw new MessagingException("Client publication is not allowed");
        if(StompCommand.CONNECT.equals(headers.getCommand())){
            String bearer=headers.getFirstNativeHeader("Authorization");
            if(bearer==null)bearer=headers.getFirstNativeHeader("authorization");
            if(bearer!=null){
                try {
                    if(!bearer.startsWith("Bearer "))throw new IllegalArgumentException();
                    headers.setUser(tokens.authenticate(bearer.substring(7)));
                }catch(RuntimeException e){throw new MessagingException("Invalid authentication");}
            }
            requireAuthenticated(headers);
            if(headers.getSessionId()!=null)sessions.put(headers.getSessionId(),(Authentication)headers.getUser());
        }
        if(StompCommand.SUBSCRIBE.equals(headers.getCommand()))authorize(headers);
        if(StompCommand.DISCONNECT.equals(headers.getCommand())&&headers.getSessionId()!=null)sessions.remove(headers.getSessionId());
        return message;
    }
    @EventListener public void disconnected(SessionDisconnectEvent event){sessions.remove(event.getSessionId());}

    /** Already-subscribed clients must stop receiving data after expiry, logout or membership revocation. */
    public Message<?> preSendOutbound(Message<?> message){
        if(SimpMessageHeaderAccessor.getMessageType(message.getHeaders())!=SimpMessageType.MESSAGE)return message;
        String sessionId=SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        Authentication auth=sessionId==null?null:sessions.get(sessionId);
        if(auth==null)return null;
        var headers=StompHeaderAccessor.wrap(message);headers.setUser(auth);
        try{authorize(headers);return message;}
        catch(RuntimeException rejected){return null;}
    }
    private Object requireAuthenticated(StompHeaderAccessor headers){
        if(!(headers.getUser() instanceof Authentication auth)||!auth.isAuthenticated()
                ||!(auth.getPrincipal() instanceof User||auth.getPrincipal() instanceof ViewerPrincipal))
            throw new MessagingException("Authentication required");
        if(auth.getDetails() instanceof TokenAuthenticationService.Session session
                &&!session.expiresAt().isAfter(Instant.now()))throw new MessagingException("Session expired");
        if(auth.getPrincipal() instanceof User old){
            User current=users.findById(old.getId()).orElseThrow(()->new MessagingException("Authentication required"));
            if(!Boolean.TRUE.equals(current.getAccountEnabled())||!Objects.equals(current.getCredentialVersion(),old.getCredentialVersion()))
                throw new MessagingException("Session expired");
            return current;
        }
        return auth.getPrincipal();
    }
    private void authorize(StompHeaderAccessor headers){
        Object principal=requireAuthenticated(headers);
        var matcher=TOPIC.matcher(Objects.toString(headers.getDestination(),""));
        if(!matcher.matches())throw new MessagingException("Subscription destination is not allowed");
        UUID cid=UUID.fromString(matcher.group(1));
        if(matcher.group(2)!=null&&!auctions.existsByIdAndChampionshipId(UUID.fromString(matcher.group(2)),cid))
            throw new MessagingException("Auction does not belong to championship");
        boolean allowed=principal instanceof ViewerPrincipal viewer&&cid.equals(viewer.championshipId());
        if(principal instanceof User user)allowed=user.getRole()==Role.SUPER_ADMIN||roles.findByUserIdAndChampionshipId(user.getId(),cid).isPresent();
        if(!allowed)throw new MessagingException("Not authorized for championship topic");
    }
}
