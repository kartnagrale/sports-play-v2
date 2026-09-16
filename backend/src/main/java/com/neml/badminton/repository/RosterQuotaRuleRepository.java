package com.neml.badminton.repository;
import com.neml.badminton.entity.RosterQuotaRule;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
public interface RosterQuotaRuleRepository extends JpaRepository<RosterQuotaRule,UUID>{
 @Query("select r from RosterQuotaRule r where r.championship.id=:cid and r.configurationVersion=(select max(x.configurationVersion) from RosterQuotaRule x where x.championship.id=:cid) and r.enabled=true and r.ruleCode <> '__EMPTY__' order by r.ruleCode")List<RosterQuotaRule> findAllByChampionshipIdAndEnabledTrue(@Param("cid")UUID championshipId);
 @Query("select r from RosterQuotaRule r where r.championship.id=:cid and r.configurationVersion=(select max(x.configurationVersion) from RosterQuotaRule x where x.championship.id=:cid) and r.ruleCode <> '__EMPTY__' order by r.ruleCode")List<RosterQuotaRule> findAllByChampionshipIdOrderByRuleCode(@Param("cid")UUID championshipId);
 @Query("select coalesce(max(r.configurationVersion),0) from RosterQuotaRule r where r.championship.id=:cid")int findLatestConfigurationVersion(@Param("cid")UUID championshipId);
}
