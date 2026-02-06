package com.univ.maturity;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AssessmentRepository extends MongoRepository<Assessment, String> {
    List<Assessment> findByTeam(Team team);
}
