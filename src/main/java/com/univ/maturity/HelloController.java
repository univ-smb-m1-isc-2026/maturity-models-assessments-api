package com.univ.maturity;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class HelloController {

    private final UserRepository userRepository;

    public HelloController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/hello")
    public Map<String, Object> hello() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("db_status", "Connecté - Nombre d'utilisateurs : " + userRepository.count());
        
        response.put("message", "Bonjour depuis l'API Spring Boot et PostgreSQL !");
        return response;
    }
}
