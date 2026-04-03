package com.univ.maturity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaturityModelRepository extends JpaRepository<MaturityModel, String> {
    Boolean existsByName(String name);
    List<MaturityModel> findByTeamId(String teamId);
    List<MaturityModel> findByTeamIdOrTeamIdIsNull(String teamId);
}
