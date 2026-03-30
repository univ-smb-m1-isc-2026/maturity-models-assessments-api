package com.univ.maturity;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "assessments")
public class Assessment {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(optional = false)
    @JoinColumn(name = "maturity_model_id", nullable = false)
    private MaturityModel maturityModel;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(columnDefinition = "text", nullable = false)
    @Convert(converter = SubmissionsConverter.class)
    private List<Submission> submissions = new ArrayList<>();

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

    public List<Submission> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<Submission> submissions) {
        this.submissions = submissions;
    }

    @Converter
    public static class SubmissionsConverter implements AttributeConverter<List<Submission>, String> {
        private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
        private static final TypeReference<List<Submission>> TYPE = new TypeReference<>() {};

        @Override
        public String convertToDatabaseColumn(List<Submission> attribute) {
            try {
                if (attribute == null) return "[]";
                return MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public List<Submission> convertToEntityAttribute(String dbData) {
            try {
                if (dbData == null || dbData.isBlank()) return new ArrayList<>();
                return MAPPER.readValue(dbData, TYPE);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
