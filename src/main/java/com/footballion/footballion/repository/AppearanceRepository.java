package com.footballion.footballion.repository;

import com.footballion.footballion.entity.Appearance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AppearanceRepository extends JpaRepository<Appearance, String> {

    boolean existsByPlayerIdAndCompetitionIdAndGoalsGreaterThanEqual(Integer playerId, String competitionId, Integer minGoals);

    boolean existsByPlayerIdAndCompetitionIdAndAssistsGreaterThanEqual(Integer playerId, String competitionId, Integer minAssists);

    List<Appearance> findByCompetitionIdAndGoalsGreaterThanEqual(String competitionId, Integer minGoals);

    List<Appearance> findByCompetitionIdAndAssistsGreaterThanEqual(String competitionId, Integer minAssists);

    List<Appearance> findByCompetitionIdInAndAppearancesCountGreaterThanEqual(Collection<String> competitionIds, Integer minAppearances);

    List<Appearance> findByPlayerClubIdAndCompetitionIdInAndAppearancesCountGreaterThanEqual(
            Integer playerClubId, Collection<String> competitionIds, Integer minAppearances);

    boolean existsByPlayerIdAndPlayerClubIdAndCompetitionIdInAndAppearancesCountGreaterThanEqual(
            Integer playerId, Integer playerClubId, Collection<String> competitionIds, Integer minAppearances);

    List<Appearance> findByCompetitionId(String competitionId);

    List<Appearance> findByPlayerClubIdAndCompetitionId(Integer playerClubId, String competitionId);

    boolean existsByPlayerIdAndPlayerClubIdAndCompetitionId(Integer playerId, Integer playerClubId, String competitionId);

    List<Appearance> findByPlayerClubIdAndGameIdAndCompetitionId(Integer fromClubId, Integer toClubId, String competitionId);

    boolean existsByPlayerIdAndPlayerClubIdAndGameIdAndCompetitionId(
            Integer playerId, Integer fromClubId, Integer toClubId, String competitionId);
}
