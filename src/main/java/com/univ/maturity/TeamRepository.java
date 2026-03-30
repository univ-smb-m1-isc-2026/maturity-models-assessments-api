package com.univ.maturity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, String> {
    List<Team> findByOwner(User owner);
    Boolean existsByName(String name);
}
