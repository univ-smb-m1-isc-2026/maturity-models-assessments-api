package com.univ.maturity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "assessments")
public class Assessment {
    @Id
    private String id;

    @DBRef
    private Team team;

    @DBRef
    private MaturityModel maturityModel;

    private LocalDateTime date;

    private List<Answer> answers = new ArrayList<>();

    public Assessment() {}

    public Assessment(Team team, MaturityModel maturityModel) {
        this.team = team;
        this.maturityModel = maturityModel;
        this.date = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public MaturityModel getMaturityModel() {
        return maturityModel;
    }

    public void setMaturityModel(MaturityModel maturityModel) {
        this.maturityModel = maturityModel;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }
}
