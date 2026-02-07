package com.univ.maturity.controllers;

import com.univ.maturity.User;
import com.univ.maturity.UserRepository;
import com.univ.maturity.payload.request.VerifyRequest;
import com.univ.maturity.payload.request.Enable2FARequest;
import com.univ.maturity.payload.request.LoginRequest;
import com.univ.maturity.payload.request.SignupRequest;
import com.univ.maturity.payload.response.JwtResponse;
import com.univ.maturity.payload.response.MessageResponse;
import com.univ.maturity.payload.response.TwoAFAResponse;
import com.univ.maturity.VerificationToken;
import com.univ.maturity.VerificationTokenRepository;
import com.univ.maturity.services.EmailService;
import com.univ.maturity.security.jwt.JwtUtils;
import com.univ.maturity.security.services.UserDetailsImpl;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    VerificationTokenRepository verificationTokenRepository;

    @Autowired
    EmailService emailService;

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @PostMapping("/signin")
    @SuppressWarnings("null")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElse(null);

        if (user != null && user.isUsing2FA()) {
            if (loginRequest.getCode() == null || loginRequest.getCode().isEmpty()) {
                 return ResponseEntity.status(403).body(new MessageResponse("2FA_REQUIRED"));
            }
            
            try {
                int code = Integer.parseInt(loginRequest.getCode());
                boolean isCodeValid = gAuth.authorize(user.getSecret2FA(), code);
                if (!isCodeValid) {
                    return ResponseEntity.status(401).body(new MessageResponse("Error: Invalid 2FA Code"));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.status(401).body(new MessageResponse("Error: Invalid 2FA Code format"));
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        List<String> roles = new ArrayList<>();

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getEmail(),
                roles,
                user != null && user.isUsing2FA()));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        User user = new User(signUpRequest.getEmail(),
                signUpRequest.getFirstName(),
                signUpRequest.getLastName(),
                encoder.encode(signUpRequest.getPassword()));

        userRepository.save(user);

        VerificationToken verificationToken = new VerificationToken(user.getId());
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());

        return ResponseEntity.ok(new MessageResponse("User registered successfully! Please check your email for the verification code."));
    }

    @PostMapping("/2fa/generate")
    public ResponseEntity<?> generate2FA() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        String otpAuthURL = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL("MaturityApp", userDetails.getEmail(), key);
        
        return ResponseEntity.ok(new TwoAFAResponse(secret, otpAuthURL));
    }
    
    @PostMapping("/2fa/enable")
    @SuppressWarnings("null")
    public ResponseEntity<?> enable2FA(@RequestBody Enable2FARequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElse(null);
        
        if (user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }

        try {
            int code = Integer.parseInt(request.getCode());
            boolean isCodeValid = gAuth.authorize(request.getSecret(), code);
            if (!isCodeValid) {
                 return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid 2FA Code"));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid 2FA Code format"));
        }
        
        user.setUsing2FA(true);
        user.setSecret2FA(request.getSecret());
        userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse("2FA Enabled successfully"));
    }
    
    @PostMapping("/2fa/disable")
    @SuppressWarnings("null")
    public ResponseEntity<?> disable2FA() {
         UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
         User user = userRepository.findById(userDetails.getId()).orElse(null);
         
         if (user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
         }

         user.setUsing2FA(false);
         user.setSecret2FA(null);
         userRepository.save(user);
         
         return ResponseEntity.ok(new MessageResponse("2FA Disabled successfully"));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@Valid @RequestBody VerifyRequest verifyRequest) {
        if (!userRepository.existsByEmail(verifyRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found!"));
        }

        User user = userRepository.findByEmail(verifyRequest.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found!"));
        }
        VerificationToken verificationToken = verificationTokenRepository.findByUserId(user.getId());

        if (verificationToken == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: No token found for this user!"));
        }

        if (!verificationToken.getToken().equals(verifyRequest.getCode())) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid code!"));
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Code expired!"));
        }

        user.setEnabled(true);
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);

        return ResponseEntity.ok(new MessageResponse("User verified successfully!"));
    }
}
