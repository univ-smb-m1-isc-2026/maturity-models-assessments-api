package com.univ.maturity.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.univ.maturity.ERole;
import com.univ.maturity.Team;
import com.univ.maturity.TeamMember;
import com.univ.maturity.TeamMemberRepository;
import com.univ.maturity.TeamRepository;
import com.univ.maturity.User;
import com.univ.maturity.UserRepository;
import com.univ.maturity.payload.request.InviteMemberRequest;
import com.univ.maturity.payload.request.TeamRequest;
import com.univ.maturity.payload.request.UpdateUserRolesRequest;
import com.univ.maturity.payload.response.MessageResponse;
import com.univ.maturity.security.services.UserDetailsImpl;

import jakarta.validation.Valid;

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

    @Autowired
    com.univ.maturity.services.EmailService emailService;

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
            
            for (User member : members) {
                teamMembers.stream()
                    .filter(tm -> tm.getUserId().equals(member.getId()))
                    .findFirst()
                    .ifPresent(tm -> member.setRoles(tm.getRoles()));
            }

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

        Set<ERole> roles = new HashSet<>();
        roles.add(ERole.ROLE_PMO);
        roles.add(ERole.ROLE_TEAM_LEADER);
        roles.add(ERole.ROLE_TEAM_MEMBER);
        
        TeamMember teamMember = new TeamMember(user.getId(), team.getId(), roles);
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
        if (!requesterMember.getRoles().contains(ERole.ROLE_TEAM_LEADER) && !requesterMember.getRoles().contains(ERole.ROLE_PMO)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Only the team leader or PMO can invite members."));
        }

        User userToInvite = userRepository.findByEmail(inviteRequest.getEmail()).orElse(null);
        if (userToInvite == null) {
            String invitationLink = "http://localhost:5173/register?teamId=" + team.getId() + "&email=" + inviteRequest.getEmail();
            emailService.sendInvitationEmail(inviteRequest.getEmail(), team.getName(), invitationLink);
            return ResponseEntity.ok(new MessageResponse("User not found, invitation email sent!"));
        }
        
        Optional<TeamMember> existingMember = teamMemberRepository.findByUserIdAndTeamId(userToInvite.getId(), team.getId());
        if (existingMember.isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User is already a member of this team."));
        }

        TeamMember newMember = new TeamMember(userToInvite.getId(), team.getId(), ERole.ROLE_TEAM_MEMBER);
        teamMemberRepository.save(newMember);

        String invitationLink = "http://localhost:5173/teams/" + team.getId();
        emailService.sendInvitationEmail(inviteRequest.getEmail(), team.getName(), invitationLink);

        return ResponseEntity.ok(new MessageResponse("User added to team successfully!"));
    }

    @PutMapping("/{id}/members/{userId}/roles")
    public ResponseEntity<?> updateMemberRoles(@PathVariable String id, @PathVariable String userId, @Valid @RequestBody UpdateUserRolesRequest rolesRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(id));

        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
        
        Optional<TeamMember> requesterMemberOpt = teamMemberRepository.findByUserIdAndTeamId(userDetails.getId(), id);
        boolean isPMO = requesterMemberOpt.isPresent() && requesterMemberOpt.get().getRoles().contains(ERole.ROLE_PMO);

        if (!isPMO && !isOwner) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You do not have permission to update roles."));
        }

        Optional<User> memberUserOpt = userRepository.findById(Objects.requireNonNull(userId));
        if (memberUserOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }
        
        Optional<TeamMember> memberShipOpt = teamMemberRepository.findByUserIdAndTeamId(userId, id);
        if (memberShipOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User is not a member of this team."));
        }

        Set<String> strRoles = rolesRequest.getRoles();
        Set<ERole> roles = new HashSet<>();

        if (strRoles != null) {
            strRoles.forEach(role -> {
                switch (role) {
                    case "pmo" -> roles.add(ERole.ROLE_PMO);
                    case "leader" -> roles.add(ERole.ROLE_TEAM_LEADER);
                    default -> roles.add(ERole.ROLE_TEAM_MEMBER);
                }
            });
        }

        TeamMember memberShip = memberShipOpt.get();
        memberShip.setRoles(roles);
        teamMemberRepository.save(memberShip);

        return ResponseEntity.ok(new MessageResponse("User roles updated successfully!"));
    }
}