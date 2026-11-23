package com.footballion.footballion.strategy;

import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.entity.Club;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.util.LeagueUtils;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
public class ClubInLeagueStrategy implements QuestionStrategy {

    private final Random random = new Random();

    @Override
    public String getType() {
        return "CLUB_IN_LEAGUE";
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
        } while (leagueId == null || leagueId.isBlank()|| !LeagueUtils.isMajorLeague(leagueId));

        String leagueName = LeagueUtils.getLeagueName(leagueId);
        String questionText = "Name a club in " + leagueName;
        
        String targetValue = leagueId;

        return new QuestionDto(questionText, getType(), targetValue);
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo, ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        
        Optional<Club> clubOpt = clubRepo.findById(guessedId);
        
        if (clubOpt.isPresent()) {
            Club guessedClub = clubOpt.get();
            
            return targetValue.equals(guessedClub.getDomesticCompetitionId());
        }
        
        return false; 
    }
}