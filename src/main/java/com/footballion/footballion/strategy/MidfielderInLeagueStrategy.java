package com.footballion.footballion.strategy;

import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.entity.Club;
import com.footballion.footballion.entity.Player;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.util.LeagueUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
public class MidfielderInLeagueStrategy implements QuestionStrategy {

    private final Random random = new Random();

    @Override
    public String getType() {
        return "MIDFIELDER_IN_LEAGUE";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        List<Club> clubs = clubRepo.findAll();
        if (clubs.isEmpty()) return null;

        Club randomClub;
        String leagueId;

        do {
            randomClub = clubs.get(random.nextInt(clubs.size()));
            leagueId = randomClub.getDomesticCompetitionId();
        } while (leagueId == null || leagueId.isBlank() || !LeagueUtils.isTop5League(leagueId));

        String leagueName = LeagueUtils.getLeagueName(leagueId);
        String questionText = "Name a current Midfielder in " + leagueName;
        return new QuestionDto(questionText, getType(), leagueId);
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo, ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        Optional<Player> playerOpt = playerRepo.findById(guessedId);
        if (playerOpt.isEmpty()) {
            return false;
        }

        Player player = playerOpt.get();
        boolean isMidfielder = "Midfield".equalsIgnoreCase(player.getPosition());
        boolean isCurrent = player.getLastSeason() != null && player.getLastSeason() >= 2024;
        if (!isMidfielder || !isCurrent || player.getCurrentClubId() == null) {
            return false;
        }

        Optional<Club> clubOpt = clubRepo.findById(player.getCurrentClubId());
        return clubOpt.isPresent() && targetValue.equalsIgnoreCase(clubOpt.get().getDomesticCompetitionId());
    }
}
