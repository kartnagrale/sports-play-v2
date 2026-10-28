package com.neml.badminton.controller;

import com.neml.badminton.config.SecurityConfig;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.security.*;
import com.neml.badminton.service.ChampionshipService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=ChampionshipController.class,properties="app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class,JwtAuthFilter.class,ChampionshipSecurity.class,TokenAuthenticationService.class,com.neml.badminton.config.AllowedOrigins.class})
class ChampionshipPrincipalTest {
    @Autowired MockMvc mvc;
    @MockBean ChampionshipService service;
    @MockBean UserRepository users;
    @MockBean ChampionshipRoleRepository roles;
    @MockBean JwtService jwt;
    UsernamePasswordAuthenticationToken auth(Object principal,String role){
        return new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+role)));
    }
    @Test void staffListsStillWork() throws Exception {
        for(Role role:List.of(Role.SUPER_ADMIN,Role.USER)){
            var user=User.builder().id(UUID.randomUUID()).role(role).build();
            when(service.listFor(user)).thenReturn(List.of());
            mvc.perform(get("/api/championships").with(authentication(auth(user,role.name())))).andExpect(status().isOk()).andExpect(content().json("[]"));
        }
    }
    @Test void viewerListIsControlled403WithoutCallingStaffService() throws Exception {
        mvc.perform(get("/api/championships").with(authentication(auth(new ViewerPrincipal(UUID.randomUUID()),"VIEWER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void anonymousAndInvalidTokensCannotList() throws Exception {
        mvc.perform(get("/api/championships")).andExpect(status().isUnauthorized());
        for(String token:List.of("invalid","expired")){
            when(jwt.parse(token)).thenThrow(new JwtException("Invalid token"));
            mvc.perform(get("/api/championships").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }
    @Test void viewerOwnRoomWorksButForeignRoomIsDenied() throws Exception {
        var cid=UUID.randomUUID(); var auth=auth(new ViewerPrincipal(cid),"VIEWER");
        mvc.perform(get("/api/championships/"+cid).with(authentication(auth))).andExpect(status().isOk());
        var foreign=UUID.randomUUID();
        mvc.perform(get("/api/championships/"+foreign).with(authentication(auth))).andExpect(status().isForbidden());
        verify(service,never()).get(foreign);
    }
}
