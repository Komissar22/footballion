package com.footballion.footballion.service;

import com.footballion.footballion.dto.AnswerResponseDto;
import com.footballion.footballion.entity.Appearance;
import com.footballion.footballion.entity.Club;
import com.footballion.footballion.entity.Player;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.util.LeagueUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RarityAndStatsService {

    // 5-tier scoring scale
    public static final int TIER_COMMON = 10;
    public static final int TIER_MODERATE = 30;
    public static final int TIER_FAIRLY_RARE = 60;
    public static final int TIER_VERY_RARE = 85;
    public static final int TIER_EXTREMELY_RARE = 100;

    private final PlayerRepository playerRepo;
    private final ClubRepository clubRepo;
    private final AppearanceRepository appearanceRepo;

    // daily users pick
    private final Map<String, Map<Integer, Integer>> dailyPicks = new ConcurrentHashMap<>();

    // daily total socres
    private final Map<LocalDate, List<Integer>> dailyTotalScores = new ConcurrentHashMap<>();

    public RarityAndStatsService(PlayerRepository playerRepo, ClubRepository clubRepo, AppearanceRepository appearanceRepo) {
        this.playerRepo = playerRepo;
        this.clubRepo = clubRepo;
        this.appearanceRepo = appearanceRepo;
    }

    public AnswerResponseDto evaluateCorrectAnswer(String strategyType, Integer guessedId, String targetValue) {
        String questionKey = LocalDate.now() + "_" + strategyType + "_" + targetValue;

        dailyPicks.putIfAbsent(questionKey, new ConcurrentHashMap<>());
        Map<Integer, Integer> questionPicks = dailyPicks.get(questionKey);
        questionPicks.put(guessedId, questionPicks.getOrDefault(guessedId, 0) + 1);

        int totalResponsesForQuestion = questionPicks.values().stream().mapToInt(Integer::intValue).sum();
        int thisAnswerPicks = questionPicks.get(guessedId);
        double crowdPickRatio = (double) thisAnswerPicks / totalResponsesForQuestion; // בין 0 ל-1
        double displayPercentage = Math.round(crowdPickRatio * 1000.0) / 10.0;

        if ("CLUB_IN_LEAGUE".equalsIgnoreCase(strategyType)) {
            Club club = clubRepo.findById(guessedId).orElse(null);
            String clubName = club != null ? club.getName() : "Unknown Club";
            int rawRarity = calculateClubIntrinsicRarity(guessedId, targetValue);
            int blendedRarity = blendWithCrowdScore(rawRarity, crowdPickRatio, totalResponsesForQuestion);
            int tierScore = mapToTierScore(blendedRarity);
            return new AnswerResponseDto(true, tierScore, getRarityLabel(tierScore), clubName, null, displayPercentage);
        }

        Player player = playerRepo.findById(guessedId).orElse(null);
        if (player == null) {
            return new AnswerResponseDto(true, TIER_FAIRLY_RARE, getRarityLabel(TIER_FAIRLY_RARE), "Unknown", null, 100.0);
        }

        int intrinsicRarity = calculatePlayerIntrinsicRarity(player, strategyType, targetValue);
        int blendedRarity = blendWithCrowdScore(intrinsicRarity, crowdPickRatio, totalResponsesForQuestion);
        int tierScore = mapToTierScore(blendedRarity);

        return new AnswerResponseDto(
                true,
                tierScore,
                getRarityLabel(tierScore),
                player.getName(),
                player.getImageUrl(),
                displayPercentage
        );
    }

    
    private int calculatePlayerIntrinsicRarity(Player guessedPlayer, String strategyType, String targetValue) {
        boolean currentOnly = isCurrentOnlyQuestion(strategyType);
        double guessedProminence = calculatePlayerProminence(guessedPlayer, strategyType);
        List<Player> candidatePool = getCandidatePlayersForQuestion(strategyType, targetValue);

        int absoluteRarity = calculateAbsolutePlayerRarity(guessedProminence);

        if (candidatePool.isEmpty()) {
            return absoluteRarity;
        }

        // סינון למאגר ייחוס של שחקני סגל בכיר מוכרים כדי ששחקני נוער/מילואים לא ידחקו תשובות נדירות לציון נמוך
        long minMvThreshold = currentOnly ? 4_000_000L : 5_000_000L;
        List<Player> referenceCohort = candidatePool.stream()
                .filter(p -> {
                    long mv = currentOnly
                            ? (p.getMarketValueInEur() != null ? p.getMarketValueInEur() : 0L)
                            : Math.max(
                                    p.getMarketValueInEur() != null ? p.getMarketValueInEur() : 0L,
                                    p.getHighestMarketValueInEur() != null ? p.getHighestMarketValueInEur() : 0L
                              );
                    return mv >= minMvThreshold;
                })
                .toList();

        if (referenceCohort.size() < 10) {
            referenceCohort = candidatePool.stream()
                    .sorted(Comparator.comparingDouble((Player p) -> calculatePlayerProminence(p, strategyType)).reversed())
                    .limit(16)
                    .toList();
        }

        long moreProminentCount = 0;
        long equalProminenceCount = 0;
        for (Player candidate : referenceCohort) {
            double pScore = calculatePlayerProminence(candidate, strategyType);
            if (pScore > guessedProminence) {
                moreProminentCount++;
            } else if (Math.abs(pScore - guessedProminence) < 1.0) {
                equalProminenceCount++;
            }
        }

        double rankPosition = moreProminentCount + Math.max(0, (equalProminenceCount - 1) * 0.5);
        double rankRatio = Math.min(1.0, rankPosition / Math.max(1.0, referenceCohort.size()));
        int poolRarity = (int) Math.round(rankRatio * 100.0);

        int combinedRarity = (int) Math.round(poolRarity * 0.65 + absoluteRarity * 0.35);
        return Math.max(0, Math.min(100, combinedRarity));
    }

    private boolean isCurrentOnlyQuestion(String strategyType) {
        if (strategyType == null) return false;
        return "PLAYER_IN_CLUB".equalsIgnoreCase(strategyType)
                || "ATTACKER_IN_LEAGUE".equalsIgnoreCase(strategyType)
                || "MIDFIELDER_IN_LEAGUE".equalsIgnoreCase(strategyType)
                || "DEFENDER_IN_LEAGUE".equalsIgnoreCase(strategyType)
                || "GOALKEEPER_IN_LEAGUE".equalsIgnoreCase(strategyType);
    }

    private double calculatePlayerProminence(Player player, String strategyType) {
        long currentMv = player.getMarketValueInEur() != null ? player.getMarketValueInEur() : 0L;
        long highestMv = player.getHighestMarketValueInEur() != null ? player.getHighestMarketValueInEur() : 0L;
        int lastSeason = player.getLastSeason() != null ? player.getLastSeason() : 2020;
        int caps = player.getInternationalCaps() != null ? player.getInternationalCaps() : 0;

        if (isCurrentOnlyQuestion(strategyType)) {
            return (currentMv * 0.85) + (highestMv * 0.15) + (caps * 100_000.0);
        }

        if ("PAST_PLAYER_IN_LEAGUE".equalsIgnoreCase(strategyType)) {
            return (highestMv * 0.85) + (caps * 250_000.0);
        }

        boolean isNationQuestion = strategyType != null && strategyType.contains("NATION");
        double peakWeight = isNationQuestion ? 0.75 : 0.45;
        double baseMv = Math.max(currentMv, highestMv * peakWeight);

        double recencyMultiplier;
        if (lastSeason >= 2024) {
            recencyMultiplier = 1.15;
        } else if (lastSeason == 2023) {
            recencyMultiplier = 0.90;
        } else {
            int yearsInactive = 2023 - lastSeason;
            recencyMultiplier = Math.max(0.20, 0.80 - (yearsInactive * 0.07));
        }

        double capsBonus = caps * 180_000.0;
        return (baseMv * recencyMultiplier) + capsBonus;
    }

    private int calculateAbsolutePlayerRarity(double prominence) {
        if (prominence >= 55_000_000) return 10;
        if (prominence >= 28_000_000) return 32;
        if (prominence >= 13_000_000) return 58;
        if (prominence >= 3_500_000) return 78;
        return 95;
    }

    private List<Player> getCandidatePlayersForQuestion(String strategyType, String targetValue) {
        if (strategyType == null || targetValue == null) {
            return Collections.emptyList();
        }
        try {
            if ("PLAYER_IN_CLUB".equalsIgnoreCase(strategyType)) {
                Integer clubId = Integer.parseInt(targetValue);
                return playerRepo.findByCurrentClubId(clubId).stream()
                        .filter(p -> p.getLastSeason() != null && p.getLastSeason() >= 2024)
                        .toList();
            }
            if ("PAST_PLAYER_IN_LEAGUE".equalsIgnoreCase(strategyType)) {
                List<Integer> clubIds = clubRepo.findByDomesticCompetitionIdIgnoreCase(targetValue)
                        .stream()
                        .map(Club::getClubId)
                        .toList();
                return playerRepo.findByCurrentClubIdIn(clubIds).stream()
                        .filter(p -> p.getLastSeason() == null || p.getLastSeason() < 2024)
                        .toList();
            }
            if ("PLAYER_IN_NATION".equalsIgnoreCase(strategyType)) {
                return playerRepo.findByCountryOfCitizenshipIgnoreCase(targetValue);
            }
            if ("NATIONALITY_IN_LEAGUE".equalsIgnoreCase(strategyType) && targetValue.contains("|")) {
                String[] parts = targetValue.split("\\|");
                String country = parts[0];
                String leagueId = parts[1];
                List<Integer> clubIds = clubRepo.findByDomesticCompetitionIdIgnoreCase(leagueId)
                        .stream()
                        .map(Club::getClubId)
                        .toList();
                return playerRepo.findByCurrentClubIdIn(clubIds).stream()
                        .filter(p -> country.equalsIgnoreCase(p.getCountryOfCitizenship()))
                        .toList();
            }
            if ("UCL_GOALSCORER".equalsIgnoreCase(strategyType) || "MAJOR_TOURNAMENT_GOALSCORER".equalsIgnoreCase(strategyType)) {
                String compId = "UCL_GOALSCORER".equalsIgnoreCase(strategyType) ? "CL" : targetValue;
                List<Integer> playerIds = appearanceRepo.findByCompetitionIdAndGoalsGreaterThanEqual(compId, 1)
                        .stream()
                        .map(Appearance::getPlayerId)
                        .toList();
                return playerRepo.findAllById(playerIds);
            }
            if ("UCL_ASSISTER".equalsIgnoreCase(strategyType)) {
                List<Integer> playerIds = appearanceRepo.findByCompetitionIdAndAssistsGreaterThanEqual("CL", 1)
                        .stream()
                        .map(Appearance::getPlayerId)
                        .toList();
                return playerRepo.findAllById(playerIds);
            }
            if ("CLUB_LOYALIST".equalsIgnoreCase(strategyType)) {
                Integer clubId = Integer.parseInt(targetValue);
                List<Integer> playerIds = appearanceRepo.findByPlayerClubIdAndCompetitionIdInAndAppearancesCountGreaterThanEqual(
                        clubId, LeagueUtils.TOP_5_LEAGUES, 100
                ).stream().map(Appearance::getPlayerId).toList();
                return playerRepo.findAllById(playerIds);
            }
            if ("PLAYED_FOR_BOTH_CLUBS".equalsIgnoreCase(strategyType) && targetValue.contains("_")) {
                String[] parts = targetValue.split("_");
                Integer clubIdA = Integer.parseInt(parts[0]);
                Integer clubIdB = Integer.parseInt(parts[1]);
                Set<Integer> clubAPlayers = appearanceRepo.findByPlayerClubIdAndCompetitionId(clubIdA, "CLUB_HISTORY")
                        .stream()
                        .map(Appearance::getPlayerId)
                        .collect(Collectors.toSet());
                List<Integer> sharedIds = appearanceRepo.findByPlayerClubIdAndCompetitionId(clubIdB, "CLUB_HISTORY")
                        .stream()
                        .map(Appearance::getPlayerId)
                        .filter(clubAPlayers::contains)
                        .toList();
                return playerRepo.findAllById(sharedIds);
            }
            if ("DIRECT_CLUB_TRANSFER".equalsIgnoreCase(strategyType) && targetValue.contains("_")) {
                String[] parts = targetValue.split("_");
                Integer fromClubId = Integer.parseInt(parts[0]);
                Integer toClubId = Integer.parseInt(parts[1]);
                List<Integer> playerIds = appearanceRepo.findByPlayerClubIdAndGameIdAndCompetitionId(fromClubId, toClubId, "TRANSFER")
                        .stream()
                        .map(Appearance::getPlayerId)
                        .toList();
                return playerRepo.findAllById(playerIds);
            }
            if (strategyType.endsWith("_IN_LEAGUE")) {
                List<Integer> clubIds = clubRepo.findByDomesticCompetitionIdIgnoreCase(targetValue)
                        .stream()
                        .map(Club::getClubId)
                        .toList();
                if (clubIds.isEmpty()) {
                    return Collections.emptyList();
                }
                String requiredPosition = mapStrategyToPosition(strategyType);
                if (requiredPosition != null) {
                    return playerRepo.findByCurrentClubIdInAndPositionIgnoreCase(clubIds, requiredPosition).stream()
                            .filter(p -> p.getLastSeason() != null && p.getLastSeason() >= 2024)
                            .toList();
                }
                return playerRepo.findByCurrentClubIdIn(clubIds);
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    private String mapStrategyToPosition(String strategyType) {
        if ("ATTACKER_IN_LEAGUE".equalsIgnoreCase(strategyType)) return "Attack";
        if ("MIDFIELDER_IN_LEAGUE".equalsIgnoreCase(strategyType)) return "Midfield";
        if ("DEFENDER_IN_LEAGUE".equalsIgnoreCase(strategyType)) return "Defender";
        if ("GOALKEEPER_IN_LEAGUE".equalsIgnoreCase(strategyType)) return "Goalkeeper";
        return null;
    }

    private int calculateClubIntrinsicRarity(Integer clubId, String targetLeagueId) {
        List<Club> leagueClubs = (targetLeagueId != null && !targetLeagueId.isBlank())
                ? clubRepo.findByDomesticCompetitionIdIgnoreCase(targetLeagueId)
                : Collections.emptyList();

        if (leagueClubs.isEmpty()) {
            Club guessedClub = clubRepo.findById(clubId).orElse(null);
            if (guessedClub != null && guessedClub.getDomesticCompetitionId() != null) {
                leagueClubs = clubRepo.findByDomesticCompetitionIdIgnoreCase(guessedClub.getDomesticCompetitionId());
            }
        }

        if (leagueClubs.size() <= 1) {
            return 60;
        }

        List<Integer> clubIds = leagueClubs.stream().map(Club::getClubId).toList();
        List<Player> leaguePlayers = playerRepo.findByCurrentClubIdIn(clubIds);

        Map<Integer, Long> clubSquadValues = new HashMap<>();
        for (Integer id : clubIds) {
            clubSquadValues.put(id, 0L);
        }
        for (Player p : leaguePlayers) {
            if (p.getCurrentClubId() == null) continue;
            long mv = p.getMarketValueInEur() != null ? p.getMarketValueInEur() : 0L;
            int lastSeason = p.getLastSeason() != null ? p.getLastSeason() : 2020;
            long weightedMv = (lastSeason >= 2023) ? mv : (mv / 4);
            clubSquadValues.merge(p.getCurrentClubId(), weightedMv, Long::sum);
        }

        long guessedClubValue = clubSquadValues.getOrDefault(clubId, 0L);
        long richerClubsCount = clubSquadValues.values().stream()
                .filter(v -> v > guessedClubValue)
                .count();

        double rankRatio = (double) richerClubsCount / Math.max(1, leagueClubs.size() - 1);
        return (int) Math.round(rankRatio * 100.0);
    }


    private int blendWithCrowdScore(int intrinsicRarity, double crowdPickRatio, int totalResponses) {
        if (totalResponses <= 2) {
            return intrinsicRarity;
        }
        double crowdRarity = (1.0 - crowdPickRatio) * 100.0;
        double crowdWeight = Math.min(0.40, (totalResponses - 2) * 0.05);
        double blended = (intrinsicRarity * (1.0 - crowdWeight)) + (crowdRarity * crowdWeight);

        return Math.max(0, Math.min(100, (int) Math.round(blended)));
    }

    public int mapToTierScore(int rawRarity) {
        if (rawRarity >= 85) return TIER_EXTREMELY_RARE; // 100 
        if (rawRarity >= 68) return TIER_VERY_RARE;      // 85 
        if (rawRarity >= 45) return TIER_FAIRLY_RARE;    // 60 
        if (rawRarity >= 22) return TIER_MODERATE;       // 30 
        return TIER_COMMON;                              // 10 
    }

    private String getRarityLabel(int tierScore) {
        if (tierScore >= TIER_EXTREMELY_RARE) return "GOAT 🐐";
        if (tierScore >= TIER_VERY_RARE) return "Absolute Ball Knowledge! 🔥";
        if (tierScore >= TIER_FAIRLY_RARE) return "Starting 11 ⚡";
        if (tierScore >= TIER_MODERATE) return "Solid Squad Player 👍";
        return "The Mainstream Pick 📢";
    }


    public Map<String, Object> recordDailyScoreAndGetPercentile(int totalScore) {
        LocalDate today = LocalDate.now();
        dailyTotalScores.putIfAbsent(today, Collections.synchronizedList(new ArrayList<>()));
        List<Integer> scoresToday = dailyTotalScores.get(today);

        synchronized (scoresToday) {
            if (scoresToday.isEmpty()) {
                scoresToday.addAll(Arrays.asList(50, 90, 130, 170, 210, 245, 280, 315, 355, 410, 455));
            }
            scoresToday.add(totalScore);
        }

        long beatenCount = scoresToday.stream().filter(s -> s < totalScore).count();
        int totalPlayers = scoresToday.size();
        int percentile = (int) Math.round(((double) beatenCount / Math.max(1, totalPlayers - 1)) * 100.0);
        percentile = Math.max(1, Math.min(99, percentile));

        double averageScore = scoresToday.stream().mapToInt(Integer::intValue).average().orElse(225.0);
        double variance = scoresToday.stream()
                .mapToDouble(s -> Math.pow(s - averageScore, 2))
                .average()
                .orElse(9000.0);
        double stdDev = Math.max(75.0, Math.min(115.0, Math.sqrt(variance)));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalScore", totalScore);
        stats.put("percentile", percentile);
        stats.put("averageScore", Math.round(averageScore));
        stats.put("stdDev", Math.round(stdDev));
        stats.put("totalPlayersToday", totalPlayers);
        return stats;
    }
}
