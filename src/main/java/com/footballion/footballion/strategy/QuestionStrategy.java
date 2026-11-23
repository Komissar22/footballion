package com.footballion.footballion.strategy;

import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.repository.AppearanceRepository;

public interface QuestionStrategy {
    
    String getType();

    QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo);

    boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo, ClubRepository clubRepo, AppearanceRepository appearanceRepo);
}