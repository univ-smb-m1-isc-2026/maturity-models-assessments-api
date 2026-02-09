package com.univ.maturity.controllers;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.maturity.ERole;
import com.univ.maturity.Team;
import com.univ.maturity.TeamMember;
import com.univ.maturity.TeamMemberRepository;
import com.univ.maturity.TeamRepository;
import com.univ.maturity.User;
import com.univ.maturity.UserRepository;
import com.univ.maturity.payload.request.InviteMemberRequest;
import com.univ.maturity.payload.request.TeamRequest;
import com.univ.maturity.security.WebSecurityConfig;
import com.univ.maturity.security.jwt.AuthEntryPointJwt;
import com.univ.maturity.security.jwt.JwtUtils;
import com.univ.maturity.security.services.UserDetailsImpl;
import com.univ.maturity.security.services.UserDetailsServiceImpl;
import com.univ.maturity.services.EmailService;

@WebMvcTest(TeamController.class)
@Import(WebSecurityConfig.class)
@SuppressWarnings("null")
public class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamRepository teamRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TeamMemberRepository teamMemberRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    @SuppressWarnings("unused")
    private AuthEntryPointJwt authEntryPointJwt;

    @MockBean
    @SuppressWarnings("unused")
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        UserDetailsImpl userDetails = new UserDetailsImpl("userId", "First", "Last", "user", "password", true, Collections.emptyList());
        when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails);
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void createTeam_ShouldReturnOk_WhenAuthenticated() throws Exception {
        TeamRequest request = new TeamRequest();
        request.setName("New Team");

        User user = new User();
        user.setId("userId");

        when(userRepository.findById("userId")).thenReturn(Optional.of(user));
        when(teamRepository.existsByName("New Team")).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenReturn(new Team("New Team", user));
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(new TeamMember());

        mockMvc.perform(post("/api/teams")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void inviteMember_ShouldReturnOk_WhenUserIsLeader() throws Exception {
        String teamId = "team1";
        InviteMemberRequest request = new InviteMemberRequest();
        request.setEmail("invitee@test.com");

        Team team = new Team();
        team.setId(teamId);
        team.setName("Test Team");
        
        TeamMember member = new TeamMember("userId", teamId, ERole.ROLE_TEAM_LEADER);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByUserIdAndTeamId("userId", teamId)).thenReturn(Optional.of(member));
        when(userRepository.findByEmail("invitee@test.com")).thenReturn(Optional.empty());
        doNothing().when(emailService).sendInvitationEmail(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/teams/" + teamId + "/invite")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        verify(emailService).sendInvitationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void inviteMember_ShouldReturnForbidden_WhenUserIsMember() throws Exception {
        String teamId = "team1";
        InviteMemberRequest request = new InviteMemberRequest();
        request.setEmail("invitee@test.com");

        Team team = new Team();
        team.setId(teamId);
        
        TeamMember member = new TeamMember("userId", teamId, ERole.ROLE_TEAM_MEMBER);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByUserIdAndTeamId("userId", teamId)).thenReturn(Optional.of(member));

        mockMvc.perform(post("/api/teams/" + teamId + "/invite")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
