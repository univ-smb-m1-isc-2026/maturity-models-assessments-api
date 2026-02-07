package com.univ.maturity;

import java.util.ArrayList;
import java.util.List;

public class Question {
    private String text;
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
