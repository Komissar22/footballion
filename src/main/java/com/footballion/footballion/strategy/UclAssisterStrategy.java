package com.footballion.footballion.strategy;

import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import org.springframework.stereotype.Component;

@Component
public class UclAssisterStrategy implements QuestionStrategy {

    @Override
    public String getType() {
        return "UCL_ASSISTER";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        String questionText = "Name a current or past player who has provided at least one assist in the UEFA Champions League";
        return new QuestionDto(questionText, getType(), "CL");
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo,
                                  ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        if (guessedId == null) {
            return false;
        }
        return appearanceRepo.existsByPlayerIdAndCompetitionIdAndAssistsGreaterThanEqual(guessedId, "CL", 1);
    }
}
