package com.footballion.footballion.loader;

import com.footballion.footballion.entity.Appearance;
import com.footballion.footballion.entity.Club;
import com.footballion.footballion.entity.Player;
import com.footballion.footballion.repository.AppearanceRepository;
import com.footballion.footballion.repository.ClubRepository;
import com.footballion.footballion.repository.PlayerRepository;
import com.footballion.footballion.util.LeagueUtils;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Set<String> TOURNAMENT_COMPETITIONS = Set.of("CL", "FIWC", "EURO");

    private final PlayerRepository playerRepository;
    private final ClubRepository clubRepository;
    private final AppearanceRepository appearanceRepository;

    public DataLoader(PlayerRepository playerRepository,
                      ClubRepository clubRepository,
                      AppearanceRepository appearanceRepository) {
        this.playerRepository = playerRepository;
        this.clubRepository = clubRepository;
        this.appearanceRepository = appearanceRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        loadClubs();
        loadPlayers();
        loadAppearancesAndTransfers();
        System.out.println("=================================================");
        System.out.println("⚽ ALL DATA LOADED SUCCESSFULLY! READY TO PLAY! ⚽");
        System.out.println("Open your browser at: http://localhost:8080");
        System.out.println("=================================================");
    }

    private void loadClubs() {
        if (clubRepository.count() > 0) {
            boolean hasSearchName = clubRepository.findById(11)
                    .map(c -> c.getSearchName() != null && !c.getSearchName().isBlank())
                    .orElse(false);
            if (hasSearchName) {
                return;
            }
            System.out.println("Updating clubs table with accent-normalized search names...");
        }

        try (Reader reader = new InputStreamReader(
                new ClassPathResource("data/clubs.csv").getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] line;
            List<Club> clubs = new ArrayList<>();

            while ((line = csvReader.readNext()) != null) {
                if (line.length < 4 || line[0].isBlank()) {
                    continue;
                }

                try {
                    Integer clubId = Integer.parseInt(line[0].trim());
                    String name = line[2].trim();
                    String domesticCompetitionId = line[3].trim();

                    clubs.add(new Club(clubId, name, domesticCompetitionId));
                } catch (NumberFormatException ignored) {
                }
            }

            clubRepository.saveAll(clubs);
            System.out.println("Loaded " + clubs.size() + " clubs into database.");

        } catch (Exception e) {
            System.err.println("Failed to load clubs.csv: " + e.getMessage());
        }
    }

    private void loadPlayers() {
        if (playerRepository.count() > 0) {
            boolean hasMetadata = playerRepository.findById(10)
                    .map(p -> p.getHighestMarketValueInEur() != null && p.getHighestMarketValueInEur() > 0
                            && p.getSearchName() != null && !p.getSearchName().isBlank())
                    .orElse(false);
            if (hasMetadata) {
                return;
            }
            System.out.println("Updating players table with market value and accent-normalized search names...");
        }

        try (Reader reader = new InputStreamReader(
                new ClassPathResource("data/players.csv").getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] line;
            List<Player> players = new ArrayList<>();

            while ((line = csvReader.readNext()) != null) {
                if (line.length < 6 || line[0].isBlank()) {
                    continue;
                }

                try {
                    // 0: player_id
                    Integer playerId = Integer.parseInt(line[0].trim());

                    // 3: name
                    String name = line.length > 3 && !line[3].isBlank() ? line[3].trim() : "Unknown";

                    // 4: last_season
                    Integer lastSeason = parseIntSafe(line, 4, 2020);

                    // 5: current_club_id
                    Integer currentClubId = parseIntSafe(line, 5, null);

                    // 9: country_of_citizenship
                    String country = line.length > 9 && !line[9].isBlank() ? line[9].trim() :
                                    (line.length > 7 && !line[7].isBlank() ? line[7].trim() : "Unknown");

                    // 12: position
                    String position = line.length > 12 && !line[12].isBlank() ? line[12].trim() :
                                     (line.length > 11 && !line[11].isBlank() ? line[11].trim() : "Unknown");

                    // 17: image_url
                    String imageUrl = line.length > 17 && !line[17].isBlank() ? line[17].trim() : null;

                    // 18: international_caps
                    Integer internationalCaps = parseIntSafe(line, 18, 0);

                    // 24: market_value_in_eur
                    Long marketValue = parseLongSafe(line, 24, 0L);

                    // 25: highest_market_value
                    Long highestMarketValue = parseLongSafe(line, 25, marketValue);

                    players.add(new Player(
                            playerId, name, currentClubId, country, position,
                            lastSeason, imageUrl, internationalCaps, marketValue, highestMarketValue
                    ));
                } catch (NumberFormatException ignored) {
                }
            }

            playerRepository.saveAll(players);
            System.out.println("Loaded " + players.size() + " players into database.");

        } catch (Exception e) {
            System.err.println("Failed to load players.csv: " + e.getMessage());
        }
    }

    private void loadAppearancesAndTransfers() {
        if (appearanceRepository.count() > 0
                && appearanceRepository.existsByPlayerIdAndCompetitionIdAndGoalsGreaterThanEqual(4360, "FIWC", 1)) {
            exportAggregatedCsvIfMissing(appearanceRepository.findAll());
            return;
        }

        System.out.println("Aggregating appearances.csv, games.csv, game_events.csv & transfers.csv for question strategies...");
        appearanceRepository.deleteAllInBatch();

        // Fast path: load pre-aggregated compact CSV if available on classpath
        ClassPathResource preAggregatedRes = new ClassPathResource("data/aggregated_appearances.csv");
        if (preAggregatedRes.exists()) {
            try (Reader reader = new InputStreamReader(preAggregatedRes.getInputStream(), StandardCharsets.UTF_8);
                 CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {
                List<Appearance> preloaded = new ArrayList<>(40000);
                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    if (line.length < 8) continue;
                    String appId = line[0];
                    Integer playerId = parseIntSafe(line, 1, null);
                    Integer clubId = parseIntSafe(line, 2, 0);
                    Integer gameId = parseIntSafe(line, 3, 0);
                    String compId = line[4];
                    Integer appCount = parseIntSafe(line, 5, 1);
                    Integer goals = parseIntSafe(line, 6, 0);
                    Integer assists = parseIntSafe(line, 7, 0);
                    if (playerId != null && compId != null) {
                        preloaded.add(new Appearance(appId, playerId, clubId, gameId, compId, appCount, goals, assists));
                    }
                }
                int batchSize = 2000;
                for (int i = 0; i < preloaded.size(); i += batchSize) {
                    appearanceRepository.saveAll(preloaded.subList(i, Math.min(i + batchSize, preloaded.size())));
                }
                System.out.println("Loaded " + preloaded.size() + " pre-aggregated appearance & transfer records from aggregated_appearances.csv.");
                return;
            } catch (Exception e) {
                System.err.println("Could not load aggregated_appearances.csv, falling back to raw CSV ETL: " + e.getMessage());
            }
        }

        Set<Integer> top5ClubIds = clubRepository.findAll().stream()
                .filter(c -> LeagueUtils.isTop5League(c.getDomesticCompetitionId()))
                .map(Club::getClubId)
                .collect(Collectors.toSet());

        Map<String, Appearance> aggregated = new HashMap<>(65536);
        Set<Integer> tournamentGamesInAppearances = new HashSet<>();

        try (Reader reader = new InputStreamReader(
                new ClassPathResource("data/appearances.csv").getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 12) {
                    continue;
                }
                Integer gameId = parseIntSafe(line, 1, null);
                Integer playerId = parseIntSafe(line, 2, null);
                Integer clubId = parseIntSafe(line, 3, null);
                String competitionId = line[7] != null ? line[7].trim().toUpperCase() : "";
                int goals = parseIntSafe(line, 10, 0);
                int assists = parseIntSafe(line, 11, 0);

                if (playerId == null || competitionId.isEmpty()) {
                    continue;
                }

                if (TOURNAMENT_COMPETITIONS.contains(competitionId)) {
                    if (gameId != null) {
                        tournamentGamesInAppearances.add(gameId);
                    }
                    String key = playerId + "_0_" + competitionId;
                    Appearance app = aggregated.get(key);
                    if (app == null) {
                        app = new Appearance(key, playerId, 0, 0, competitionId, 1, goals, assists);
                        aggregated.put(key, app);
                    } else {
                        app.setAppearancesCount(app.getAppearancesCount() + 1);
                        app.setGoals(app.getGoals() + goals);
                        app.setAssists(app.getAssists() + assists);
                    }
                }

                if (clubId != null && top5ClubIds.contains(clubId)) {
                    String historyKey = playerId + "_" + clubId + "_CLUB_HISTORY";
                    Appearance hist = aggregated.get(historyKey);
                    if (hist == null) {
                        hist = new Appearance(historyKey, playerId, clubId, 0, "CLUB_HISTORY", 1, goals, assists);
                        aggregated.put(historyKey, hist);
                    } else {
                        hist.setAppearancesCount(hist.getAppearancesCount() + 1);
                        hist.setGoals(hist.getGoals() + goals);
                        hist.setAssists(hist.getAssists() + assists);
                    }

                    if (LeagueUtils.isTop5League(competitionId)) {
                        String leagueKey = playerId + "_" + clubId + "_" + competitionId;
                        Appearance leagueApp = aggregated.get(leagueKey);
                        if (leagueApp == null) {
                            leagueApp = new Appearance(leagueKey, playerId, clubId, 0, competitionId, 1, goals, assists);
                            aggregated.put(leagueKey, leagueApp);
                        } else {
                            leagueApp.setAppearancesCount(leagueApp.getAppearancesCount() + 1);
                            leagueApp.setGoals(leagueApp.getGoals() + goals);
                            leagueApp.setAssists(leagueApp.getAssists() + assists);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process appearances.csv: " + e.getMessage());
        }

        Map<Integer, String> tournamentGameToCompetition = new HashMap<>();
        try (Reader reader = new InputStreamReader(
                new ClassPathResource("data/games.csv").getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 2) continue;
                Integer gameId = parseIntSafe(line, 0, null);
                String compId = line[1] != null ? line[1].trim().toUpperCase() : "";
                if (gameId != null && TOURNAMENT_COMPETITIONS.contains(compId)
                        && !tournamentGamesInAppearances.contains(gameId)) {
                    tournamentGameToCompetition.put(gameId, compId);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process games.csv: " + e.getMessage());
        }

        if (!tournamentGameToCompetition.isEmpty()) {
            try (Reader reader = new InputStreamReader(
                    new ClassPathResource("data/game_events.csv").getInputStream(), StandardCharsets.UTF_8);
                 CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    if (line.length < 8) continue;
                    if (!"Goals".equalsIgnoreCase(line[4] != null ? line[4].trim() : "")) {
                        continue;
                    }
                    Integer gameId = parseIntSafe(line, 2, null);
                    String compId = gameId != null ? tournamentGameToCompetition.get(gameId) : null;
                    if (compId == null) {
                        continue;
                    }
                    String description = line.length > 8 && line[8] != null ? line[8] : "";
                    if (description.toLowerCase().contains("own-goal")) {
                        continue;
                    }

                    Integer scorerId = parseIntSafe(line, 7, null);
                    if (scorerId != null) {
                        String key = scorerId + "_0_" + compId;
                        Appearance app = aggregated.get(key);
                        if (app == null) {
                            app = new Appearance(key, scorerId, 0, 0, compId, 1, 1, 0);
                            aggregated.put(key, app);
                        } else {
                            app.setGoals(app.getGoals() + 1);
                        }
                    }

                    Integer assisterId = parseIntSafe(line, 10, null);
                    if (assisterId != null) {
                        String assistKey = assisterId + "_0_" + compId;
                        Appearance assistApp = aggregated.get(assistKey);
                        if (assistApp == null) {
                            assistApp = new Appearance(assistKey, assisterId, 0, 0, compId, 1, 0, 1);
                            aggregated.put(assistKey, assistApp);
                        } else {
                            assistApp.setAssists(assistApp.getAssists() + 1);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to process game_events.csv: " + e.getMessage());
            }
        }

        try (Reader reader = new InputStreamReader(
                new ClassPathResource("data/transfers.csv").getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 8) {
                    continue;
                }
                Integer playerId = parseIntSafe(line, 0, null);
                Integer fromClubId = parseIntSafe(line, 3, null);
                Integer toClubId = parseIntSafe(line, 4, null);
                int feeInThousands = (int) (parseLongSafe(line, 7, 0L) / 1000L);

                if (playerId == null || fromClubId == null || toClubId == null || fromClubId.equals(toClubId)) {
                    continue;
                }

                if (top5ClubIds.contains(fromClubId) && top5ClubIds.contains(toClubId)) {
                    String transferKey = playerId + "_" + fromClubId + "_" + toClubId + "_TRANSFER";
                    Appearance existing = aggregated.get(transferKey);
                    if (existing == null) {
                        aggregated.put(transferKey, new Appearance(
                                transferKey, playerId, fromClubId, toClubId, "TRANSFER", 1, feeInThousands, 0
                        ));
                    } else {
                        existing.setAppearancesCount(existing.getAppearancesCount() + 1);
                        existing.setGoals(Math.max(existing.getGoals(), feeInThousands));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process transfers.csv: " + e.getMessage());
        }

        List<Appearance> toSave = aggregated.values().stream()
                .filter(app -> {
                    String comp = app.getCompetitionId();
                    if (TOURNAMENT_COMPETITIONS.contains(comp)) {
                        return app.getGoals() > 0 || app.getAssists() > 0;
                    }
                    if (LeagueUtils.isTop5League(comp)) {
                        return app.getAppearancesCount() >= 100;
                    }
                    return true;
                })
                .toList();

        int batchSize = 2000;
        for (int i = 0; i < toSave.size(); i += batchSize) {
            int end = Math.min(i + batchSize, toSave.size());
            appearanceRepository.saveAll(toSave.subList(i, end));
        }

        exportAggregatedCsvIfMissing(toSave);
        System.out.println("Loaded " + toSave.size() + " aggregated appearance & transfer records into database.");
    }

    private void exportAggregatedCsvIfMissing(List<Appearance> records) {
        java.nio.file.Path outPath = java.nio.file.Paths.get("src/main/resources/data/aggregated_appearances.csv");
        if (java.nio.file.Files.exists(outPath) || !java.nio.file.Files.exists(outPath.getParent())) {
            return;
        }
        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
            writer.write("\"appearance_id\",\"player_id\",\"player_club_id\",\"game_id\",\"competition_id\",\"appearances_count\",\"goals\",\"assists\"\n");
            for (Appearance a : records) {
                writer.write(String.format("\"%s\",\"%d\",\"%d\",\"%d\",\"%s\",\"%d\",\"%d\",\"%d\"\n",
                        a.getAppearanceId(),
                        a.getPlayerId() != null ? a.getPlayerId() : 0,
                        a.getPlayerClubId() != null ? a.getPlayerClubId() : 0,
                        a.getGameId() != null ? a.getGameId() : 0,
                        a.getCompetitionId() != null ? a.getCompetitionId() : "",
                        a.getAppearancesCount() != null ? a.getAppearancesCount() : 1,
                        a.getGoals() != null ? a.getGoals() : 0,
                        a.getAssists() != null ? a.getAssists() : 0
                ));
            }
            System.out.println("Exported compact pre-aggregated dataset to " + outPath);
        } catch (Exception e) {
            System.err.println("Optional export of aggregated_appearances.csv skipped: " + e.getMessage());
        }
    }

    private Integer parseIntSafe(String[] line, int index, Integer defaultValue) {
        if (line.length > index && line[index] != null && !line[index].isBlank()) {
            try {
                String raw = line[index].trim().split("\\.")[0];
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Long parseLongSafe(String[] line, int index, Long defaultValue) {
        if (line.length > index && line[index] != null && !line[index].isBlank()) {
            try {
                String raw = line[index].trim().split("\\.")[0];
                return Long.parseLong(raw);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
