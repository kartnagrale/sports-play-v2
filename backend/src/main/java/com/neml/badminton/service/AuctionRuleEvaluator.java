package com.neml.badminton.service;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class AuctionRuleEvaluator {
    public record TeamEligibility(UUID teamId,int premiumPurchases,Integer premiumLimit,int squadSlotsRemaining,
            int femalePlayers,int minimumFemale,int malePlayers,int minimumMale,BigDecimal maximumAffordableBid,String unavailableReason) {}
    private final PlayerRepository players;
    private final TournamentSettingsRepository settings;
    private final AuctionConfigurationRepository policies;
    private final CompetitionConfigurationRepository competitions;
    public AuctionRuleEvaluator(PlayerRepository players,TournamentSettingsRepository settings,AuctionConfigurationRepository policies,CompetitionConfigurationRepository competitions){
        this.players=players;this.settings=settings;this.policies=policies;this.competitions=competitions;
    }
    public void assertReady(UUID cid,List<Team> teams) {
        CompetitionConfiguration c=competitions.findByChampionshipId(cid).orElseThrow();
        if(teams.size()!=c.getConfiguredTeamCount()||teams.size()<c.getMinimumTeamCount()||teams.size()>c.getMaximumTeamCount())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Exactly "+c.getConfiguredTeamCount()+" teams required. Currently "+teams.size()+"/"+c.getConfiguredTeamCount());
        if(Boolean.TRUE.equals(policies.findFirstByChampionshipIdOrderByConfigurationVersionDesc(cid).orElseThrow().getCaptainPreassignmentRequired()))
            for(Team team:teams)if(players.findAllByChampionshipIdAndTeamIdOrderByAuctionOrderAsc(cid,team.getId()).stream().noneMatch(p->p.getStatus()==PlayerStatus.PREASSIGNED&&p.isRosterEligible()))
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,team.getName()+": captain must be assigned to the team roster");
    }
    public AuctionBidRules.Rules rules(UUID cid){
        TournamentSettings s=settings.findByChampionshipId(cid).orElseThrow();
        AuctionConfiguration p=policies.findFirstByChampionshipIdOrderByConfigurationVersionDesc(cid).orElseThrow();
        int required=competitions.findByChampionshipId(cid).map(CompetitionConfiguration::getMinimumRosterSize).orElse(s.getMaxSquadSize());
        return new AuctionBidRules.Rules(required,s.getMaxSquadSize(),s.getMinMale(),s.getMinFemale(),s.getPlayerBasePrice(),p.getMaximumBid(),p.getMaximumPremiumPurchases());
    }
    public TeamEligibility evaluate(UUID cid,Team team,Player candidate,BigDecimal amount){
        var r=rules(cid);
        List<Player> roster=players.findAllByChampionshipIdAndTeamIdOrderByAuctionOrderAsc(cid,team.getId());
        int male=(int)roster.stream().filter(p->p.getGender()==Gender.MALE).count();
        int female=(int)roster.stream().filter(p->p.getGender()==Gender.FEMALE).count();
        int premium=(int)roster.stream().filter(p->p.getStatus()==PlayerStatus.SOLD&&p.getSoldPrice()!=null&&r.maximumBid()!=null&&p.getSoldPrice().compareTo(r.maximumBid())==0).count();
        BigDecimal affordable=team.getPurseRemaining().subtract(r.minimumBid().multiply(BigDecimal.valueOf(Math.max(0,r.requiredPlayers()-roster.size()-1))));
        if(r.maximumBid()!=null)affordable=affordable.min(r.maximumBid());
        if(r.maximumBid()!=null&&r.maximumPremiumPurchases()!=null&&premium>=r.maximumPremiumPurchases())affordable=affordable.min(r.maximumBid().subtract(new BigDecimal("0.01")));
        String reason=candidate==null?"No active player":AuctionBidRules.unavailable(r,male,female,premium,candidate.getGender(),amount,team.getPurseRemaining());
        return new TeamEligibility(team.getId(),premium,r.maximumPremiumPurchases(),Math.max(0,r.maximumPlayers()-roster.size()),female,r.minimumFemale(),male,r.minimumMale(),affordable.max(BigDecimal.ZERO),reason);
    }
}
