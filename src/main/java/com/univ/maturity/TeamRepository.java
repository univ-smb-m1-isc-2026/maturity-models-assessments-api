package com.univ.maturity;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TeamRepository extends MongoRepository<Team, String> {
    List<Team> findByOwner(User owner);
    Boolean existsByName(String name);
}
