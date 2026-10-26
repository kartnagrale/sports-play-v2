package com.neml.badminton.competition;

import com.neml.badminton.dto.MatchDtos.*;
import com.neml.badminton.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import com.neml.badminton.repository.*;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class CricketCompetitionEngine implements SportCompetitionEngine {
    private CricketInningsRepository innings;private CricketConfigurationRepository cricketConfigurations;
    public CricketCompetitionEngine(){}
    @Autowired public CricketCompetitionEngine(CricketInningsRepository innings,CricketConfigurationRepository cricketConfigurations){this.innings=innings;this.cricketConfigurations=cricketConfigurations;}
    @Override public String sportCode(){return "CRICKET";}
    @Override public CompetitionDefaults defaultConfiguration(){return new CompetitionDefaults(
            RoundRobinMode.SINGLE,2,0,"NET_RUN_RATE,MATCHES_WON,NAME",4,1,true,List.of("Main Ground"));}

    @Override public void validateConfiguration(CompetitionConfiguration c,int teamCount){
        List<String> errors=new ArrayList<>();
        if(c.getPointsPerWin()<0||c.getPointsPerLoss()<0||c.getPointsPerTie()<0||c.getPointsPerNoResult()<0)
            errors.add("League points cannot be negative");
        if(c.getFixtureSpacingDays()<0) errors.add("Fixture spacing cannot be negative");
        if(teamCount>0&&(c.getQualifyingTeamCount()>teamCount||c.getQualifyingTeamCount()<0))
            errors.add("Qualifying team count must be between zero and the number of teams");
        int required=switch(c.getPlayoffFormat()){case NONE->0;case TOP_TWO_FINAL->2;default->4;};
        if(c.getQualifyingTeamCount()!=required) errors.add(c.getPlayoffFormat()+" requires "+required+" qualifiers");
        if(errors.size()>0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,String.join("; ",errors));
    }
    @Override public List<RubberDefinition> fixtureComponents(){return List.of();}
    @Override public void validateLineup(FormatType ignored,List<Player> a,List<Player> b){bad("Cricket lineups use the playing-XI API");}
    @Override public void validateScore(MatchFormat ignored,int a,int b,boolean finalResult){bad("Cricket scores are calculated from deliveries");}
    @Override public MatchOutcome calculateMatchOutcome(Match fixture){
        return new MatchOutcome(fixture.getWinnerTeam(),0,0,fixture.getStatus()==MatchStatus.COMPLETED);
    }
    @Override public List<StandingDto> calculateStandings(CompetitionConfiguration c,TournamentSettings ignored,
            List<Team> teams,List<Match> fixtures,Map<UUID,List<UUID>> ignoredPlayers,boolean penalties){
        Map<UUID,int[]> stats=new HashMap<>(); teams.forEach(t->stats.put(t.getId(),new int[4]));
        Map<UUID,double[]> rate=new HashMap<>();teams.forEach(t->rate.put(t.getId(),new double[4]));
        for(Match m:fixtures){if(m.getStatus()!=MatchStatus.COMPLETED)continue; int[] a=stats.get(m.getTeamA().getId()),b=stats.get(m.getTeamB().getId());
            if(a==null||b==null)continue; a[0]++;b[0]++;a[3]+=n(m.getTeamAPoints());b[3]+=n(m.getTeamBPoints());
            if(m.getWinnerTeam()!=null&&m.getWinnerTeam().getId().equals(m.getTeamA().getId())){a[1]++;b[2]++;}
            else if(m.getWinnerTeam()!=null){b[1]++;a[2]++;}
            if(innings!=null){List<CricketInnings> played=innings.findAllByChampionshipIdAndMatchIdOrderByInningsNumber(c.getChampionship().getId(),m.getId());CricketConfiguration rules=cricketConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(c.getChampionship().getId()).orElse(null);if(rules!=null&&played.size()==2)for(CricketInnings inning:played){UUID batting=inning.getBattingTeam().getId();UUID bowling=m.getTeamA().getId().equals(batting)?m.getTeamB().getId():m.getTeamA().getId();int balls=inning.getWickets()>=rules.getPlayersPerSide()-1?rules.getOversPerInnings()*6:inning.getLegalBalls();rate.get(batting)[0]+=inning.getRuns();rate.get(batting)[1]+=balls;rate.get(bowling)[2]+=inning.getRuns();rate.get(bowling)[3]+=balls;}}}
        List<StandingDto> out=new ArrayList<>(); for(Team t:teams){int[] s=stats.get(t.getId());out.add(new StandingDto(
                TeamRef.from(t),s[0],s[1],s[2],0,0,0,s[3],0,s[3],nrr(rate.get(t.getId())),0,List.of()));}
        out.sort(Comparator.comparingInt(StandingDto::totalPoints).reversed().thenComparing(
                Comparator.comparingDouble((StandingDto s)->Optional.ofNullable(s.netRunRate()).orElse(0d)).reversed()).thenComparing(Comparator.comparingInt(StandingDto::won).reversed()).thenComparing(x->x.team().name()));
        List<StandingDto> ranked=new ArrayList<>(); for(int i=0;i<out.size();i++){StandingDto s=out.get(i);ranked.add(new StandingDto(
                s.team(),s.played(),s.won(),s.lost(),0,0,0,s.basePoints(),0,s.totalPoints(),s.netRunRate(),i+1,List.of()));} return ranked;
    }
    @Override public List<SeedPair> playoffPairings(int n){if(n==2)return List.of(new SeedPair(1,2));if(n==4)return List.of(new SeedPair(1,4),new SeedPair(2,3));bad("Cricket playoffs support two or four qualifiers");return List.of();}
    @Override public List<String> supportedWidgets(){return List.of("CRICKET_STANDINGS","NET_RUN_RATE","TOP_BATTERS","TOP_BOWLERS","UPCOMING_FIXTURES","PLAYOFF_BRACKET");}
    @Override public Map<String,String> terminology(){return Map.of("fixture","Match","segment","Innings","segments","Innings","score","Runs","standings","Points table");}
    @Override public Map<String,Object> dashboardMetrics(List<StandingDto> standings,List<Match> fixtures){return Map.of(
            "completedMatches",fixtures.stream().filter(m->m.getStatus()==MatchStatus.COMPLETED).count(),
            "leader",standings.isEmpty()?"":standings.get(0).team().name());}
    private int n(Integer v){return v==null?0:v;} private void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
    private double nrr(double[] r){if(r==null||r[1]==0||r[3]==0)return 0d;return Math.round(((r[0]*6d/r[1])-(r[2]*6d/r[3]))*1000d)/1000d;}
}
