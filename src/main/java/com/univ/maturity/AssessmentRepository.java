package com.univ.maturity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, String> {
    List<Assessment> findByTeam(Team team);
}
