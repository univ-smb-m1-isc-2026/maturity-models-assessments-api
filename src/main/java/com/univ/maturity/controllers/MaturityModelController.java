package com.univ.maturity.controllers;

import com.univ.maturity.MaturityModel;
import com.univ.maturity.MaturityModelRepository;
import com.univ.maturity.payload.response.MessageResponse;
import com.univ.maturity.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/models")
public class MaturityModelController {

    @Autowired
    MaturityModelRepository maturityModelRepository;

    @Autowired
    com.univ.maturity.TeamRepository teamRepository;

    @GetMapping
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('TEAM_LEADER') or hasRole('PMO')")
    public List<MaturityModel> getAllModels() {
        return maturityModelRepository.findAll();
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('TEAM_LEADER') or hasRole('PMO')")
    public List<MaturityModel> getModelsByTeam(@PathVariable String teamId) {
        return maturityModelRepository.findByTeamId(teamId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('TEAM_LEADER') or hasRole('PMO')")
    public ResponseEntity<?> getModelById(@PathVariable String id) {
        Optional<MaturityModel> model = maturityModelRepository.findById(Objects.requireNonNull(id));
        if (model.isPresent()) {
            return ResponseEntity.ok(model.get());
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Model not found!"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('PMO') or hasRole('TEAM_LEADER')")
    public ResponseEntity<?> createModel(@Valid @RequestBody MaturityModel maturityModel) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        boolean isPMO = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PMO"));
        
        if (!isPMO) {
             
             if (maturityModel.getTeamId() == null) {
                 return ResponseEntity.badRequest().body(new MessageResponse("Error: Team ID is required."));
             }
             
             Optional<com.univ.maturity.Team> teamOpt = teamRepository.findById(Objects.requireNonNull(maturityModel.getTeamId()));
             if (teamOpt.isEmpty()) {
                 return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
             }
             
             if (!teamOpt.get().getOwner().getId().equals(userDetails.getId())) {
                 return ResponseEntity.status(403).body(new MessageResponse("Error: You must be the Team Owner to create a model for this team."));
             }
        }

        if (maturityModelRepository.existsByName(maturityModel.getName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Model name already exists!"));
        }

        maturityModelRepository.save(maturityModel);
        return ResponseEntity.ok(new MessageResponse("Maturity Model created successfully!"));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PMO')")
    public ResponseEntity<?> updateModel(@PathVariable String id, @Valid @RequestBody MaturityModel maturityModelRequest) {
        Optional<MaturityModel> modelOpt = maturityModelRepository.findById(Objects.requireNonNull(id));
        
        if (modelOpt.isEmpty()) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Model not found!"));
        }
        
        MaturityModel model = modelOpt.get();
        model.setName(maturityModelRequest.getName());
        model.setQuestions(maturityModelRequest.getQuestions());
        
        maturityModelRepository.save(model);
        
        return ResponseEntity.ok(new MessageResponse("Maturity Model updated successfully!"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PMO')")
    public ResponseEntity<?> deleteModel(@PathVariable String id) {
        if (!maturityModelRepository.existsById(Objects.requireNonNull(id))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Model not found!"));
        }
        maturityModelRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Maturity Model deleted successfully!"));
    }
}