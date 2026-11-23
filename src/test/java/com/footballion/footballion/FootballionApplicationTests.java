package com.footballion.footballion;

import com.footballion.footballion.service.RarityAndStatsService;
import com.footballion.footballion.util.LeagueUtils;
import com.footballion.footballion.util.TextNormalizer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FootballionApplicationTests {

    @Test
    void testFiveTierRarityMapping() {
        RarityAndStatsService service = new RarityAndStatsService(null, null, null);

        assertEquals(10, service.mapToTierScore(5), "Obvious answers should score 10 points");
        assertEquals(10, service.mapToTierScore(21), "Upper boundary of common tier should score 10 points");
        assertEquals(30, service.mapToTierScore(22), "Moderately common answers should score 30 points");
        assertEquals(30, service.mapToTierScore(44), "Upper boundary of moderate tier should score 30 points");
        assertEquals(60, service.mapToTierScore(45), "Fairly rare answers should score 60 points");
        assertEquals(60, service.mapToTierScore(67), "Upper boundary of fairly rare tier should score 60 points");
        assertEquals(85, service.mapToTierScore(68), "Very rare answers should score 85 points");
        assertEquals(85, service.mapToTierScore(84), "Upper boundary of very rare tier should score 85 points");
        assertEquals(100, service.mapToTierScore(85), "Extremely rare answers should score 100 points");
        assertEquals(100, service.mapToTierScore(100), "Maximum rarity should score 100 points");
    }

    @Test
    void testTop5AndMajorLeagueFilters() {
        assertTrue(LeagueUtils.isTop5League("GB1"), "Premier League should be Top 5");
        assertTrue(LeagueUtils.isTop5League("ES1"), "La Liga should be Top 5");
        assertTrue(LeagueUtils.isTop5League("IT1"), "Serie A should be Top 5");
        assertTrue(LeagueUtils.isTop5League("L1"), "Bundesliga should be Top 5");
        assertTrue(LeagueUtils.isTop5League("FR1"), "Ligue 1 should be Top 5");

        assertFalse(LeagueUtils.isTop5League("PO1"), "Liga Portugal should not be in Top 5 filter");
        assertTrue(LeagueUtils.isMajorLeague("PO1"), "Liga Portugal should still be a Major League for ClubInLeague");
        assertFalse(LeagueUtils.isTop5League(null));
    }

    @Test
    void testDailyScorePercentileAndBellCurveStats() {
        RarityAndStatsService service = new RarityAndStatsService(null, null, null);
        Map<String, Object> stats = service.recordDailyScoreAndGetPercentile(320);

        assertNotNull(stats);
        assertEquals(320, stats.get("totalScore"));
        assertTrue((int) stats.get("percentile") >= 1 && (int) stats.get("percentile") <= 99);
        assertNotNull(stats.get("averageScore"));
        assertNotNull(stats.get("stdDev"));
    }

    @Test
    void testAccentInsensitiveTextNormalizer() {
        assertEquals("desire doue", TextNormalizer.normalize("Désiré Doué"));
        assertEquals("kylian mbappe", TextNormalizer.normalize("Kylian Mbappé"));
        assertEquals("martin odegaard", TextNormalizer.normalize("Martin Ødegaard"));
        assertEquals("luka modric", TextNormalizer.normalize("Luka Modrić"));
    }

    @Test
    void testTop50NationsAndBigClubPairs() {
        assertTrue(LeagueUtils.isTop50Nation("Israel"), "Israel should be included in Top 50 Football Nations");
        assertTrue(LeagueUtils.isTop50Nation("Brazil"), "Brazil should be included in Top 50 Football Nations");
        assertFalse(LeagueUtils.isTop50Nation("Comoros"), "Comoros should be excluded from Top 50 Football Nations");

        // 2 Big Clubs (different leagues: Chelsea 631 in GB1, Real Madrid 418 in ES1) -> Valid
        assertTrue(LeagueUtils.isValidClubPair(631, "GB1", 418, "ES1"));
        // 1 Big Club + 1 Smaller Club in the SAME league (Juventus 506 + Empoli 749 in IT1) -> Valid
        assertTrue(LeagueUtils.isValidClubPair(749, "IT1", 506, "IT1"));
        // 2 Smaller Clubs in the same league (Empoli 749 + Torino 416 in IT1) -> Invalid
        assertFalse(LeagueUtils.isValidClubPair(749, "IT1", 416, "IT1"));
    }
}
