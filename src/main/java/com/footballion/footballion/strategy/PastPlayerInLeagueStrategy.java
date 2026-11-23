package com.footballion.footballion.strategy;

import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.entity.Club;
import com.footballion.footballion.entity.Player;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.util.LeagueUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
public class PastPlayerInLeagueStrategy implements QuestionStrategy {

    private final Random random = new Random();

    @Override
    public String getType() {
        return "PAST_PLAYER_IN_LEAGUE";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        List<String> top5Leagues = new ArrayList<>(LeagueUtils.TOP_5_LEAGUES);
        String leagueId = top5Leagues.get(random.nextInt(top5Leagues.size()));
        String leagueName = LeagueUtils.getLeagueName(leagueId);

        String questionText = "Name a past player who played in " + leagueName + " (no longer playing in this league)";
        return new QuestionDto(questionText, getType(), leagueId);
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo,
                                  ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        if (guessedId == null || targetValue == null) {
            return false;
        }

        Optional<Player> playerOpt = playerRepo.findById(guessedId);
        if (playerOpt.isEmpty()) {
            return false;
        }
        Player player = playerOpt.get();

        List<Integer> leagueClubIds = clubRepo.findByDomesticCompetitionIdIgnoreCase(targetValue)
                .stream()
                .map(Club::getClubId)
                .toList();

        boolean isActiveNow = player.getLastSeason() != null && player.getLastSeason() >= 2024;
        boolean currentlyInThisLeague = isActiveNow
                && player.getCurrentClubId() != null
                && leagueClubIds.contains(player.getCurrentClubId());

        // אם השחקן עדיין פעיל היום בליגה הזו, הוא אינו "שחקן עבר של הליגה"
        if (currentlyInThisLeague) {
            return false;
        }

        // בדיקה 1: הקבוצה האחרונה שלו בקריירה הייתה בליגה הזו והוא כבר לא פעיל (lastSeason < 2024)
        if (!isActiveNow && player.getCurrentClubId() != null && leagueClubIds.contains(player.getCurrentClubId())) {
            return true;
        }

        // בדיקה 2: הוא שיחק בעבר במועדון מהליגה הזו (CLUB_HISTORY) וכיום הוא בליגה אחרת או פרש
        for (Integer clubId : leagueClubIds) {
            if (appearanceRepo.existsByPlayerIdAndPlayerClubIdAndCompetitionId(guessedId, clubId, "CLUB_HISTORY")) {
                return true;
            }
        }

        return false;
    }
}
