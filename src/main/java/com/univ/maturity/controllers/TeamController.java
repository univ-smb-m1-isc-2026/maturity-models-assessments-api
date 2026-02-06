package com.univ.maturity.controllers;

import com.univ.maturity.Team;
import com.univ.maturity.TeamRepository;
import com.univ.maturity.User;
import com.univ.maturity.UserRepository;
import com.univ.maturity.payload.request.InviteMemberRequest;
import com.univ.maturity.payload.request.TeamRequest;
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
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('TEAM_LEADER') or hasRole('PMO')")
    public ResponseEntity<?> getUserTeams() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(Objects.requireNonNull(userDetails.getId())).orElse(null);
        
        if (user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }

        List<Team> teams = teamRepository.findByMembersContaining(user);
        return ResponseEntity.ok(teams);
    }

    @PostMapping
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('PMO')")
    public ResponseEntity<?> createTeam(@Valid @RequestBody TeamRequest teamRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(Objects.requireNonNull(userDetails.getId())).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }

        if (teamRepository.existsByName(teamRequest.getName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team name already exists!"));
        }

        Team team = new Team(teamRequest.getName(), user);
        teamRepository.save(team);

        return ResponseEntity.ok(new MessageResponse("Team created successfully!"));
    }

    @PostMapping("/{id}/invite")
    @PreAuthorize("hasRole('TEAM_LEADER') or hasRole('PMO')")
    public ResponseEntity<?> inviteMember(@PathVariable String id, @Valid @RequestBody InviteMemberRequest inviteRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(id));

        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }

        Team team = teamOpt.get();

        if (!team.getOwner().getId().equals(userDetails.getId())) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Only the team owner can invite members."));
        }

        User userToInvite = userRepository.findByEmail(inviteRequest.getEmail()).orElse(null);
        if (userToInvite == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User with this email not found."));
        }

        team.getMembers().add(userToInvite);
        teamRepository.save(team);

        return ResponseEntity.ok(new MessageResponse("User added to team successfully!"));
    }
}
