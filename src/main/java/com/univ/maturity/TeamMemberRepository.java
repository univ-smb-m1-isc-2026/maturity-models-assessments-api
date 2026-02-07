package com.univ.maturity;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends MongoRepository<TeamMember, String> {
    List<TeamMember> findByUserId(String userId);
    List<TeamMember> findByTeamId(String teamId);
    Optional<TeamMember> findByUserIdAndTeamId(String userId, String teamId);
    void deleteByTeamId(String teamId);
}
