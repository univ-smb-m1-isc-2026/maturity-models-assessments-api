package com.univ.maturity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invitations")
public class Invitation {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String teamId;

    @Column(nullable = false, length = 36)
    private String inviterUserId;
    private String inviterFirstName;
    private String inviterLastName;
    private String inviterEmail;

    @Column(nullable = false)
    private String inviteeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant lastSentAt = Instant.now();
    private Instant expiresAt;
    private Instant acceptedAt;
    private Instant revokedAt;

    @Column(nullable = false, unique = true)
    private String token = UUID.randomUUID().toString();

    public Invitation() {}

    public Invitation(String teamId, String inviterUserId, String inviteeEmail) {
        this.teamId = teamId;
        this.inviterUserId = inviterUserId;
        this.inviteeEmail = inviteeEmail;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getInviterUserId() {
        return inviterUserId;
    }

    public void setInviterUserId(String inviterUserId) {
        this.inviterUserId = inviterUserId;
    }
    
    public String getInviterFirstName() {
        return inviterFirstName;
    }
    
    public void setInviterFirstName(String inviterFirstName) {
        this.inviterFirstName = inviterFirstName;
    }
    
    public String getInviterLastName() {
        return inviterLastName;
    }
    
    public void setInviterLastName(String inviterLastName) {
        this.inviterLastName = inviterLastName;
    }
    
    public String getInviterEmail() {
        return inviterEmail;
    }
    
    public void setInviterEmail(String inviterEmail) {
        this.inviterEmail = inviterEmail;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastSentAt() {
        return lastSentAt;
    }
    
    public void setLastSentAt(Instant lastSentAt) {
        this.lastSentAt = lastSentAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
    
    public Instant getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
