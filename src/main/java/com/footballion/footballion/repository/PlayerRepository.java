package com.footballion.footballion.repository;

import com.footballion.footballion.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {
    List<Player> findTop10ByNameContainingIgnoreCase(String name);
    List<Player> findTop10BySearchNameContainingIgnoreCaseOrderByHighestMarketValueInEurDesc(String searchName);
    List<Player> findByCurrentClubId(Integer currentClubId);
    List<Player> findByCountryOfCitizenshipIgnoreCase(String countryOfCitizenship);
    List<Player> findByCurrentClubIdIn(Collection<Integer> clubIds);
    List<Player> findByCurrentClubIdInAndPositionIgnoreCase(Collection<Integer> clubIds, String position);
}