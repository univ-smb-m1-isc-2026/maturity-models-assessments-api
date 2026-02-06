package com.univ.maturity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;

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
        
        if (userRepository.count() == 0) {
            userRepository.save(new User("test@example.com", "TestUser", "password123"));
            response.put("db_status", "Initialized with test user");
        } else {
            response.put("db_status", "Connected - Users count: " + userRepository.count());
        }
        
        response.put("message", "Hello World from Spring Boot API & MongoDB!");
        return response;
    }
}
