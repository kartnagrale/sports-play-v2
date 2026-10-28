package com.neml.badminton.config;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthRateLimitFilterTest {
    @Test void excessiveAttemptsAreBoundedPerPeerAndDoNotReachAuthentication() throws Exception {
        var filter=new AuthRateLimitFilter(30);var chain=mock(FilterChain.class);
        for(int i=0;i<31;i++){
            var request=new MockHttpServletRequest("POST","/api/auth/login");request.setRemoteAddr("127.0.0.1");
            var response=new MockHttpServletResponse();filter.doFilter(request,response,chain);
            assertThat(response.getStatus()).isEqualTo(i<30?200:429);
            if(i==30)assertThat(response.getHeader("Retry-After")).isEqualTo("900");
        }
        var anotherPeer=new MockHttpServletRequest("POST","/api/auth/login");anotherPeer.setRemoteAddr("127.0.0.2");
        var response=new MockHttpServletResponse();filter.doFilter(anotherPeer,response,chain);
        assertThat(response.getStatus()).isEqualTo(200);verify(chain,times(31)).doFilter(any(),any());
    }
}
