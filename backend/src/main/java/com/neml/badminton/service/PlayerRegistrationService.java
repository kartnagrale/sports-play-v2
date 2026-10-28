package com.neml.badminton.service;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

/** Authoritative rules shared by manual creation and workbook confirmation. Callers never update by name. */
@Service
public class PlayerRegistrationService {
    public record Registration(String fullName, Gender gender, BigDecimal basePrice, String skillLevel,
                               Integer auctionOrder, String playerReference) {}
    private final ChampionshipRepository championships;
    private final AuctionRepository auctions;
    private final PlayerRepository players;
    private final TournamentSettingsRepository settings;
    private final SquadConfirmationService squads;
    public PlayerRegistrationService(ChampionshipRepository championships, AuctionRepository auctions,
            PlayerRepository players, TournamentSettingsRepository settings, SquadConfirmationService squads) {
        this.championships=championships;this.auctions=auctions;this.players=players;this.settings=settings;this.squads=squads;
    }
    public static String reference(String value) {
        return value==null||value.isBlank()?null:value.trim().toUpperCase(Locale.ROOT);
    }
    public static List<String> validate(Registration r) {
        List<String> errors=new ArrayList<>();
        if(r.fullName()==null||r.fullName().isBlank()||r.fullName().trim().length()>255) errors.add("Full Name is required (maximum 255 characters)");
        if(r.gender()==null) errors.add("Gender must be MALE or FEMALE");
        if(r.skillLevel()!=null&&r.skillLevel().length()>255) errors.add("Skill Level must be at most 255 characters");
        if(r.basePrice()!=null&&(r.basePrice().signum()<=0||r.basePrice().stripTrailingZeros().scale()>2||(long)r.basePrice().precision()-r.basePrice().scale()>17)) errors.add("Base Price must be positive, with at most 17 whole digits and 2 decimal places");
        if(r.auctionOrder()!=null&&r.auctionOrder()<1) errors.add("Auction Order must be a positive integer");
        if(reference(r.playerReference())!=null&&!reference(r.playerReference()).matches("[A-Z0-9][A-Z0-9_-]{0,63}")) errors.add("Player Reference must be 1–64 letters, digits, underscores or hyphens");
        return errors;
    }
    @Transactional
    public Championship lockMutable(UUID cid) {
        Championship c=championships.findByIdForUpdate(cid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Championship not found"));
        squads.assertRosterMutable(cid);
        if(c.getStatus()==ChampionshipStatus.COMPLETED||c.getStatus()==ChampionshipStatus.ARCHIVED) conflict("Player registration has closed for this championship");
        if(c.getLifecyclePhase().ordinal()>ChampionshipPhase.PLAYER_REGISTRATION.ordinal()) conflict("Player registration has closed for this championship");
        return c;
    }
    @Transactional
    public List<Player> register(UUID cid,List<Registration> rows) {
        Championship c=lockMutable(cid);
        Auction auction=auctions.findAllByChampionshipIdOrderByCreatedAtDesc(cid).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Configure the championship before adding players"));
        TournamentSettings config=settings.findByChampionshipId(cid).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Championship settings not found"));
        List<Player> existing=players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid);
        Set<Integer> orders=new HashSet<>(); Set<String> references=new HashSet<>();
        existing.forEach(p->{if(p.getAuction()!=null&&auction.getId().equals(p.getAuction().getId())&&p.getAuctionOrder()!=null) orders.add(p.getAuctionOrder());if(p.getPlayerReference()!=null)references.add(p.getPlayerReference());});
        // Reserve explicit orders first so auto assignment cannot collide with a later row.
        for(Registration r:rows) {
            List<String> errors=validate(r);if(!errors.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,String.join("; ",errors));
            if(r.auctionOrder()!=null&&!orders.add(r.auctionOrder())) conflict("Auction Order is already assigned");
            if(reference(r.playerReference())!=null&&!references.add(reference(r.playerReference()))) conflict("Player Reference is already registered");
        }
        long next=existing.stream().mapToLong(p->p.getAuctionOrder()==null?0:p.getAuctionOrder()).max().orElse(0)+1;
        List<Player> result=new ArrayList<>();
        for(Registration r:rows) {
            while(next<=Integer.MAX_VALUE&&orders.contains((int)next))next++;
            if(r.auctionOrder()==null&&next>Integer.MAX_VALUE) conflict("No auction orders remain available");
            int order=r.auctionOrder()==null?(int)next++:r.auctionOrder();orders.add(order);
            result.add(players.save(Player.builder().championship(c).auction(auction).fullName(r.fullName().trim()).gender(r.gender())
                    .basePrice(r.basePrice()==null?config.getPlayerBasePrice():r.basePrice()).skillLevel(r.skillLevel())
                    .auctionOrder(order).playerReference(reference(r.playerReference())).status(PlayerStatus.AVAILABLE).build()));
        }
        c.setLifecyclePhase(ChampionshipPhase.PLAYER_REGISTRATION);championships.save(c);players.flush();
        return result;
    }
    private static void conflict(String message){throw new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
