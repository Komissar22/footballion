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
public class PlayedForBothClubsStrategy implements QuestionStrategy {

    private final Random random = new Random();
    private List<int[]> cachedClubPairs = null;

    @Override
    public String getType() {
        return "PLAYED_FOR_BOTH_CLUBS";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        List<int[]> validPairs = getOrBuildValidClubPairs(clubRepo, appearanceRepo);
        if (validPairs.isEmpty()) {
            return null;
        }

        int[] pair = validPairs.get(random.nextInt(validPairs.size()));
        Optional<Club> clubAOpt = clubRepo.findById(pair[0]);
        Optional<Club> clubBOpt = clubRepo.findById(pair[1]);

        if (clubAOpt.isEmpty() || clubBOpt.isEmpty()) {
            return null;
        }

        String clubAName = clubAOpt.get().getName();
        String clubBName = clubBOpt.get().getName();
        String questionText = "Name a current or past player who has played for both " + clubAName + " and " + clubBName;
        String targetValue = pair[0] + "_" + pair[1];

        return new QuestionDto(questionText, getType(), targetValue);
    }

    private synchronized List<int[]> getOrBuildValidClubPairs(ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        if (cachedClubPairs != null && !cachedClubPairs.isEmpty()) {
            return cachedClubPairs;
        }

        Map<Integer, String> clubLeagues = clubRepo.findAll().stream()
                .filter(c -> c.getDomesticCompetitionId() != null)
                .collect(Collectors.toMap(Club::getClubId, Club::getDomesticCompetitionId));

        List<Appearance> history = appearanceRepo.findByCompetitionId("CLUB_HISTORY");
        Map<Integer, List<Integer>> playerToClubs = new HashMap<>();

        for (Appearance app : history) {
            if (app.getPlayerId() == null || app.getPlayerClubId() == null) continue;
            playerToClubs.computeIfAbsent(app.getPlayerId(), k -> new ArrayList<>()).add(app.getPlayerClubId());
        }

        Map<String, Integer> pairCounts = new HashMap<>();
        for (List<Integer> clubs : playerToClubs.values()) {
            if (clubs.size() < 2) continue;
            Collections.sort(clubs);
            for (int i = 0; i < clubs.size(); i++) {
                for (int j = i + 1; j < clubs.size(); j++) {
                    int c1 = clubs.get(i);
                    int c2 = clubs.get(j);
                    if (!LeagueUtils.isValidClubPair(c1, clubLeagues.get(c1), c2, clubLeagues.get(c2))) {
                        continue;
                    }
                    String key = c1 + "_" + c2;
                    pairCounts.merge(key, 1, Integer::sum);
                }
            }
        }

        List<int[]> pairs = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pairCounts.entrySet()) {
            if (entry.getValue() >= 10) {
                String[] parts = entry.getKey().split("_");
                pairs.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
            }
        }

        cachedClubPairs = pairs;
        return cachedClubPairs;
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo,
                                  ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        if (guessedId == null || targetValue == null || !targetValue.contains("_")) {
            return false;
        }
        try {
            String[] parts = targetValue.split("_");
            Integer clubIdA = Integer.parseInt(parts[0]);
            Integer clubIdB = Integer.parseInt(parts[1]);

            boolean playedForA = appearanceRepo.existsByPlayerIdAndPlayerClubIdAndCompetitionId(guessedId, clubIdA, "CLUB_HISTORY");
            boolean playedForB = appearanceRepo.existsByPlayerIdAndPlayerClubIdAndCompetitionId(guessedId, clubIdB, "CLUB_HISTORY");
            return playedForA && playedForB;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
