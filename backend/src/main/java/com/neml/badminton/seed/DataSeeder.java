package com.neml.badminton.seed;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final AuctionStateRepository auctionStateRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auction.base-price}")
    private long basePrice;

    @Value("${app.auction.team-purse}")
    private long teamPurse;

    public DataSeeder(UserRepository userRepository, TeamRepository teamRepository,
                      PlayerRepository playerRepository, AuctionStateRepository auctionStateRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.auctionStateRepository = auctionStateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Already seeded (idempotent)
        }
        
        // Admin
        userRepository.save(User.builder()
                .email("admin@neml.com")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .fullName("Tournament Admin")
                .role(Role.ADMIN)
                .build());

        if (auctionStateRepository.count() == 0) {
            auctionStateRepository.save(AuctionState.builder()
                    .status(AuctionStatus.NOT_STARTED)
                    .timerSeconds(30)
                    .build());
        }
    }
}
