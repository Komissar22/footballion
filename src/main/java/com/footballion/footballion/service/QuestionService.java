package com.footballion.footballion.service;

import com.footballion.footballion.dto.AnswerResponseDto;
import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.strategy.QuestionStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class QuestionService {

    private final List<QuestionStrategy> strategies;
    private final PlayerRepository playerRepo;
    private final ClubRepository clubRepo;
    private final AppearanceRepository appearanceRepo;
    private final RarityAndStatsService rarityService;

    // saving today's questions
    private LocalDate cachedDate = null;
    private List<QuestionDto> cachedDailyQuestions = new ArrayList<>();

    public QuestionService(List<QuestionStrategy> strategies, PlayerRepository playerRepo,
                           ClubRepository clubRepo, AppearanceRepository appearanceRepo,
                           RarityAndStatsService rarityService) {
        this.strategies = strategies;
        this.playerRepo = playerRepo;
        this.clubRepo = clubRepo;
        this.appearanceRepo = appearanceRepo;
        this.rarityService = rarityService;
    }

    public synchronized List<QuestionDto> getDailyGameQuestions() {
        LocalDate today = LocalDate.now();

        if (today.equals(cachedDate) && !cachedDailyQuestions.isEmpty()) {
            return cachedDailyQuestions;
        }

        if (strategies.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestionDto> gameQuestions = new ArrayList<>();
        List<QuestionStrategy> sortedStrategies = new ArrayList<>(strategies);
        sortedStrategies.sort(Comparator.comparing(QuestionStrategy::getType));

        Random dailyRandom = new Random(today.toEpochDay());
        Collections.shuffle(sortedStrategies, dailyRandom);

        int targetQuestions = 5;

        for (QuestionStrategy strategy : sortedStrategies) {
            if (gameQuestions.size() >= targetQuestions) {
                break;
            }
            QuestionDto question = strategy.generateQuestion(clubRepo, playerRepo, appearanceRepo);
            if (question != null) {
                gameQuestions.add(question);
            }
        }

        this.cachedDate = today;
        this.cachedDailyQuestions = gameQuestions;
        return cachedDailyQuestions;
    }

    public AnswerResponseDto checkAndScoreAnswer(String strategyType, Integer guessedId, String targetValue) {
        Optional<QuestionStrategy> matchingStrategy = strategies.stream()
                .filter(s -> s.getType().equals(strategyType))
                .findFirst();

        if (matchingStrategy.isPresent()) {
            boolean isValid = matchingStrategy.get().validateAnswer(guessedId, targetValue, playerRepo, clubRepo, appearanceRepo);
            if (isValid) {
                return rarityService.evaluateCorrectAnswer(strategyType, guessedId, targetValue);
            }
        }

        return new AnswerResponseDto(false, 0, "'Wrong Answer'", null, null, 0.0);
    }

    public Map<String, Object> finishDailyGame(int totalScore) {
        return rarityService.recordDailyScoreAndGetPercentile(totalScore);
    }
}