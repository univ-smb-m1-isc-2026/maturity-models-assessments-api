package com.univ.maturity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "teams")
public class Team {
    @Id
    private String id;

    private String name;

    @DBRef
    private User owner;

    @DBRef
    private Set<User> members = new HashSet<>();

    public Team() {}

    public Team(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.members.add(owner);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }
}
