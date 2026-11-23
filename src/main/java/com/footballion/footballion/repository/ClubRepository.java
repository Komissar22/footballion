package com.footballion.footballion.repository;

import com.footballion.footballion.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Integer> {
    List<Club> findTop10ByNameContainingIgnoreCase(String name);
    List<Club> findTop10BySearchNameContainingIgnoreCase(String searchName);
    List<Club> findByDomesticCompetitionIdIgnoreCase(String domesticCompetitionId);
}