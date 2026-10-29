package com.neml.badminton.service;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

/** Every overlapping configuration writer takes the same championship lock first. */
@Service
public class AuctionPolicyGuard {
    private final ChampionshipRepository championships;
    private final AuctionRepository auctions;
    private final TeamRepository teams;
    private final BidRepository bids;
    public AuctionPolicyGuard(ChampionshipRepository championships, AuctionRepository auctions, TeamRepository teams, BidRepository bids) {
        this.championships=championships; this.auctions=auctions; this.teams=teams;this.bids=bids;
    }
    public Championship lockEditable(UUID cid) {
        Championship c=championships.findByIdForUpdate(cid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Championship not found"));
        if(bids.existsByChampionshipId(cid) || auctions.findAllByChampionshipIdOrderByCreatedAtDesc(cid).stream().anyMatch(a->a.getStatus()!=AuctionStatus.NOT_STARTED)
                || teams.findAllByChampionshipIdOrderByName(cid).stream().anyMatch(t->t.getPurseTotal().compareTo(t.getPurseRemaining())!=0))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Auction and roster rules are locked after the first auction action");
        return c;
    }
}
