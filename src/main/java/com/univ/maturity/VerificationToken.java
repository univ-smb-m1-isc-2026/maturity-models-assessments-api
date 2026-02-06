package com.univ.maturity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "verification_tokens")
public class VerificationToken {
    @Id
    private String id;
    
    private String token;
    
    private String userId;
    
    private LocalDateTime expiryDate;

    public VerificationToken() {}

    public VerificationToken(String userId) {
        this.userId = userId;
        this.token = String.valueOf((int) ((Math.random() * 900000) + 100000));
        this.expiryDate = calculateExpiryDate(10);
    }

    private LocalDateTime calculateExpiryDate(int expiryTimeInMinutes) {
        return LocalDateTime.now().plusMinutes(expiryTimeInMinutes);
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
}
