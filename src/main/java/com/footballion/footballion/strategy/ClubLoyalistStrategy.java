package com.footballion.footballion.strategy;

import com.footballion.footballion.dto.QuestionDto;
import com.footballion.footballion.entity.Appearance;
import com.footballion.footballion.entity.Club;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.util.LeagueUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ClubLoyalistStrategy implements QuestionStrategy {

    private final Random random = new Random();

    @Override
    public String getType() {
        return "CLUB_LOYALIST";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        List<Appearance> loyalists = appearanceRepo.findByCompetitionIdInAndAppearancesCountGreaterThanEqual(
                LeagueUtils.TOP_5_LEAGUES, 100
        );
        if (loyalists.isEmpty()) {
            return null;
        }

        Map<Integer, Long> clubCounts = loyalists.stream()
                .filter(a -> a.getPlayerClubId() != null && a.getPlayerClubId() > 0)
                .collect(Collectors.groupingBy(Appearance::getPlayerClubId, Collectors.counting()));

        List<Integer> eligibleClubIds = clubCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (eligibleClubIds.isEmpty()) {
            eligibleClubIds = new ArrayList<>(clubCounts.keySet());
        }

        while (!eligibleClubIds.isEmpty()) {
            Integer chosenClubId = eligibleClubIds.remove(random.nextInt(eligibleClubIds.size()));
            Optional<Club> clubOpt = clubRepo.findById(chosenClubId);
            if (clubOpt.isPresent() && LeagueUtils.isTop5League(clubOpt.get().getDomesticCompetitionId())) {
                Club club = clubOpt.get();
                String questionText = "Name a current or past player with 100+ league appearances for " + club.getName();
                return new QuestionDto(questionText, getType(), String.valueOf(club.getClubId()));
            }
        }

        return null;
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo,
                                  ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        if (guessedId == null || targetValue == null) {
            return false;
        }
        try {
            Integer targetClubId = Integer.parseInt(targetValue);
            return appearanceRepo.existsByPlayerIdAndPlayerClubIdAndCompetitionIdInAndAppearancesCountGreaterThanEqual(
                    guessedId, targetClubId, LeagueUtils.TOP_5_LEAGUES, 100
            );
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
