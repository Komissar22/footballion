package com.footballion.footballion.strategy;

import com.footballion.footballion.dto.QuestionDto;
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
public class PlayerInNationStrategy implements QuestionStrategy {

    private final Random random = new Random();

    @Override
    public String getType() {
        return "PLAYER_IN_NATION";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        List<String> nations = LeagueUtils.TOP_50_FOOTBALL_NATIONS;
        String nation = nations.get(random.nextInt(nations.size()));

        String questionText = "Name a current or past player from " + nation;
        return new QuestionDto(questionText, getType(), nation);
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo, ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        Optional<Player> playerOpt = playerRepo.findById(guessedId);
        if (playerOpt.isEmpty()) {
            return false;
        }

        Player player = playerOpt.get();
        return targetValue.equalsIgnoreCase(player.getCountryOfCitizenship());
    }
}
