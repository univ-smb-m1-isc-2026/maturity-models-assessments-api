package com.univ.maturity;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {
    VerificationToken findByToken(String token);
    VerificationToken findByUserId(String userId);
}
