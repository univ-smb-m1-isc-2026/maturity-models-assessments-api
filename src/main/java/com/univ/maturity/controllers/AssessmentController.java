package com.univ.maturity.controllers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univ.maturity.Answer;
import com.univ.maturity.Assessment;
import com.univ.maturity.AssessmentRepository;
import com.univ.maturity.ERole;
import com.univ.maturity.MaturityModel;
import com.univ.maturity.MaturityModelRepository;
import com.univ.maturity.Submission;
import com.univ.maturity.Team;
import com.univ.maturity.TeamMember;
import com.univ.maturity.TeamMemberRepository;
import com.univ.maturity.TeamRepository;
import com.univ.maturity.payload.request.StartAssessmentRequest;
import com.univ.maturity.payload.response.MessageResponse;
import com.univ.maturity.security.services.UserDetailsImpl;

import jakarta.validation.Valid;

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
    public ResponseEntity<?> startAssessment(@Valid @RequestBody StartAssessmentRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(request.getTeamId()));
        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        Optional<TeamMember> memberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), request.getTeamId());
        boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
        boolean isPMOorLeader = memberOpt.isPresent() && (memberOpt.get().getRoles().contains(ERole.ROLE_PMO) || memberOpt.get().getRoles().contains(ERole.ROLE_TEAM_LEADER));
        
        if (!isOwner && !isPMOorLeader) {
             return ResponseEntity.status(403).body(new MessageResponse("Error: You must be the Team Owner, PMO or Leader to start an assessment."));
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
    public ResponseEntity<?> getTeamAssessments(@PathVariable String teamId) {
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(teamId));
        if (teamOpt.isEmpty()) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<TeamMember> memberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), teamId);
        boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
        
        if (memberOpt.isEmpty() && !isOwner) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You are not a member of this team."));
        }
        
        List<Assessment> assessments = assessmentRepository.findByTeam(teamOpt.get());
        return ResponseEntity.ok(assessments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAssessment(@PathVariable String id) {
        Optional<Assessment> assessmentOpt = assessmentRepository.findById(Objects.requireNonNull(id));
        if (assessmentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Assessment not found."));
        }
        Assessment assessment = assessmentOpt.get();
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String teamId = Objects.requireNonNull(assessment.getTeam().getId());
        Optional<TeamMember> memberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), teamId);
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        boolean isOwner = teamOpt.isPresent() && teamOpt.get().getOwner().getId().equals(userDetails.getId());

        if (memberOpt.isEmpty() && !isOwner) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You are not a member of the team associated with this assessment."));
        }
        
        return ResponseEntity.ok(assessmentOpt.get());
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<?> submitAssessment(@PathVariable String id, @RequestBody List<Answer> answers) {
        Optional<Assessment> assessmentOpt = assessmentRepository.findById(Objects.requireNonNull(id));
        if (assessmentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Assessment not found."));
        }

        Assessment assessment = assessmentOpt.get();
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String teamId = Objects.requireNonNull(assessment.getTeam().getId());
        Optional<TeamMember> memberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), teamId);
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        boolean isOwner = teamOpt.isPresent() && teamOpt.get().getOwner().getId().equals(userDetails.getId());

        if (memberOpt.isEmpty() && !isOwner) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You are not a member of this team."));
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