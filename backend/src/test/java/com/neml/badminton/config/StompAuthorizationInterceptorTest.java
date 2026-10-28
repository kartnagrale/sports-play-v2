package com.neml.badminton.config;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StompAuthorizationInterceptorTest {
    final TokenAuthenticationService tokens=mock(TokenAuthenticationService.class);
    final UserRepository users=mock(UserRepository.class);
    final ChampionshipRoleRepository roles=mock(ChampionshipRoleRepository.class);
    final AuctionRepository auctions=mock(AuctionRepository.class);
    final StompAuthorizationInterceptor interceptor=new StompAuthorizationInterceptor(tokens,users,roles,auctions);
    final UUID cid=UUID.randomUUID();

    Message<?> frame(StompCommand command, String destination, Object principal) {
        var headers=StompHeaderAccessor.create(command);
        if(destination!=null)headers.setDestination(destination);
        if(principal!=null)headers.setUser(new UsernamePasswordAuthenticationToken(principal,null,List.of()));
        return MessageBuilder.createMessage(new byte[0],headers.getMessageHeaders());
    }
    @Test void anonymousConnectIsRejected() {
        assertThatThrownBy(()->interceptor.preSend(frame(StompCommand.CONNECT,null,null),null)).isInstanceOf(MessagingException.class);
    }
    @Test void everyClientSendIsDeniedRegardlessOfIdentityOrDestination() {
        for(Object principal:Arrays.asList(null,new ViewerPrincipal(cid),User.builder().role(Role.SUPER_ADMIN).build()))
            for(String destination:List.of("/topic/championship/"+cid+"/matches","/queue/events","/app/score","/other"))
                assertThatThrownBy(()->interceptor.preSend(frame(StompCommand.SEND,destination,principal),null))
                        .isInstanceOf(MessagingException.class).hasMessage("Client publication is not allowed");
        verifyNoInteractions(users,roles,auctions);
    }
    @Test void cookieAuthenticatedStaffConnectIsPreserved() {
        var user=User.builder().id(UUID.randomUUID()).role(Role.USER).build();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        var message=frame(StompCommand.CONNECT,null,user);
        assertThat(interceptor.preSend(message,null)).isSameAs(message);
    }
    @Test void viewerMaySubscribeOnlyToItsOwnExactTopic() {
        var viewer=new ViewerPrincipal(cid);
        var message=frame(StompCommand.SUBSCRIBE,"/topic/championship/"+cid+"/matches",viewer);
        assertThat(interceptor.preSend(message,null)).isSameAs(message);
        for(String destination:List.of("/topic/championship/"+UUID.randomUUID()+"/matches","/topic/**","/queue/events"))
            assertThatThrownBy(()->interceptor.preSend(frame(StompCommand.SUBSCRIBE,destination,viewer),null)).isInstanceOf(MessagingException.class);
    }
    @Test void auctionMustBelongToChampionshipAndStaffMustHaveMembership() {
        var user=User.builder().id(UUID.randomUUID()).role(Role.USER).build();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        var aid=UUID.randomUUID();
        var message=frame(StompCommand.SUBSCRIBE,"/topic/championship/"+cid+"/auction/"+aid,user);
        assertThatThrownBy(()->interceptor.preSend(message,null)).isInstanceOf(MessagingException.class);
        when(auctions.existsByIdAndChampionshipId(aid,cid)).thenReturn(true);
        assertThatThrownBy(()->interceptor.preSend(message,null)).isInstanceOf(MessagingException.class);
        when(roles.findByUserIdAndChampionshipId(user.getId(),cid)).thenReturn(Optional.of(new ChampionshipRole()));
        assertThat(interceptor.preSend(message,null)).isSameAs(message);
    }
    @Test void outboundEventsStopAfterExpiryAndLogout() {
        var user=User.builder().id(UUID.randomUUID()).role(Role.SUPER_ADMIN).build();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        var auth=new UsernamePasswordAuthenticationToken(user,null,List.of());
        auth.setDetails(new TokenAuthenticationService.Session(java.time.Instant.now().plusSeconds(60),0));
        var connect=StompHeaderAccessor.create(StompCommand.CONNECT);connect.setSessionId("session");connect.setUser(auth);
        interceptor.preSend(MessageBuilder.createMessage(new byte[0],connect.getMessageHeaders()),null);
        var event=StompHeaderAccessor.create(StompCommand.MESSAGE);event.setSessionId("session");
        event.setDestination("/topic/championship/"+cid+"/matches");
        var message=MessageBuilder.createMessage(new byte[0],event.getMessageHeaders());
        assertThat(interceptor.preSendOutbound(message)).isSameAs(message);
        auth.setDetails(new TokenAuthenticationService.Session(java.time.Instant.now().minusSeconds(1),0));
        assertThat(interceptor.preSendOutbound(message)).isNull();
        auth.setDetails(new TokenAuthenticationService.Session(java.time.Instant.now().plusSeconds(60),0));
        when(users.findById(user.getId())).thenReturn(Optional.of(User.builder().id(user.getId()).role(Role.SUPER_ADMIN).credentialVersion(1).build()));
        assertThat(interceptor.preSendOutbound(message)).isNull();
    }
}
