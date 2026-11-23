package com.footballion.footballion.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LeagueUtils {

    public static final Set<String> TOP_5_LEAGUES = Set.of("GB1", "ES1", "IT1", "L1", "FR1");

    // 50 נבחרות הכדורגל המובילות והמוכרות בעולם (כולל ישראל)
    public static final List<String> TOP_50_FOOTBALL_NATIONS = List.of(
        "Argentina", "France", "Spain", "England", "Brazil",
        "Portugal", "Netherlands", "Belgium", "Italy", "Germany",
        "Uruguay", "Colombia", "Croatia", "Morocco", "Japan",
        "United States", "Senegal", "Iran", "Mexico", "Switzerland",
        "Denmark", "Austria", "Korea, South", "Australia", "Ukraine",
        "Türkiye", "Sweden", "Wales", "Serbia", "Nigeria",
        "Algeria", "Egypt", "Poland", "Hungary", "Norway",
        "Scotland", "Czech Republic", "Ecuador", "Cote d'Ivoire", "Cameroon",
        "Canada", "Chile", "Greece", "Romania", "Slovakia",
        "Ireland", "Ghana", "Paraguay", "Slovenia", "Israel"
    );

    private static final Set<String> TOP_50_NATIONS_LOWER = TOP_50_FOOTBALL_NATIONS.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

    // המועדונים הגדולים באירופה (Top 5 Leagues) לפי club_id של Transfermarkt
    public static final Set<Integer> BIG_CLUB_IDS = Set.of(
        // England (Premier League)
        281,   // Manchester City
        11,    // Arsenal FC
        31,    // Liverpool FC
        631,   // Chelsea FC
        985,   // Manchester United
        148,   // Tottenham Hotspur
        762,   // Newcastle United
        405,   // Aston Villa
        // Spain (La Liga)
        418,   // Real Madrid
        131,   // FC Barcelona
        13,    // Atlético de Madrid
        // Italy (Serie A)
        46,    // Inter Milan
        5,     // AC Milan
        506,   // Juventus FC
        6195,  // SSC Napoli
        12,    // AS Roma
        // Germany (Bundesliga)
        27,    // Bayern Munich
        16,    // Borussia Dortmund
        15,    // Bayer 04 Leverkusen
        23826, // RB Leipzig
        // France (Ligue 1)
        583,   // Paris Saint-Germain
        162,   // AS Monaco
        244,   // Olympique Marseille
        1041   // Olympique Lyon
    );

    // מילון שמתרגם את הקודים של Transfermarkt לשמות אמיתיים
    private static final Map<String, String> LEAGUE_NAMES = Map.ofEntries(
        Map.entry("GB1", "Premier League (England)"),
        Map.entry("ES1", "La Liga (Spain)"),
        Map.entry("IT1", "Serie A (Italy)"),
        Map.entry("L1", "Bundesliga (Germany)"), 
        Map.entry("FR1", "Ligue 1 (France)"),
        Map.entry("PO1", "Liga Portugal"),
        Map.entry("NL1", "Eredivisie (Netherlands)"),
        Map.entry("SA1", "Saudi Pro League"),
        Map.entry("TR1", "Süper Lig (Turkey)"),
        Map.entry("GR1", "Super League (Greece)"),
        Map.entry("BE1", "Jupiler Pro League (Belgium)"),
        Map.entry("BRA1", "Série A (Brazil)"),
        Map.entry("MLS1", "MLS (USA)")
    );

    // פונקציה שמחזירה את שם הליגה (או את הקוד אם היא לא נמצאה)
    public static String getLeagueName(String leagueId) {
        return LEAGUE_NAMES.getOrDefault(leagueId, leagueId);
    }

    // פונקציה שבודקת האם הליגה הזו היא ליגה גדולה ומוכרת ששמנו במילון
    public static boolean isMajorLeague(String leagueId) {
        return leagueId != null && LEAGUE_NAMES.containsKey(leagueId);
    }

    // פונקציה שבודקת האם הליגה היא אחת מ-5 הליגות הבכירות באירופה
    public static boolean isTop5League(String leagueId) {
        return leagueId != null && TOP_5_LEAGUES.contains(leagueId.toUpperCase());
    }

    // פונקציה שבודקת האם המדינה שייכת ל-50 נבחרות הכדורגל המובילות (כולל ישראל)
    public static boolean isTop50Nation(String country) {
        return country != null && TOP_50_NATIONS_LOWER.contains(country.trim().toLowerCase());
    }

    // פונקציה שבודקת האם מועדון הוא מועדון פאר גדול
    public static boolean isBigClub(Integer clubId) {
        return clubId != null && BIG_CLUB_IDS.contains(clubId);
    }

    /**
     * בדיקת תקינות צמד מועדונים לשאלות העברה ישירה / שיחק בשני המועדונים:
     * 1. שני המועדונים הם מועדונים גדולים (יכולים להיות מאותה ליגה או מליגות שונות), או:
     * 2. מועדון אחד גדול והמועדון השני קטן יותר אך מאותה ליגה בדיוק.
     */
    public static boolean isValidClubPair(Integer clubIdA, String leagueA, Integer clubIdB, String leagueB) {
        if (clubIdA == null || clubIdB == null || clubIdA.equals(clubIdB)) {
            return false;
        }
        boolean bigA = isBigClub(clubIdA);
        boolean bigB = isBigClub(clubIdB);

        if (bigA && bigB) {
            return true;
        }
        if ((bigA || bigB) && leagueA != null && leagueA.equalsIgnoreCase(leagueB)) {
            return true;
        }
        return false;
    }
}
