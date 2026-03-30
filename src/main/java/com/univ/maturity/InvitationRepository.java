package com.univ.maturity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, String> {
    Optional<Invitation> findByToken(String token);
    List<Invitation> findByTeamId(String teamId);
    Optional<Invitation> findByInviteeEmailAndTeamIdAndStatus(String email, String teamId, InvitationStatus status);
}
