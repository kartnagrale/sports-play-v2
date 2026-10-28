package com.neml.badminton.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neml.badminton.config.*;
import com.neml.badminton.dto.AuthDtos;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.UserRepository;
import com.neml.badminton.security.*;
import com.neml.badminton.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=AuthController.class,properties={"app.cors.allowed-origins=https://client.example","app.auth.cookie-secure=true"})
@Import({SecurityConfig.class,AllowedOrigins.class,JwtAuthFilter.class,TokenAuthenticationService.class})
class BrowserAuthSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean AuthService auth;
    @MockitoBean AccountActivationService activations;
    @MockitoBean UserRepository users;
    @MockitoBean JwtService jwt;
    @Test void loginRequiresCsrfEvenBeforeAuthentication() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"user@example.com\",\"password\":\"long-test-password\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(auth);
    }
    @Test void csrfCookieAndHeaderPermitLoginWithHardenedCookieWithoutJsonToken() throws Exception {
        var result=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var cookie=result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();assertThat(cookie.isHttpOnly()).isTrue();assertThat(cookie.getSecure()).isTrue();
        String token=json.readTree(result.getResponse().getContentAsString()).get("token").asText();
        var user=new AuthDtos.UserInfo(UUID.randomUUID().toString(),"user@example.com","Test",Role.USER,List.of());
        when(auth.login(any())).thenReturn(new AuthDtos.AuthResponse("sensitive-jwt",user));
        mvc.perform(post("/api/auth/login").cookie(cookie).header("X-XSRF-TOKEN",token)
                .contentType("application/json").content("{\"email\":\"user@example.com\",\"password\":\"long-test-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("HttpOnly"),org.hamcrest.Matchers.containsString("Secure"),org.hamcrest.Matchers.containsString("SameSite=Lax"))));
    }
    @Test void wrongCsrfHeaderIsDeniedAndForeignCorsOriginIsDenied() throws Exception {
        var response=mvc.perform(get("/api/auth/csrf")).andReturn().getResponse();
        mvc.perform(post("/api/auth/logout").cookie(response.getCookie("XSRF-TOKEN")).header("X-XSRF-TOKEN","forged"))
                .andExpect(status().isForbidden());
        mvc.perform(options("/api/auth/login").header("Origin","https://foreign.example")
                .header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(options("/api/auth/login").header("Origin","https://client.example")
                .header("Access-Control-Request-Method","POST")).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin","https://client.example"));
    }
    @Test void invalidOriginConfigurationFailsClosed(){
        for(String value:List.of("*","https://*.example.com","https://example.com/path","null","https://user:pass@example.com"))
            assertThatThrownBy(()->new AllowedOrigins(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
