package com.univ.maturity.controllers;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
import com.univ.maturity.Invitation;
import com.univ.maturity.InvitationRepository;
import com.univ.maturity.InvitationStatus;
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
    
    @Autowired
    InvitationRepository invitationRepository;
    
    @Value("${app.invitation.ttlHours:168}")
    private int invitationTtlHours;

    @GetMapping
    @SuppressWarnings("null")
    public ResponseEntity<?> getUserTeams() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(Objects.requireNonNull(userDetails.getId())).orElse(null);
        
        if (user == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }

        List<TeamMember> memberships = teamMemberRepository.findByUser_Id(user.getId());
        List<String> teamIds = memberships.stream()
                .map(m -> m.getTeam().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        List<Team> teams = teamRepository.findAllById(teamIds);

        for (Team team : teams) {
            List<TeamMember> teamMembers = teamMemberRepository.findByTeam_Id(team.getId());
            List<String> memberIds = teamMembers.stream()
                    .map(tm -> tm.getUser().getId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<User> members = userRepository.findAllById(memberIds);
            
            for (User member : members) {
                teamMembers.stream()
                    .filter(tm -> tm.getUser().getId().equals(member.getId()))
                    .findFirst()
                    .ifPresent(tm -> member.setTeamRoles(tm.getRoles()));
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

        Set<ERole> profileRoles = user.getRoles();
        boolean hasTeamCreationRole = profileRoles.contains(ERole.ROLE_PMO) || profileRoles.contains(ERole.ROLE_TEAM_LEADER);
        if (!hasTeamCreationRole) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: Team members cannot create a team. You must be invited to join one."));
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
        
        TeamMember teamMember = new TeamMember(user, team, roles);
        teamMemberRepository.save(teamMember);

        return ResponseEntity.ok(new MessageResponse("Team created successfully!"));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<?> inviteMember(@PathVariable String id, @Valid @RequestBody InviteMemberRequest inviteRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User requester = userRepository.findById(Objects.requireNonNull(userDetails.getId())).orElse(null);
        if (requester == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }
        if (isProfileMemberOnly(requester)) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: Team Member profiles cannot invite members."));
        }
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(id));

        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }

        Team team = teamOpt.get();

        Optional<TeamMember> requesterMemberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), team.getId());
        
        if (requesterMemberOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: You are not a member of this team."));
        }

        TeamMember requesterMember = requesterMemberOpt.get();
        if (!requesterMember.getRoles().contains(ERole.ROLE_TEAM_LEADER) && !requesterMember.getRoles().contains(ERole.ROLE_PMO)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Only the team leader or PMO can invite members."));
        }

        Optional<Invitation> existingPending = invitationRepository.findByInviteeEmailAndTeamIdAndStatus(inviteRequest.getEmail(), team.getId(), InvitationStatus.PENDING);
        if (existingPending.isPresent()) {
            Invitation existing = existingPending.get();
            if (isExpired(existing)) {
                existing.setStatus(InvitationStatus.EXPIRED);
                invitationRepository.save(existing);
            } else {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: An invitation is already pending for this email."));
            }
        }
        
        Invitation invitation = new Invitation(team.getId(), userDetails.getId(), inviteRequest.getEmail());
        Optional<User> inviterUserOpt = userRepository.findById(Objects.requireNonNull(userDetails.getId()));
        if (inviterUserOpt.isPresent()) {
            User inviter = inviterUserOpt.get();
            invitation.setInviterFirstName(inviter.getFirstName());
            invitation.setInviterLastName(inviter.getLastName());
            invitation.setInviterEmail(inviter.getEmail());
        }
        Instant now = Instant.now();
        invitation.setLastSentAt(now);
        invitation.setExpiresAt(now.plusSeconds(invitationTtlHours * 3600L));
        invitationRepository.save(invitation);
        
        User userToInvite = userRepository.findByEmail(inviteRequest.getEmail()).orElse(null);
        String tokenLink = (userToInvite == null)
            ? ("http://localhost:5173/register?teamId=" + team.getId() + "&email=" + inviteRequest.getEmail())
            : ("http://localhost:5173/invitations/accept?token=" + invitation.getToken());
        try {
            boolean sent = emailService.sendInvitationEmail(inviteRequest.getEmail(), team.getName(), tokenLink);
            if (sent) {
                return ResponseEntity.ok(new MessageResponse("Invitation email sent!"));
            }
            return ResponseEntity.ok(new MessageResponse("Invitation generated (email sending disabled)."));
        } catch (Exception e) {
            invitationRepository.delete(invitation);
            return ResponseEntity.internalServerError().body(new MessageResponse("Error: Unable to send invitation email. Please try again later."));
        }
    }
    
    @GetMapping("/{id}/invitations")
    public ResponseEntity<?> getTeamInvitations(@PathVariable String id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(id));
        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        Optional<TeamMember> requesterMemberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), id);
        boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
        boolean isPMOorLeader = requesterMemberOpt.isPresent() && (requesterMemberOpt.get().getRoles().contains(ERole.ROLE_PMO) || requesterMemberOpt.get().getRoles().contains(ERole.ROLE_TEAM_LEADER));
        
        if (!isOwner && !isPMOorLeader) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You do not have permission to view invitations."));
        }
        
        List<Invitation> invitations = invitationRepository.findByTeamId(id);
        Instant now = Instant.now();
        boolean changed = false;
        for (Invitation inv : invitations) {
            if (inv.getStatus() == InvitationStatus.PENDING && inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(now)) {
                inv.setStatus(InvitationStatus.EXPIRED);
                changed = true;
            }
        }
        if (changed) {
            invitationRepository.saveAll(Objects.requireNonNull(invitations));
        }
        
        return ResponseEntity.ok(invitations);
    }
    
    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<?> acceptInvitation(@PathVariable String token) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Invitation> invitationOpt = invitationRepository.findByToken(Objects.requireNonNull(token));
        
        if (invitationOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invitation not found."));
        }
        
        Invitation invitation = invitationOpt.get();
        if (invitation.getStatus() == InvitationStatus.REVOKED) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: This invitation was revoked."));
        }
        if (invitation.getStatus() == InvitationStatus.EXPIRED) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: This invitation has expired."));
        }
        if (invitation.getStatus() != InvitationStatus.PENDING && invitation.getStatus() != InvitationStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invitation is not valid."));
        }
        
        if (invitation.getStatus() == InvitationStatus.PENDING && isExpired(invitation)) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: This invitation has expired."));
        }
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(invitation.getTeamId()));
        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        Optional<User> userOpt = userRepository.findById(Objects.requireNonNull(userDetails.getId()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }
        
        User user = userOpt.get();
        if (!user.getEmail().equalsIgnoreCase(invitation.getInviteeEmail())) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: This invitation is not for your email."));
        }
        
        Optional<TeamMember> existingMember = teamMemberRepository.findByUser_IdAndTeam_Id(user.getId(), teamOpt.get().getId());
        if (existingMember.isPresent()) {
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setAcceptedAt(Instant.now());
            invitation.setToken(UUID.randomUUID().toString());
            invitationRepository.save(invitation);
            return ResponseEntity.ok(new MessageResponse("You are already a member. Invitation marked as accepted."));
        }
        
        boolean memberCreated = false;
        try {
            TeamMember newMember = new TeamMember(user, teamOpt.get(), ERole.ROLE_TEAM_MEMBER);
            teamMemberRepository.save(newMember);
            memberCreated = true;
        } catch (DataIntegrityViolationException e) {
            Optional<TeamMember> memberNow = teamMemberRepository.findByUser_IdAndTeam_Id(user.getId(), teamOpt.get().getId());
            if (memberNow.isEmpty()) {
                throw e;
            }
        }
        
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitation.setToken(UUID.randomUUID().toString());
        invitationRepository.save(invitation);
        
        if (memberCreated) {
            return ResponseEntity.ok(new MessageResponse("Invitation accepted. You have been added to the team."));
        }
        return ResponseEntity.ok(new MessageResponse("You are already a member. Invitation marked as accepted."));
    }
    
    @PostMapping("/{id}/invitations/{invitationId}/resend")
    public ResponseEntity<?> resendInvitation(@PathVariable String id, @PathVariable String invitationId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(id));
        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        Optional<TeamMember> requesterMemberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), id);
        boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
        boolean isPMOorLeader = requesterMemberOpt.isPresent() && (requesterMemberOpt.get().getRoles().contains(ERole.ROLE_PMO) || requesterMemberOpt.get().getRoles().contains(ERole.ROLE_TEAM_LEADER));
        if (!isOwner && !isPMOorLeader) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You do not have permission to resend invitations."));
        }
        
        Optional<Invitation> invitationOpt = invitationRepository.findById(Objects.requireNonNull(invitationId));
        if (invitationOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invitation not found."));
        }
        
        Invitation invitation = invitationOpt.get();
        if (!id.equals(invitation.getTeamId())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invitation does not belong to this team."));
        }
        
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invitation already accepted."));
        }
        if (invitation.getStatus() == InvitationStatus.REVOKED) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invitation was revoked."));
        }
        
        String previousToken = invitation.getToken();
        Instant previousLastSentAt = invitation.getLastSentAt();
        Instant previousExpiresAt = invitation.getExpiresAt();
        InvitationStatus previousStatus = invitation.getStatus();
        
        Instant now = Instant.now();
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setLastSentAt(now);
        invitation.setExpiresAt(now.plusSeconds(invitationTtlHours * 3600L));
        invitation.setStatus(InvitationStatus.PENDING);
        invitationRepository.save(invitation);
        
        User userToInvite = userRepository.findByEmail(invitation.getInviteeEmail()).orElse(null);
        String tokenLink = (userToInvite == null)
            ? ("http://localhost:5173/register?teamId=" + id + "&email=" + invitation.getInviteeEmail())
            : ("http://localhost:5173/invitations/accept?token=" + invitation.getToken());
        
        try {
            boolean sent = emailService.sendInvitationEmail(invitation.getInviteeEmail(), teamOpt.get().getName(), tokenLink);
            if (sent) {
                return ResponseEntity.ok(new MessageResponse("Invitation email resent!"));
            }
            return ResponseEntity.ok(new MessageResponse("Invitation updated (email sending disabled)."));
        } catch (Exception e) {
            invitation.setToken(previousToken);
            invitation.setLastSentAt(previousLastSentAt);
            invitation.setExpiresAt(previousExpiresAt);
            invitation.setStatus(previousStatus);
            invitationRepository.save(invitation);
            return ResponseEntity.internalServerError().body(new MessageResponse("Error: Unable to resend invitation email. Please try again later."));
        }
    }
    
    @PostMapping("/{id}/invitations/{invitationId}/revoke")
    public ResponseEntity<?> revokeInvitation(@PathVariable String id, @PathVariable String invitationId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(id));
        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        Optional<TeamMember> requesterMemberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), id);
        boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
        boolean isPMOorLeader = requesterMemberOpt.isPresent() && (requesterMemberOpt.get().getRoles().contains(ERole.ROLE_PMO) || requesterMemberOpt.get().getRoles().contains(ERole.ROLE_TEAM_LEADER));
        if (!isOwner && !isPMOorLeader) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You do not have permission to revoke invitations."));
        }
        
        Optional<Invitation> invitationOpt = invitationRepository.findById(Objects.requireNonNull(invitationId));
        if (invitationOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invitation not found."));
        }
        
        Invitation invitation = invitationOpt.get();
        if (!id.equals(invitation.getTeamId())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invitation does not belong to this team."));
        }
        
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invitation already accepted."));
        }
        
        invitation.setStatus(InvitationStatus.REVOKED);
        invitation.setRevokedAt(Instant.now());
        invitation.setToken(UUID.randomUUID().toString());
        invitationRepository.save(invitation);
        
        return ResponseEntity.ok(new MessageResponse("Invitation revoked."));
    }
    
    private boolean isExpired(Invitation invitation) {
        return invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(Instant.now());
    }

    @PutMapping("/{id}/members/{userId}/roles")
    public ResponseEntity<?> updateMemberRoles(@PathVariable String id, @PathVariable String userId, @Valid @RequestBody UpdateUserRolesRequest rolesRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User requester = userRepository.findById(Objects.requireNonNull(userDetails.getId())).orElse(null);
        if (requester == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }
        if (isProfileMemberOnly(requester)) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: Team Member profiles cannot edit roles."));
        }
        Optional<Team> teamOpt = teamRepository.findById(Objects.requireNonNull(id));

        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Team not found."));
        }
        
        boolean isOwner = teamOpt.get().getOwner().getId().equals(userDetails.getId());
        
        Optional<TeamMember> requesterMemberOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userDetails.getId(), id);
        boolean isPMO = requesterMemberOpt.isPresent() && requesterMemberOpt.get().getRoles().contains(ERole.ROLE_PMO);

        if (!isPMO && !isOwner) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: You do not have permission to update roles."));
        }

        Optional<User> memberUserOpt = userRepository.findById(Objects.requireNonNull(userId));
        if (memberUserOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found."));
        }
        
        Optional<TeamMember> memberShipOpt = teamMemberRepository.findByUser_IdAndTeam_Id(userId, id);
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

    private boolean isProfileMemberOnly(User user) {
        Set<ERole> profileRoles = user.getRoles();
        boolean hasManagementProfile = profileRoles.contains(ERole.ROLE_PMO) || profileRoles.contains(ERole.ROLE_TEAM_LEADER);
        return profileRoles.contains(ERole.ROLE_TEAM_MEMBER) && !hasManagementProfile;
    }
}
