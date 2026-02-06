package com.univ.maturity;

public class Answer {
    private String questionText;
    private int selectedLevel;
    private String comment;

    public Answer() {}

    public Answer(String questionText, int selectedLevel, String comment) {
        this.questionText = questionText;
        this.selectedLevel = selectedLevel;
        this.comment = comment;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public int getSelectedLevel() {
        return selectedLevel;
    }

    public void setSelectedLevel(int selectedLevel) {
        this.selectedLevel = selectedLevel;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
