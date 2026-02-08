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

import com.univ.maturity.payload.request.UpdateUserRolesRequest;
import java.util.HashSet;
import java.util.Set;

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

        TeamMember teamMember = new TeamMember(user.getId(), team.getId(), ERole.ROLE_PMO);
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
        
        boolean isPMO = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PMO"));

        if (!isPMO) {
            if (requesterMemberOpt.isEmpty()) {
                 return ResponseEntity.badRequest().body(new MessageResponse("Error: You are not a member of this team."));
            }
    
            TeamMember requesterMember = requesterMemberOpt.get();
            if (requesterMember.getRole() != ERole.ROLE_TEAM_LEADER && requesterMember.getRole() != ERole.ROLE_PMO) {
                 return ResponseEntity.badRequest().body(new MessageResponse("Error: Only the team leader can invite members."));
            }
        }

        User userToInvite = userRepository.findByEmail(inviteRequest.getEmail()).orElse(null);
        if (userToInvite == null) {
            String invitationLink = "http://localhost:5173/register?role=user&teamId=" + team.getId() + "&email=" + inviteRequest.getEmail();
            emailService.sendInvitationEmail(inviteRequest.getEmail(), team.getName(), invitationLink);
            return ResponseEntity.ok(new MessageResponse("User not found, invitation email sent!"));
        }
        
        Optional<TeamMember> existingMember = teamMemberRepository.findByUserIdAndTeamId(userToInvite.getId(), team.getId());
        if (existingMember.isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User is already a member of this team."));
        }

        TeamMember newMember = new TeamMember(userToInvite.getId(), team.getId(), ERole.ROLE_TEAM_MEMBER);
        teamMemberRepository.save(newMember);

        return ResponseEntity.ok(new MessageResponse("User added to team successfully!"));
    }

    @PutMapping("/{id}/members/{userId}/roles")
    public ResponseEntity<?> updateMemberRoles(@PathVariable String id, @PathVariable String userId, @Valid @RequestBody UpdateUserRolesRequest rolesRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(id));

        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        boolean isPMO = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PMO"));
        boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
        
        if (!isPMO && !isOwner) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You do not have permission to update roles."));
        }

        Optional<User> memberUserOpt = userRepository.findById(Objects.requireNonNull(userId));
        if (memberUserOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }
        User memberUser = memberUserOpt.get();

        Optional<TeamMember> memberShipOpt = teamMemberRepository.findByUserIdAndTeamId(userId, id);
        if (memberShipOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User is not a member of this team."));
        }

        Set<String> strRoles = rolesRequest.getRoles();
        Set<ERole> roles = new HashSet<>();

        if (strRoles != null) {
            strRoles.forEach(role -> {
                switch (role) {
                    case "pmo":
                        roles.add(ERole.ROLE_PMO);
                        break;
                    case "leader":
                        roles.add(ERole.ROLE_TEAM_LEADER);
                        break;
                    default:
                        roles.add(ERole.ROLE_TEAM_MEMBER);
                }
            });
        }

        memberUser.setRoles(roles);
        userRepository.save(memberUser);

        TeamMember memberShip = memberShipOpt.get();
        if (roles.contains(ERole.ROLE_PMO)) {
            memberShip.setRole(ERole.ROLE_PMO);
        } else if (roles.contains(ERole.ROLE_TEAM_LEADER)) {
            memberShip.setRole(ERole.ROLE_TEAM_LEADER);
        } else {
            memberShip.setRole(ERole.ROLE_TEAM_MEMBER);
        }
        teamMemberRepository.save(memberShip);

        return ResponseEntity.ok(new MessageResponse("User roles updated successfully!"));
    }
}