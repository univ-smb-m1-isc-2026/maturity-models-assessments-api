package com.univ.maturity.payload.request;

import jakarta.validation.constraints.NotBlank;

public class StartAssessmentRequest {
    @NotBlank
    private String teamId;

    @NotBlank
    private String maturityModelId;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getMaturityModelId() {
        return maturityModelId;
    }

    public void setMaturityModelId(String maturityModelId) {
        this.maturityModelId = maturityModelId;
    }
}
