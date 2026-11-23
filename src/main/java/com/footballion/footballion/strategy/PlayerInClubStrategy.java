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
public class PlayerInClubStrategy implements QuestionStrategy {

    private final Random random = new Random();

    @Override
    public String getType() {
        return "PLAYER_IN_CLUB";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        List<Club> top5Clubs = clubRepo.findAll().stream()
                .filter(c -> LeagueUtils.isTop5League(c.getDomesticCompetitionId()))
                .filter(c -> playerRepo.findByCurrentClubId(c.getClubId()).stream()
                        .filter(p -> p.getLastSeason() != null && p.getLastSeason() >= 2024)
                        .count() >= 12)
                .toList();

        if (top5Clubs.isEmpty()) return null;

        Club randomClub = top5Clubs.get(random.nextInt(top5Clubs.size()));
        String questionText = "Name a current player in " + randomClub.getName();
        String targetValue = String.valueOf(randomClub.getClubId());

        return new QuestionDto(questionText, getType(), targetValue);
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo, ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        Integer targetClubId = Integer.parseInt(targetValue);
        Optional<Player> playerOpt = playerRepo.findById(guessedId);

        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            boolean isCurrent = player.getLastSeason() != null && player.getLastSeason() >= 2024;
            return isCurrent && player.getCurrentClubId() != null && player.getCurrentClubId().equals(targetClubId);
        }

        return false;
    }
}
