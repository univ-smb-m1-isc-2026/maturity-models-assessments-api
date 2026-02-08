package com.univ.maturity.controllers;

import com.univ.maturity.*;
import com.univ.maturity.payload.request.StartAssessmentRequest;
import com.univ.maturity.payload.response.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.univ.maturity.security.services.UserDetailsImpl;
import org.springframework.security.core.context.SecurityContextHolder;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    @Autowired
    AssessmentRepository assessmentRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    MaturityModelRepository maturityModelRepository;

    @Autowired
    TeamMemberRepository teamMemberRepository;

    @PostMapping("/start")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('PMO')")
    public ResponseEntity<?> startAssessment(@Valid @RequestBody StartAssessmentRequest request) {
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(request.getTeamId()));
        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }

        Optional<MaturityModel> modelOpt = maturityModelRepository.findById(Objects.requireNonNull(request.getMaturityModelId()));
        if (modelOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Maturity Model not found."));
        }

        Assessment assessment = new Assessment(teamOpt.get(), modelOpt.get());
        assessmentRepository.save(assessment);
        return ResponseEntity.ok(assessment);
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('TEAM_LEADER') or hasRole('PMO')")
    public ResponseEntity<?> getTeamAssessments(@PathVariable String teamId) {
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(teamId));
        if (teamOpt.isEmpty()) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        List<Assessment> assessments = assessmentRepository.findByTeam(teamOpt.get());
        return ResponseEntity.ok(assessments);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('TEAM_LEADER') or hasRole('PMO')")
    public ResponseEntity<?> getAssessment(@PathVariable String id) {
        Optional<Assessment> assessmentOpt = assessmentRepository.findById(Objects.requireNonNull(id));
        if (assessmentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Assessment not found."));
        }
        return ResponseEntity.ok(assessmentOpt.get());
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('TEAM_LEADER') or hasRole('PMO')")
    public ResponseEntity<?> submitAssessment(@PathVariable String id, @RequestBody List<Answer> answers) {
        Optional<Assessment> assessmentOpt = assessmentRepository.findById(Objects.requireNonNull(id));
        if (assessmentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Assessment not found."));
        }

        Assessment assessment = assessmentOpt.get();
        
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isPMO = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PMO"));
        
        if (!isPMO) {
            Optional<TeamMember> membership = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), assessment.getTeam().getId());
            if (membership.isEmpty()) {
                return ResponseEntity.status(403).body(new MessageResponse("Error: You are not a member of this team."));
            }
        }

        List<Submission> submissions = assessment.getSubmissions();
        Optional<Submission> existingSubmission = submissions.stream()
            .filter(s -> s.getUserId().equals(userDetails.getId()))
            .findFirst();

        if (existingSubmission.isPresent()) {
            existingSubmission.get().setAnswers(answers);
            existingSubmission.get().setDate(java.time.LocalDateTime.now());
        } else {
            Submission newSubmission = new Submission(userDetails.getId(), answers);
            submissions.add(newSubmission);
        }

        assessment.setSubmissions(submissions);
        assessment.setDate(java.time.LocalDateTime.now()); 
        
        assessmentRepository.save(assessment);
        
        return ResponseEntity.ok(assessment);
    }
}
