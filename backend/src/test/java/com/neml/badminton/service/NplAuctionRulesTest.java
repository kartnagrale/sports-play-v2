package com.neml.badminton.service;
import com.neml.badminton.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class NplAuctionRulesTest {
    AuctionBidRules.Rules rules=new AuctionBidRules.Rules(11,11,0,3,money(5000),money(15000),4);
    BigDecimal money(int n){return BigDecimal.valueOf(n);}
    String check(int male,int female,int premium,Gender gender,int amount,int purse){return AuctionBidRules.unavailable(rules,male,female,premium,gender,money(amount),money(purse));}
    @Test void maximumPriceIsAllowedForTheFourthCommittedPremiumPurchase(){assertThat(check(4,0,3,Gender.MALE,15000,55000)).isNull();}
    @ParameterizedTest @ValueSource(ints={15001,16000}) void ceilingIsAuthoritative(int bid){assertThat(check(1,0,0,Gender.MALE,bid,100000)).contains("maximum");}
    @Test void fifthPremiumIsBlockedButCheaperPurchaseStillFits(){
        assertThat(check(5,0,4,Gender.FEMALE,15000,40000)).contains("Premium");
        assertThat(check(5,0,4,Gender.FEMALE,5000,40000)).isNull();
    }
    @Test void captainConsumesOneSlotAndTenPurchasesFillTheTeam(){assertThat(check(8,3,4,Gender.MALE,5000,10000)).contains("full");}
    @Test void reserveUsesTheTenRemainingPurchasesAfterCaptainAssignment(){
        // First purchase reserves 9 x 5,000; a purse of 59,999 cannot fund a 15,000 bid.
        assertThat(check(1,0,0,Gender.MALE,15000,59999)).contains("45000");
        assertThat(check(1,0,0,Gender.MALE,15000,60000)).isNull();
    }
    @Test void fourPremiumAndSixMinimumPurchasesLeaveTenThousand(){
        int male=1,female=0,purse=100000,premium=0;
        for(int i=0;i<10;i++){
            int amount=i<4?15000:5000;Gender gender=i>=7?Gender.FEMALE:Gender.MALE;
            assertThat(check(male,female,premium,gender,amount,purse)).isNull();
            purse-=amount;if(amount==15000)premium++;if(gender==Gender.FEMALE)female++;else male++;
        }
        assertThat(purse).isEqualTo(10000);assertThat(male+female).isEqualTo(11);assertThat(female).isEqualTo(3);
    }
    @Test void femaleCaptainCountsAndZeroMaleMinimumAllowsAdditionalWomen(){
        assertThat(check(7,3,0,Gender.FEMALE,5000,20000)).isNull();
        assertThat(check(0,10,0,Gender.FEMALE,5000,20000)).isNull();
    }
    @Test void purchaseCannotConsumeTheLastRequiredFemaleSlot(){
        assertThat(check(8,2,0,Gender.MALE,5000,20000)).contains("gender");
        assertThat(check(8,2,0,Gender.FEMALE,5000,20000)).isNull();
    }
    @Test void reserveUsesRequiredRosterRatherThanOptionalMaximum(){
        var flexible=new AuctionBidRules.Rules(5,7,0,0,money(5000),null,null);
        assertThat(AuctionBidRules.unavailable(flexible,4,0,0,Gender.FEMALE,money(20000),money(20000))).isNull();
    }
    @Test void preassignedIdentityIsEligibleWithoutSale(){
        Player p=Player.builder().status(PlayerStatus.PREASSIGNED).captainUserId(java.util.UUID.randomUUID()).team(new Team()).build();
        assertThat(p.isRosterEligible()).isTrue();assertThat(p.getSoldPrice()).isNull();assertThat(p.getAuction()).isNull();
        p.setSoldPrice(money(0));assertThat(p.isRosterEligible()).isFalse();
    }
}
