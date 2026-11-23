package com.footballion.footballion.strategy;

import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MajorTournamentGoalscorerStrategy implements QuestionStrategy {

    private static final String[][] TOURNAMENTS = {
        {"FIWC", "FIFA World Cup"},
        {"EURO", "UEFA European Championship (EURO)"}
    };

    private final Random random = new Random();

    @Override
    public String getType() {
        return "MAJOR_TOURNAMENT_GOALSCORER";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        String[] chosen = TOURNAMENTS[random.nextInt(TOURNAMENTS.length)];
        String compId = chosen[0];
        String tournamentName = chosen[1];

        String questionText = "Name a current or past player who has scored at least one goal in the " + tournamentName;
        return new QuestionDto(questionText, getType(), compId);
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo,
                                  ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        if (guessedId == null || targetValue == null) {
            return false;
        }
        return appearanceRepo.existsByPlayerIdAndCompetitionIdAndGoalsGreaterThanEqual(guessedId, targetValue, 1);
    }
}
