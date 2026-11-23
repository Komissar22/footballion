package com.footballion.footballion.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Index;

@Entity
@Table(
    name = "appearances",
    indexes = {
        @Index(name = "idx_app_comp_goals", columnList = "competition_id, goals"),
        @Index(name = "idx_app_comp_assists", columnList = "competition_id, assists"),
        @Index(name = "idx_app_comp_count", columnList = "competition_id, appearances_count"),
        @Index(name = "idx_app_player_comp", columnList = "player_id, competition_id"),
        @Index(name = "idx_app_club_comp", columnList = "player_club_id, competition_id")
    }
)
public class Appearance {

    @Id
    @Column(name = "appearance_id", length = 64)
    private String appearanceId;

    @Column(name = "player_id")
    private Integer playerId;

    @Column(name = "player_club_id")
    private Integer playerClubId;

    @Column(name = "game_id")
    private Integer gameId;

    @Column(name = "competition_id", length = 32)
    private String competitionId;

    @Column(name = "appearances_count")
    private Integer appearancesCount;

    @Column(name = "goals")
    private Integer goals;

    @Column(name = "assists")
    private Integer assists;

    public Appearance() {
    }

    public Appearance(String appearanceId, Integer playerId, Integer playerClubId, Integer gameId, Integer goals, Integer assists) {
        this.appearanceId = appearanceId;
        this.playerId = playerId;
        this.playerClubId = playerClubId;
        this.gameId = gameId;
        this.goals = goals;
        this.assists = assists;
        this.appearancesCount = 1;
    }

    public Appearance(String appearanceId, Integer playerId, Integer playerClubId, Integer gameId,
                      String competitionId, Integer appearancesCount, Integer goals, Integer assists) {
        this.appearanceId = appearanceId;
        this.playerId = playerId;
        this.playerClubId = playerClubId;
        this.gameId = gameId;
        this.competitionId = competitionId;
        this.appearancesCount = appearancesCount;
        this.goals = goals;
        this.assists = assists;
    }

    public String getAppearanceId() { return appearanceId; }
    public void setAppearanceId(String appearanceId) { this.appearanceId = appearanceId; }

    public Integer getPlayerId() { return playerId; }
    public void setPlayerId(Integer playerId) { this.playerId = playerId; }

    public Integer getPlayerClubId() { return playerClubId; }
    public void setPlayerClubId(Integer playerClubId) { this.playerClubId = playerClubId; }

    public Integer getGameId() { return gameId; }
    public void setGameId(Integer gameId) { this.gameId = gameId; }

    public String getCompetitionId() { return competitionId; }
    public void setCompetitionId(String competitionId) { this.competitionId = competitionId; }

    public Integer getAppearancesCount() { return appearancesCount; }
    public void setAppearancesCount(Integer appearancesCount) { this.appearancesCount = appearancesCount; }

    public Integer getGoals() { return goals; }
    public void setGoals(Integer goals) { this.goals = goals; }

    public Integer getAssists() { return assists; }
    public void setAssists(Integer assists) { this.assists = assists; }
}
