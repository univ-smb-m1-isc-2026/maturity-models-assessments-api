package com.univ.maturity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Submission {
    private String userId;
    private List<Answer> answers = new ArrayList<>();
    private LocalDateTime date;

    public Submission() {}

    public Submission(String userId, List<Answer> answers) {
        this.userId = userId;
        this.answers = answers;
        this.date = LocalDateTime.now();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
