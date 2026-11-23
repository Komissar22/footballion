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
public class DirectClubTransferStrategy implements QuestionStrategy {

    private final Random random = new Random();
    private List<int[]> cachedTransferRoutes = null;

    @Override
    public String getType() {
        return "DIRECT_CLUB_TRANSFER";
    }

    @Override
    public QuestionDto generateQuestion(ClubRepository clubRepo, PlayerRepository playerRepo, AppearanceRepository appearanceRepo) {
        List<int[]> routes = getOrBuildTransferRoutes(clubRepo, appearanceRepo);
        if (routes.isEmpty()) {
            return null;
        }

        int[] route = routes.get(random.nextInt(routes.size()));
        Optional<Club> fromClubOpt = clubRepo.findById(route[0]);
        Optional<Club> toClubOpt = clubRepo.findById(route[1]);

        if (fromClubOpt.isEmpty() || toClubOpt.isEmpty()) {
            return null;
        }

        String fromName = fromClubOpt.get().getName();
        String toName = toClubOpt.get().getName();
        String questionText = "Name a current or past player who transferred directly from " + fromName + " to " + toName;
        String targetValue = route[0] + "_" + route[1];

        return new QuestionDto(questionText, getType(), targetValue);
    }

    private synchronized List<int[]> getOrBuildTransferRoutes(ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        if (cachedTransferRoutes != null && !cachedTransferRoutes.isEmpty()) {
            return cachedTransferRoutes;
        }

        Map<Integer, String> clubLeagues = clubRepo.findAll().stream()
                .filter(c -> c.getDomesticCompetitionId() != null)
                .collect(Collectors.toMap(Club::getClubId, Club::getDomesticCompetitionId));

        List<Appearance> transfers = appearanceRepo.findByCompetitionId("TRANSFER");
        Map<String, Set<Integer>> routePlayers = new HashMap<>();

        for (Appearance tr : transfers) {
            Integer fromId = tr.getPlayerClubId();
            Integer toId = tr.getGameId();
            if (tr.getPlayerId() == null || fromId == null || toId == null) continue;

            String fromLeague = clubLeagues.get(fromId);
            String toLeague = clubLeagues.get(toId);
            if (!LeagueUtils.isValidClubPair(fromId, fromLeague, toId, toLeague)) {
                continue;
            }

            String key = fromId + "_" + toId;
            routePlayers.computeIfAbsent(key, k -> new HashSet<>()).add(tr.getPlayerId());
        }

        List<int[]> validRoutes = new ArrayList<>();
        for (Map.Entry<String, Set<Integer>> entry : routePlayers.entrySet()) {
            // בוחרים מסלולי העברה ישירים שבהם עברו לפחות 5 שחקנים שונים
            if (entry.getValue().size() >= 5) {
                String[] parts = entry.getKey().split("_");
                validRoutes.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
            }
        }

        cachedTransferRoutes = validRoutes;
        return cachedTransferRoutes;
    }

    @Override
    public boolean validateAnswer(Integer guessedId, String targetValue, PlayerRepository playerRepo,
                                  ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        if (guessedId == null || targetValue == null || !targetValue.contains("_")) {
            return false;
        }
        try {
            String[] parts = targetValue.split("_");
            Integer fromClubId = Integer.parseInt(parts[0]);
            Integer toClubId = Integer.parseInt(parts[1]);
            return appearanceRepo.existsByPlayerIdAndPlayerClubIdAndGameIdAndCompetitionId(
                    guessedId, fromClubId, toClubId, "TRANSFER"
            );
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
