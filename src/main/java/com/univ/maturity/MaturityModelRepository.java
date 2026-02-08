package com.univ.maturity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MaturityModelRepository extends MongoRepository<MaturityModel, String> {
    Boolean existsByName(String name);
    List<MaturityModel> findByTeamId(String teamId);
}
