package com.univ.maturity.payload.response;

public class TwoAFAResponse {
    private String secret;
    private String otpAuthUrl;

    public TwoAFAResponse(String secret, String otpAuthUrl) {
        this.secret = secret;
        this.otpAuthUrl = otpAuthUrl;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getOtpAuthUrl() {
        return otpAuthUrl;
    }

    public void setOtpAuthUrl(String otpAuthUrl) {
        this.otpAuthUrl = otpAuthUrl;
    }
}
