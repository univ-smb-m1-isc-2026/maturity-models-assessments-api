package com.univ.maturity.controllers;

import com.univ.maturity.ERole;
import com.univ.maturity.Team;
import com.univ.maturity.TeamMember;
import com.univ.maturity.TeamMemberRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TeamMemberRepository teamMemberRepository;

    @GetMapping
    @SuppressWarnings("null")
    public ResponseEntity<?> getUserTeams() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(Objects.requireNonNull(userDetails.getId())).orElse(null);
        
        if (user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }

        List<TeamMember> memberships = teamMemberRepository.findByUserId(user.getId());
        List<String> teamIds = memberships.stream()
                .map(TeamMember::getTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        List<Team> teams = teamRepository.findAllById(teamIds);

        for (Team team : teams) {
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(team.getId());
            List<String> memberIds = teamMembers.stream()
                    .map(TeamMember::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<User> members = userRepository.findAllById(memberIds);
            team.setMembers(members);
        }

        return ResponseEntity.ok(teams);
    }

    @PostMapping
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

        TeamMember teamMember = new TeamMember(user.getId(), team.getId(), ERole.ROLE_TEAM_LEADER);
        teamMemberRepository.save(teamMember);

        return ResponseEntity.ok(new MessageResponse("Team created successfully!"));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<?> inviteMember(@PathVariable String id, @Valid @RequestBody InviteMemberRequest inviteRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(id));

        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }

        Team team = teamOpt.get();

        Optional<TeamMember> requesterMemberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), team.getId());
        
        if (requesterMemberOpt.isEmpty()) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: You are not a member of this team."));
        }

        TeamMember requesterMember = requesterMemberOpt.get();
        if (requesterMember.getRole() != ERole.ROLE_TEAM_LEADER && requesterMember.getRole() != ERole.ROLE_PMO) {
             return ResponseEntity.badRequest().body(new MessageResponse("Error: Only the team leader can invite members."));
        }

        User userToInvite = userRepository.findByEmail(inviteRequest.getEmail()).orElse(null);
        if (userToInvite == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User with this email not found."));
        }
        
        Optional<TeamMember> existingMember = teamMemberRepository.findByUserIdAndTeamId(userToInvite.getId(), team.getId());
        if (existingMember.isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User is already a member of this team."));
        }

        TeamMember newMember = new TeamMember(userToInvite.getId(), team.getId(), ERole.ROLE_TEAM_MEMBER);
        teamMemberRepository.save(newMember);

        return ResponseEntity.ok(new MessageResponse("User added to team successfully!"));
    }
}