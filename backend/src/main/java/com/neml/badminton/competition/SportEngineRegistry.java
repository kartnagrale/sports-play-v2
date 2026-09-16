package com.neml.badminton.competition;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Component
public class SportEngineRegistry {
    private final Map<String, SportCompetitionEngine> engines;

    public SportEngineRegistry(List<SportCompetitionEngine> engineList) {
        Map<String, SportCompetitionEngine> registered = new HashMap<>();
        for (SportCompetitionEngine engine : engineList) {
            String code = normalize(engine.sportCode());
            if (registered.putIfAbsent(code, engine) != null) {
                throw new IllegalStateException("Duplicate sport engine: " + code);
            }
        }
        this.engines = Map.copyOf(registered);
    }

    public SportCompetitionEngine require(String sportType) {
        SportCompetitionEngine engine = engines.get(normalize(sportType));
        if (engine == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Competition management is not yet supported for sport " + sportType);
        }
        return engine;
    }

    public boolean supports(String sportType) {
        return engines.containsKey(normalize(sportType));
    }

    public static String normalize(String sportType) {
        if (sportType == null) return "";
        return sportType.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
