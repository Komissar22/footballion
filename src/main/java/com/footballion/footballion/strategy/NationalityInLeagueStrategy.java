package com.footballion.footballion.strategy;

import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.entity.Club;
import com.footballion.footballion.entity.Player;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.util.LeagueUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class NationalityInLeagueStrategy implements QuestionStrategy {

    private final Random random = new Random();
    private List<String> cachedValidCombos = null;

    @Override
    public String getType() {
        return "NATIONALITY_IN_LEAGUE";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        List<String> validCombos = getOrBuildValidCombos(clubRepo, playerRepo);
        if (validCombos.isEmpty()) {
            return null;
        }

        String combo = validCombos.get(random.nextInt(validCombos.size()));
        String[] parts = combo.split("\\|");
        String country = parts[0];
        String leagueId = parts[1];
        String leagueName = LeagueUtils.getLeagueName(leagueId);

        String questionText = "Name a current or past player from " + country + " in " + leagueName;
        return new QuestionDto(questionText, getType(), combo);
    }

    private synchronized List<String> getOrBuildValidCombos(ClubRepository clubRepo, PlayerRepository playerRepo) {
        if (cachedValidCombos != null && !cachedValidCombos.isEmpty()) {
            return cachedValidCombos;
        }

        Map<Integer, String> clubToLeague = new HashMap<>();
        for (Club club : clubRepo.findAll()) {
            if (LeagueUtils.isTop5League(club.getDomesticCompetitionId())) {
                clubToLeague.put(club.getClubId(), club.getDomesticCompetitionId().toUpperCase());
            }
        }

        List<Player> top5Players = playerRepo.findByCurrentClubIdIn(clubToLeague.keySet());
        Map<String, Integer> activeComboCounts = new HashMap<>();

        for (Player p : top5Players) {
            String country = p.getCountryOfCitizenship();
            if (!LeagueUtils.isTop50Nation(country)) {
                continue;
            }
            // סופרים רק שחקנים פעילים בשנים האחרונות כדי לוודא שיש מספיק שחקנים מוכרים בליגה הזו היום
            int lastSeason = p.getLastSeason() != null ? p.getLastSeason() : 2020;
            if (lastSeason < 2024) {
                continue;
            }

            String leagueId = clubToLeague.get(p.getCurrentClubId());
            if (leagueId != null) {
                String key = country + "|" + leagueId;
                activeComboCounts.merge(key, 1, Integer::sum);
            }
        }

        List<String> combos = activeComboCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 6)
                .map(Map.Entry::getKey)
                .toList();

        cachedValidCombos = combos;
        return cachedValidCombos;
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo,
                                  ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        if (guessedId == null || targetValue == null || !targetValue.contains("|")) {
            return false;
        }
        String[] parts = targetValue.split("\\|");
        if (parts.length < 2) {
            return false;
        }
        String targetCountry = parts[0];
        String targetLeagueId = parts[1];

        Optional<Player> playerOpt = playerRepo.findById(guessedId);
        if (playerOpt.isEmpty()) {
            return false;
        }

        Player player = playerOpt.get();
        if (!targetCountry.equalsIgnoreCase(player.getCountryOfCitizenship())) {
            return false;
        }

        if (player.getCurrentClubId() != null) {
            Optional<Club> clubOpt = clubRepo.findById(player.getCurrentClubId());
            if (clubOpt.isPresent() && targetLeagueId.equalsIgnoreCase(clubOpt.get().getDomesticCompetitionId())) {
                return true;
            }
        }

        // תמיכה גם בשחקני עבר ששיחקו בליגה הזו בעבר ועברו מאז לליגה אחרת
        List<Club> leagueClubs = clubRepo.findByDomesticCompetitionIdIgnoreCase(targetLeagueId);
        for (Club c : leagueClubs) {
            if (appearanceRepo.existsByPlayerIdAndPlayerClubIdAndCompetitionId(guessedId, c.getClubId(), "CLUB_HISTORY")) {
                return true;
            }
        }
        return false;
    }
}
