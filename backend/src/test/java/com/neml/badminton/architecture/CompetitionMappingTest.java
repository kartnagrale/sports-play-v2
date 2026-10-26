package com.neml.badminton.architecture;

import com.neml.badminton.entity.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class CompetitionMappingTest {
    @Test
    void compositeTenantAssociationsProduceValidHibernateMetadata() {
        var registry=new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect","org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access","false").build();
        try {
            MetadataSources sources=new MetadataSources(registry);
            for(Class<?> type: new Class<?>[]{User.class,Championship.class,Auction.class,Team.class,Player.class,
                    Match.class,MatchFormat.class,CompetitionConfiguration.class,CompetitionStage.class,
                    CompetitionQualification.class,CompetitionAuditEvent.class,ConfigurationSectionState.class,
                    ChampionshipBrandingConfiguration.class,AuctionConfiguration.class,RosterQuotaRule.class,
                    BadmintonConfiguration.class,BadmintonRubberRule.class,CricketConfiguration.class,
                    CricketMatchState.class,CricketPlayingMember.class,CricketInnings.class,CricketDelivery.class}) {
                sources.addAnnotatedClass(type);
            }
            assertThatCode(()->((MetadataImplementor)sources.buildMetadata()).validate()).doesNotThrowAnyException();
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
