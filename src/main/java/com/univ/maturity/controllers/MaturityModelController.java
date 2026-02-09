package com.univ.maturity.controllers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univ.maturity.ERole;
import com.univ.maturity.MaturityModel;
import com.univ.maturity.MaturityModelRepository;
import com.univ.maturity.TeamMember;
import com.univ.maturity.TeamMemberRepository;
import com.univ.maturity.payload.response.MessageResponse;
import com.univ.maturity.security.services.UserDetailsImpl;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/models")
public class MaturityModelController {

    @Autowired
    MaturityModelRepository maturityModelRepository;

    @Autowired
    com.univ.maturity.TeamRepository teamRepository;

    @Autowired
    TeamMemberRepository teamMemberRepository;

    @GetMapping
    public List<MaturityModel> getAllModels() {
        return maturityModelRepository.findAll();
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<?> getModelsByTeam(@PathVariable String teamId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<TeamMember> memberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), teamId);
        
        Optional<com.univ.maturity.Team> teamOpt = teamRepository.findById(Objects.requireNonNull(teamId));
        boolean isOwner = teamOpt.isPresent() && teamOpt.get().getOwner().getId().equals(userDetails.getId());

        if (memberOpt.isEmpty() && !isOwner) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You are not a member of this team."));
        }
        return ResponseEntity.ok(maturityModelRepository.findByTeamId(teamId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getModelById(@PathVariable String id) {
        Optional<MaturityModel> modelOpt = maturityModelRepository.findById(Objects.requireNonNull(id));
        if (modelOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Model not found!"));
        }
        MaturityModel model = modelOpt.get();
        
        if (model.getTeamId() != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Optional<TeamMember> memberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), model.getTeamId());
            Optional<com.univ.maturity.Team> teamOpt = teamRepository.findById(Objects.requireNonNull(model.getTeamId()));
            boolean isOwner = teamOpt.isPresent() && teamOpt.get().getOwner().getId().equals(userDetails.getId());

            if (memberOpt.isEmpty() && !isOwner) {
                return ResponseEntity.status(403).body(new MessageResponse("Error: You are not a member of the team this model belongs to."));
            }
        }
        
        return ResponseEntity.ok(model);
    }

    @PostMapping
    public ResponseEntity<?> createModel(@Valid @RequestBody MaturityModel maturityModel) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (maturityModel.getTeamId() == null) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Team ID is required."));
        }
        
        Optional<com.univ.maturity.Team> teamOpt = teamRepository.findById(Objects.requireNonNull(maturityModel.getTeamId()));
        if (teamOpt.isEmpty()) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
        
        Optional<TeamMember> memberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), maturityModel.getTeamId());
        boolean isPMOorLeader = memberOpt.isPresent() && (memberOpt.get().getRoles().contains(ERole.ROLE_PMO) || memberOpt.get().getRoles().contains(ERole.ROLE_TEAM_LEADER));
        
        if (!isOwner && !isPMOorLeader) {
             return ResponseEntity.status(403).body(new MessageResponse("Error: You must be the Team Owner, PMO or Leader to create a model for this team."));
        }

        if (maturityModelRepository.existsByName(maturityModel.getName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Model name already exists!"));
        }

        maturityModelRepository.save(maturityModel);
        return ResponseEntity.ok(new MessageResponse("Maturity Model created successfully!"));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateModel(@PathVariable String id, @Valid @RequestBody MaturityModel maturityModelRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<MaturityModel> modelOpt = maturityModelRepository.findById(Objects.requireNonNull(id));
        
        if (modelOpt.isEmpty()) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Model not found!"));
        }
        
        MaturityModel model = modelOpt.get();
        if (model.getTeamId() != null) {
             Optional<com.univ.maturity.Team> teamOpt = teamRepository.findById(Objects.requireNonNull(model.getTeamId()));
             boolean isOwner = teamOpt.isPresent() && teamOpt.get().getOwner().getId().equals(userDetails.getId());
             Optional<TeamMember> memberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), model.getTeamId());
             boolean isPMO = memberOpt.isPresent() && memberOpt.get().getRoles().contains(ERole.ROLE_PMO);
             
             if (!isOwner && !isPMO) {
                 return ResponseEntity.status(403).body(new MessageResponse("Error: Only Team Owner or PMO can update this model."));
             }
        }
        
        model.setName(maturityModelRequest.getName());
        model.setQuestions(maturityModelRequest.getQuestions());
        
        maturityModelRepository.save(model);
        
        return ResponseEntity.ok(new MessageResponse("Maturity Model updated successfully!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteModel(@PathVariable String id) {
        Optional<MaturityModel> modelOpt = maturityModelRepository.findById(Objects.requireNonNull(id));
        if (modelOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Model not found!"));
        }
        
        MaturityModel model = modelOpt.get();
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (model.getTeamId() != null) {
            Optional<com.univ.maturity.Team> teamOpt = teamRepository.findById(Objects.requireNonNull(model.getTeamId()));
            boolean isOwner = teamOpt.isPresent() && teamOpt.get().getOwner().getId().equals(userDetails.getId());
            Optional<TeamMember> memberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), model.getTeamId());
            boolean isPMO = memberOpt.isPresent() && memberOpt.get().getRoles().contains(ERole.ROLE_PMO);
            
            if (!isOwner && !isPMO) {
                return ResponseEntity.status(403).body(new MessageResponse("Error: Only Team Owner or PMO can delete this model."));
            }
        }
        
        maturityModelRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Maturity Model deleted successfully!"));
    }
}