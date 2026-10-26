package com.neml.badminton.service;

import com.neml.badminton.competition.SportEngineRegistry;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChampionshipConfigurationInitializationTest {
    @Mock CompetitionConfigurationRepository competitionConfigurations;
    @Mock ChampionshipBrandingConfigurationRepository brandingConfigurations;
    @Mock AuctionConfigurationRepository auctionConfigurations;
    @Mock CricketConfigurationRepository cricketConfigurations;
    @Mock BadmintonConfigurationRepository badmintonConfigurations;
    @Mock ConfigurationSectionService configurationSections;
    @Mock SportEngineRegistry sportEngines;
    @InjectMocks ChampionshipService service;

    @Test
    void demoCricketReceivesRecordsRequiredByDashboardAndAuction() {
        Championship championship = championship("CRICKET");
        service.initializeConfigurations(championship);

        verify(competitionConfigurations).save(argThat(config -> config.getChampionship() == championship
                && config.getSportCode().equals("CRICKET") && config.getPlayingSideSize() == 11));
        verify(configurationSections).initialize(championship);
        verify(brandingConfigurations).save(any());
        verify(auctionConfigurations).save(any());
        verify(cricketConfigurations).save(argThat(config -> config.getConfigurationVersion() == 1));
        verifyNoInteractions(badmintonConfigurations);
    }

    @Test
    void existingSettingsSurviveDemoStartup() {
        Championship championship = championship("CRICKET");
        UUID id = championship.getId();
        when(competitionConfigurations.findByChampionshipId(id)).thenReturn(Optional.of(new CompetitionConfiguration()));
        when(brandingConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(id))
                .thenReturn(Optional.of(new ChampionshipBrandingConfiguration()));
        when(auctionConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(id))
                .thenReturn(Optional.of(new AuctionConfiguration()));
        when(cricketConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(id))
                .thenReturn(Optional.of(new CricketConfiguration()));

        service.initializeConfigurations(championship);

        verify(competitionConfigurations, never()).save(any());
        verify(brandingConfigurations, never()).save(any());
        verify(auctionConfigurations, never()).save(any());
        verify(cricketConfigurations, never()).save(any());
        verify(configurationSections).initialize(championship);
    }

    @Test
    void badmintonInitializationIncludesItsFiveRubberRules() {
        Championship championship = championship("BADMINTON");
        service.initializeConfigurations(championship);
        verify(badmintonConfigurations).save(argThat(config -> config.getChampionship() == championship
                && config.getRubbers().size() == 5));
        verifyNoInteractions(cricketConfigurations);
    }

    private Championship championship(String sport) {
        return Championship.builder().id(UUID.randomUUID()).name("Merge Check").sportType(sport).isPublic(false).build();
    }
}
