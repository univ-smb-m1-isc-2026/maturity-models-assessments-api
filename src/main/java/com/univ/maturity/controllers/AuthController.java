package com.univ.maturity.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.univ.maturity.InvitationRepository;
import com.univ.maturity.InvitationStatus;
import com.univ.maturity.TeamMemberRepository;
import com.univ.maturity.TeamRepository;
import com.univ.maturity.User;
import com.univ.maturity.UserRepository;
import com.univ.maturity.VerificationToken;
import com.univ.maturity.VerificationTokenRepository;
import com.univ.maturity.payload.request.Enable2FARequest;
import com.univ.maturity.payload.request.LoginRequest;
import com.univ.maturity.payload.request.SignupRequest;
import com.univ.maturity.payload.request.VerifyRequest;
import com.univ.maturity.payload.response.JwtResponse;
import com.univ.maturity.payload.response.MessageResponse;
import com.univ.maturity.payload.response.TwoAFAResponse;
import com.univ.maturity.security.jwt.JwtUtils;
import com.univ.maturity.security.services.UserDetailsImpl;
import com.univ.maturity.services.EmailService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TeamMemberRepository teamMemberRepository;
    
    @Autowired
    InvitationRepository invitationRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    VerificationTokenRepository verificationTokenRepository;

    @Autowired
    EmailService emailService;

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @PostMapping("/signin")
    @SuppressWarnings("null")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        User existingUser = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
        if (existingUser != null && !existingUser.isEnabled()) {
            return ResponseEntity.status(403).body(new MessageResponse("EMAIL_NOT_VERIFIED"));
        }

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

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

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

        if (signUpRequest.getTeamId() != null && !signUpRequest.getTeamId().isEmpty()) {
            try {
                String teamId = Objects.requireNonNull(signUpRequest.getTeamId());
                com.univ.maturity.Team team = teamRepository.findById(teamId).orElse(null);
                if (team != null) {
                    com.univ.maturity.TeamMember member = new com.univ.maturity.TeamMember(user, team, com.univ.maturity.ERole.ROLE_TEAM_MEMBER);
                    teamMemberRepository.save(member);
                }
                
                invitationRepository.findByInviteeEmailAndTeamIdAndStatus(user.getEmail(), teamId, InvitationStatus.PENDING)
                    .ifPresent(inv -> {
                        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(java.time.Instant.now())) {
                            inv.setStatus(InvitationStatus.EXPIRED);
                            invitationRepository.save(inv);
                            return;
                        }
                        inv.setStatus(InvitationStatus.ACCEPTED);
                        inv.setAcceptedAt(java.time.Instant.now());
                        inv.setToken(java.util.UUID.randomUUID().toString());
                        invitationRepository.save(inv);
                    });
            } catch (Exception e) {
                System.err.println("Failed to auto-join team: " + e.getMessage());
            }
        }

        VerificationToken verificationToken = new VerificationToken(user.getId());
        verificationTokenRepository.save(verificationToken);

        try {
            boolean sent = emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());
            if (sent) {
                return ResponseEntity.ok(new MessageResponse("User registered successfully! Please check your email for the verification code."));
            }
            return ResponseEntity.ok(new MessageResponse("User registered successfully! Email sending is disabled; check server logs for the verification code."));
        } catch (Exception e) {
            verificationTokenRepository.delete(verificationToken);
            userRepository.delete(user);
            return ResponseEntity.internalServerError().body(new MessageResponse("Error: Unable to send verification email. Please try again later."));
        }
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
    
    @PostMapping("/verify/resend")
    public ResponseEntity<?> resendVerification(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found!"));
        }
        if (user.isEnabled()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User already verified!"));
        }
        
        VerificationToken existing = verificationTokenRepository.findByUserId(user.getId());
        if (existing != null) {
            verificationTokenRepository.delete(existing);
        }
        
        VerificationToken verificationToken = new VerificationToken(user.getId());
        verificationTokenRepository.save(verificationToken);
        
        try {
            boolean sent = emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());
            if (sent) {
                return ResponseEntity.ok(new MessageResponse("Verification email sent! Please check your inbox."));
            }
            return ResponseEntity.ok(new MessageResponse("Verification code generated (email sending disabled)."));
        } catch (Exception e) {
            verificationTokenRepository.delete(verificationToken);
            return ResponseEntity.internalServerError().body(new MessageResponse("Error: Unable to send verification email. Please try again later."));
        }
    }
}
