package com.footballion.footballion.entity;

import com.footballion.footballion.util.TextNormalizer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(
    name = "clubs",
    indexes = {
        @Index(name = "idx_club_search_name", columnList = "search_name")
    }
)
public class Club {

    @Id
    @Column(name = "club_id")
    private Integer clubId;

    @Column(name = "name")
    private String name;

    @Column(name = "search_name")
    private String searchName;

    @Column(name = "domestic_competition_id")
    private String domesticCompetitionId;

    public Club() {
    }

    public Club(Integer clubId, String name, String domesticCompetitionId) {
        this.clubId = clubId;
        this.name = name;
        this.searchName = TextNormalizer.normalize(name);
        this.domesticCompetitionId = domesticCompetitionId;
    }

    // Getters and Setters
    public Integer getClubId() { return clubId; }
    public void setClubId(Integer clubId) { this.clubId = clubId; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.searchName = TextNormalizer.normalize(name);
    }

    public String getSearchName() { return searchName; }
    public void setSearchName(String searchName) { this.searchName = searchName; }

    public String getDomesticCompetitionId() { return domesticCompetitionId; }
    public void setDomesticCompetitionId(String domesticCompetitionId) { this.domesticCompetitionId = domesticCompetitionId; }
}