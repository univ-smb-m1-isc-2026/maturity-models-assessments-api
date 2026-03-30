package com.univ.maturity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {
    List<TeamMember> findByUser_Id(String userId);
    List<TeamMember> findByTeam_Id(String teamId);
    Optional<TeamMember> findByUser_IdAndTeam_Id(String userId, String teamId);
    void deleteByTeam_Id(String teamId);
}
