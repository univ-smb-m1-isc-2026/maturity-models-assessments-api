package com.univ.maturity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "team_members")
public class TeamMember {
    @Id
    private String id;

    private String userId;
    private String teamId;
    private Set<ERole> roles = new HashSet<>();

    public TeamMember() {}

    public TeamMember(String userId, String teamId, Set<ERole> roles) {
        this.userId = userId;
        this.teamId = teamId;
        this.roles = roles;
    }

    public TeamMember(String userId, String teamId, ERole role) {
        this.userId = userId;
        this.teamId = teamId;
        this.roles.add(role);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public Set<ERole> getRoles() {
        return roles;
    }

    public void setRoles(Set<ERole> roles) {
        this.roles = roles;
    }
}
