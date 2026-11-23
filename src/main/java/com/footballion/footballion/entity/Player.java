package com.footballion.footballion.entity;

import com.footballion.footballion.util.TextNormalizer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(
    name = "players",
    indexes = {
        @Index(name = "idx_player_search_name", columnList = "search_name")
    }
)
public class Player {

    @Id
    @Column(name = "player_id")
    private Integer playerId;

    @Column(name = "name")
    private String name;

    @Column(name = "search_name")
    private String searchName;

    @Column(name = "current_club_id")
    private Integer currentClubId;

    @Column(name = "country_of_citizenship")
    private String countryOfCitizenship;

    @Column(name = "position")
    private String position;

    @Column(name = "last_season")
    private Integer lastSeason;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "international_caps")
    private Integer internationalCaps;

    @Column(name = "market_value_in_eur")
    private Long marketValueInEur;

    @Column(name = "highest_market_value_in_eur")
    private Long highestMarketValueInEur;

    // Constructors
    public Player() {
    }

    public Player(Integer playerId, String name, Integer currentClubId, String countryOfCitizenship) {
        this.playerId = playerId;
        this.name = name;
        this.searchName = TextNormalizer.normalize(name);
        this.currentClubId = currentClubId;
        this.countryOfCitizenship = countryOfCitizenship;
    }

    public Player(Integer playerId, String name, Integer currentClubId, String countryOfCitizenship, String position) {
        this.playerId = playerId;
        this.name = name;
        this.searchName = TextNormalizer.normalize(name);
        this.currentClubId = currentClubId;
        this.countryOfCitizenship = countryOfCitizenship;
        this.position = position;
    }

    public Player(Integer playerId, String name, Integer currentClubId, String countryOfCitizenship, String position,
                  Integer lastSeason, String imageUrl, Integer internationalCaps, Long marketValueInEur, Long highestMarketValueInEur) {
        this.playerId = playerId;
        this.name = name;
        this.searchName = TextNormalizer.normalize(name);
        this.currentClubId = currentClubId;
        this.countryOfCitizenship = countryOfCitizenship;
        this.position = position;
        this.lastSeason = lastSeason;
        this.imageUrl = imageUrl;
        this.internationalCaps = internationalCaps;
        this.marketValueInEur = marketValueInEur;
        this.highestMarketValueInEur = highestMarketValueInEur;
    }

    // Getters and Setters
    public Integer getPlayerId() { return playerId; }
    public void setPlayerId(Integer playerId) { this.playerId = playerId; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.searchName = TextNormalizer.normalize(name);
    }

    public String getSearchName() { return searchName; }
    public void setSearchName(String searchName) { this.searchName = searchName; }

    public Integer getCurrentClubId() { return currentClubId; }
    public void setCurrentClubId(Integer currentClubId) { this.currentClubId = currentClubId; }

    public String getCountryOfCitizenship() { return countryOfCitizenship; }
    public void setCountryOfCitizenship(String countryOfCitizenship) { this.countryOfCitizenship = countryOfCitizenship; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Integer getLastSeason() { return lastSeason; }
    public void setLastSeason(Integer lastSeason) { this.lastSeason = lastSeason; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getInternationalCaps() { return internationalCaps; }
    public void setInternationalCaps(Integer internationalCaps) { this.internationalCaps = internationalCaps; }

    public Long getMarketValueInEur() { return marketValueInEur; }
    public void setMarketValueInEur(Long marketValueInEur) { this.marketValueInEur = marketValueInEur; }

    public Long getHighestMarketValueInEur() { return highestMarketValueInEur; }
    public void setHighestMarketValueInEur(Long highestMarketValueInEur) { this.highestMarketValueInEur = highestMarketValueInEur; }
}