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
import com.univ.maturity.User;
import com.univ.maturity.UserRepository;
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

    @Autowired
    UserRepository userRepository;

    @GetMapping
    public List<MaturityModel> getAllModels() {
        return maturityModelRepository.findAll();
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<?> getModelsByTeam(@PathVariable String teamId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<TeamMember> memberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), teamId);
        
        Optional<com.univ.maturity.Team> teamOpt = teamRepository.findById(Objects.requireNonNull(teamId));
        boolean isOwner = teamOpt.isPresent() && teamOpt.get().getOwner().getId().equals(userDetails.getId());

        if (memberOpt.isEmpty() && !isOwner) {
            return ResponseEntity.status(403).body(new MessageResponse("Erreur : vous n'êtes pas membre de cette équipe."));
        }
        return ResponseEntity.ok(maturityModelRepository.findByTeamIdOrTeamIdIsNull(teamId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getModelById(@PathVariable String id) {
        Optional<MaturityModel> modelOpt = maturityModelRepository.findById(Objects.requireNonNull(id));
        if (modelOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Erreur : modèle introuvable !"));
        }
        MaturityModel model = modelOpt.get();
        
        if (model.getTeamId() != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Optional<TeamMember> memberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), model.getTeamId());
            Optional<com.univ.maturity.Team> teamOpt = teamRepository.findById(Objects.requireNonNull(model.getTeamId()));
            boolean isOwner = teamOpt.isPresent() && teamOpt.get().getOwner().getId().equals(userDetails.getId());

            if (memberOpt.isEmpty() && !isOwner) {
                return ResponseEntity.status(403).body(new MessageResponse("Erreur : vous n'êtes pas membre de l'équipe à laquelle ce modèle appartient."));
            }
        }
        
        return ResponseEntity.ok(model);
    }

    @PostMapping
    public ResponseEntity<?> createModel(@Valid @RequestBody MaturityModel maturityModel) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User requester = userRepository.findById(Objects.requireNonNull(userDetails.getId())).orElse(null);
        if (requester == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }
        if (!isProfilePMO(requester)) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: Only PMO profile can create maturity models."));
        }

        String teamId = maturityModel.getTeamId();
        if (teamId != null && teamId.isBlank()) {
            maturityModel.setTeamId(null);
            teamId = null;
        }

        if (teamId != null) {
            Optional<com.univ.maturity.Team> teamOpt = teamRepository.findById(teamId);
            if (teamOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Erreur : équipe introuvable."));
            }

            boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
            Optional<TeamMember> memberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), teamId);
            boolean isPMO = memberOpt.isPresent() && memberOpt.get().getRoles().contains(ERole.ROLE_PMO);

            if (!isOwner && !isPMO) {
                return ResponseEntity.status(403).body(new MessageResponse("Erreur : vous devez être le propriétaire de l'équipe ou PMO pour créer un modèle pour cette équipe."));
            }
        }

        if (maturityModelRepository.existsByName(maturityModel.getName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Erreur : le nom du modèle existe déjà !"));
        }

        maturityModelRepository.save(maturityModel);
        return ResponseEntity.ok(new MessageResponse("Modèle de maturité créé avec succès !"));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateModel(@PathVariable String id, @Valid @RequestBody MaturityModel maturityModelRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User requester = userRepository.findById(Objects.requireNonNull(userDetails.getId())).orElse(null);
        if (requester == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }
        if (!isProfilePMO(requester)) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: Only PMO profile can update maturity models."));
        }
        Optional<MaturityModel> modelOpt = maturityModelRepository.findById(Objects.requireNonNull(id));
        
        if (modelOpt.isEmpty()) {
               return ResponseEntity.badRequest().body(new MessageResponse("Erreur : modèle introuvable !"));
        }
        
        MaturityModel model = modelOpt.get();
        if (model.getTeamId() != null) {
             Optional<com.univ.maturity.Team> modelTeamOpt = teamRepository.findById(Objects.requireNonNull(model.getTeamId()));
             boolean isOwner = modelTeamOpt.isPresent() && modelTeamOpt.get().getOwner().getId().equals(userDetails.getId());
             Optional<TeamMember> memberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), model.getTeamId());
             boolean isPMO = memberOpt.isPresent() && memberOpt.get().getRoles().contains(ERole.ROLE_PMO);
             
             if (!isOwner && !isPMO) {
                 return ResponseEntity.status(403).body(new MessageResponse("Erreur : seul le propriétaire de l'équipe ou le PMO peut modifier ce modèle."));
             }
        }
        
        model.setName(maturityModelRequest.getName());
        model.setQuestions(maturityModelRequest.getQuestions());
        
        maturityModelRepository.save(model);
        
        return ResponseEntity.ok(new MessageResponse("Modèle de maturité mis à jour avec succès !"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteModel(@PathVariable String id) {
        Optional<MaturityModel> modelOpt = maturityModelRepository.findById(Objects.requireNonNull(id));
        if (modelOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Erreur : modèle introuvable !"));
        }
        
        MaturityModel model = modelOpt.get();
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User requester = userRepository.findById(Objects.requireNonNull(userDetails.getId())).orElse(null);
        if (requester == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }
        if (!isProfilePMO(requester)) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: Only PMO profile can delete maturity models."));
        }
        
        if (model.getTeamId() != null) {
            Optional<com.univ.maturity.Team> modelTeamOpt = teamRepository.findById(Objects.requireNonNull(model.getTeamId()));
            boolean isOwner = modelTeamOpt.isPresent() && modelTeamOpt.get().getOwner().getId().equals(userDetails.getId());
            Optional<TeamMember> memberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), model.getTeamId());
            boolean isPMO = memberOpt.isPresent() && memberOpt.get().getRoles().contains(ERole.ROLE_PMO);
            
            if (!isOwner && !isPMO) {
                return ResponseEntity.status(403).body(new MessageResponse("Erreur : seul le propriétaire de l'équipe ou le PMO peut supprimer ce modèle."));
            }
        }
        
        maturityModelRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Modèle de maturité supprimé avec succès !"));
    }

    private boolean isProfileMemberOnly(User user) {
        java.util.Set<ERole> profileRoles = user.getRoles();
        boolean hasManagementProfile = profileRoles.contains(ERole.ROLE_PMO) || profileRoles.contains(ERole.ROLE_TEAM_LEADER);
        return profileRoles.contains(ERole.ROLE_TEAM_MEMBER) && !hasManagementProfile;
    }

    private boolean isProfilePMO(User user) {
        return user.getRoles().contains(ERole.ROLE_PMO);
    }
}
