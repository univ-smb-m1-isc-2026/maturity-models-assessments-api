package com.univ.maturity;

import java.io.IOException;
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
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name = "maturity_models")
public class MaturityModel {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 36)
    private String teamId;

    @NotEmpty
    @Valid
    @Column(columnDefinition = "text", nullable = false)
    @Convert(converter = QuestionsConverter.class)
    private List<Question> questions = new ArrayList<>();

    public MaturityModel() {}

    public MaturityModel(String name) {
        this.name = name;
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

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    @Converter
    public static class QuestionsConverter implements AttributeConverter<List<Question>, String> {
        private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
        private static final TypeReference<List<Question>> TYPE = new TypeReference<>() {};

        @Override
        public String convertToDatabaseColumn(List<Question> attribute) {
            try {
                if (attribute == null) return "[]";
                return MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public List<Question> convertToEntityAttribute(String dbData) {
            try {
                if (dbData == null || dbData.isBlank()) return new ArrayList<>();
                return MAPPER.readValue(dbData, TYPE);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
