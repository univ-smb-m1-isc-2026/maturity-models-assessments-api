package com.univ.maturity;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MaturityModelRepository extends MongoRepository<MaturityModel, String> {
    Boolean existsByName(String name);
}
