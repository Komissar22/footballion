package com.footballion.footballion.controller;

import com.footballion.footballion.dto.AnswerResponseDto;
import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.service.QuestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final QuestionService questionService;

    public GameController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/start")
    public List<QuestionDto> startGame() {
        return questionService.getDailyGameQuestions();
    }

    @PostMapping("/answer")
    public AnswerResponseDto checkAnswer(@RequestBody AnswerRequest request) {
        return questionService.checkAndScoreAnswer(
                request.getStrategyType(),
                request.getGuessedId(),
                request.getTargetValue()
        );
    }

    @PostMapping("/finish")
    public Map<String, Object> finishGame(@RequestBody Map<String, Integer> payload) {
        int totalScore = payload.getOrDefault("totalScore", 0);
        return questionService.finishDailyGame(totalScore);
    }
}

class AnswerRequest {
    private String strategyType;
    private Integer guessedId;
    private String targetValue;

    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }

    public Integer getGuessedId() { return guessedId; }
    public void setGuessedId(Integer guessedId) { this.guessedId = guessedId; }

    public String getTargetValue() { return targetValue; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }
}