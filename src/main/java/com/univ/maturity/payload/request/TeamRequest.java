package com.univ.maturity.payload.request;

import jakarta.validation.constraints.NotBlank;

public class TeamRequest {
    @NotBlank
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
