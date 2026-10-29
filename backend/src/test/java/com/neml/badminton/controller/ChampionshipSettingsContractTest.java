package com.neml.badminton.controller;

import com.neml.badminton.config.JacksonConfig;
import com.neml.badminton.dto.ChampionshipDtos.ConfigureChampionshipRequest;
import com.neml.badminton.service.ChampionshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChampionshipSettingsContractTest {
    ChampionshipService service=mock(ChampionshipService.class);
    MockMvc mvc;
    String path="/api/championships/"+UUID.randomUUID()+"/settings";
    String valid="{\"auctionName\":\"NPL\",\"maxSquadSize\":11,\"purseLimit\":100000,\"minMale\":0,\"minFemale\":3,\"playerBasePrice\":5000,\"bidIncrement\":1000,\"timerSeconds\":30,\"customRules\":\"\",\"expectedVersion\":1}";
    @BeforeEach void setup(){mvc=MockMvcBuilders.standaloneSetup(new ChampionshipController(service))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new JacksonConfig().objectMapper()))
            .setControllerAdvice(new ApiExceptionHandler()).build();}
    @Test void legitimateRupeeAmountsReachServiceWithoutConversion() throws Exception {
        mvc.perform(put(path).contentType("application/json").content(valid)).andExpect(status().isOk());
        verify(service).configure(any(),argThat(r->r.purseLimit().equals(new BigDecimal("100000"))
                &&r.playerBasePrice().equals(new BigDecimal("5000"))&&r.bidIncrement().equals(new BigDecimal("1000"))&&r.minMale()==0));
    }
    @Test void originalResponseEchoIsRejectedWithSafeActionableField() throws Exception {
        mvc.perform(put(path).contentType("application/json").content(valid.replace("{","{\"championshipId\":\"private-value-must-not-leak\",")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid field 'championshipId': check its name and value type"));
        verifyNoInteractions(service);
    }
    @Test void badNumericTypeNamesFieldWithoutDisclosingInput() throws Exception {
        mvc.perform(put(path).contentType("application/json").content(valid.replace("100000","\"confidential\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid field 'purseLimit': check its name and value type"));
    }
    @Test void nullRequiredMoneyAndNegativeQuotaAreRejected() throws Exception {
        mvc.perform(put(path).contentType("application/json").content(valid.replace("100000","null"))).andExpect(status().isBadRequest());
        mvc.perform(put(path).contentType("application/json").content(valid.replace("\"minMale\":0","\"minMale\":-1"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
