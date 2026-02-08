package com.univ.maturity;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Question {
    @NotBlank
    private String text;

    @Size(min = 5, max = 5)
    @Valid
    private List<Level> levels = new ArrayList<>();

    public Question() {}

    public Question(String text, List<Level> levels) {
        this.text = text;
        this.levels = levels;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public void setLevels(List<Level> levels) {
        this.levels = levels;
    }
}
