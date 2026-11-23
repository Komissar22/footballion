package com.footballion.footballion.controller;

import com.footballion.footballion.entity.Club;
import com.footballion.footballion.entity.Player;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.util.TextNormalizer;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final PlayerRepository playerRepo;
    private final ClubRepository clubRepo;

    public SearchController(PlayerRepository playerRepo, ClubRepository clubRepo) {
        this.playerRepo = playerRepo;
        this.clubRepo = clubRepo;
    }

    @GetMapping
    public List<SearchResultDto> search(@RequestParam String query) {
        List<SearchResultDto> results = new ArrayList<>();
        
        String normalizedQuery = TextNormalizer.normalize(query);
        if (normalizedQuery.length() < 2) {
            return results;
        }

        List<Player> players = playerRepo.findTop10BySearchNameContainingIgnoreCaseOrderByHighestMarketValueInEurDesc(normalizedQuery);
        if (players.isEmpty()) {
            players = playerRepo.findTop10ByNameContainingIgnoreCase(query);
        }
        for (Player p : players) {
            results.add(new SearchResultDto(p.getPlayerId(), p.getName(), "Player"));
        }

        List<Club> clubs = clubRepo.findTop10BySearchNameContainingIgnoreCase(normalizedQuery);
        if (clubs.isEmpty()) {
            clubs = clubRepo.findTop10ByNameContainingIgnoreCase(query);
        }
        for (Club c : clubs) {
            results.add(new SearchResultDto(c.getClubId(), c.getName(), "Club"));
        }

        return results;
    }
}

class SearchResultDto {
    private Integer id;
    private String name;
    private String type;

    public SearchResultDto(Integer id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
}