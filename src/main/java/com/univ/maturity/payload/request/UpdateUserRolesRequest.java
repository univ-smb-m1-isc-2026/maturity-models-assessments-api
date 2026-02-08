package com.univ.maturity.payload.request;

import java.util.Set;
import jakarta.validation.constraints.NotEmpty;

public class UpdateUserRolesRequest {
    @NotEmpty
    private Set<String> roles;

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
