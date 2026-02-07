package com.univ.maturity.payload.request;

public class Enable2FARequest {
    private String secret;
    private String code;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
