package com.univ.maturity.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.maturity.ERole;
import com.univ.maturity.Level;
import com.univ.maturity.MaturityModel;
import com.univ.maturity.MaturityModelRepository;
import com.univ.maturity.Question;
import com.univ.maturity.Team;
import com.univ.maturity.TeamMember;
import com.univ.maturity.TeamMemberRepository;
import com.univ.maturity.TeamRepository;
import com.univ.maturity.User;
import com.univ.maturity.security.WebSecurityConfig;
import com.univ.maturity.security.jwt.AuthEntryPointJwt;
import com.univ.maturity.security.jwt.JwtUtils;
import com.univ.maturity.security.services.UserDetailsImpl;
import com.univ.maturity.security.services.UserDetailsServiceImpl;

@WebMvcTest(MaturityModelController.class)
@Import(WebSecurityConfig.class)
@SuppressWarnings("null")
public class MaturityModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaturityModelRepository maturityModelRepository;

    @MockBean
    private TeamRepository teamRepository;

    @MockBean
    private TeamMemberRepository teamMemberRepository;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @MockBean
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        UserDetailsImpl userDetails = new UserDetailsImpl("userId", "First", "Last", "user", "password", true, Collections.emptyList());
        when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails);
        Objects.requireNonNull(authEntryPointJwt);
        Objects.requireNonNull(jwtUtils);
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void getAllModels_ShouldReturnList_WhenAuthorized() throws Exception {
        MaturityModel model1 = new MaturityModel();
        model1.setId("1");
        model1.setName("DevOps");
        
        when(maturityModelRepository.findAll()).thenReturn(Arrays.asList(model1));

        mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("DevOps"));
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void createModel_ShouldReturnOk_WhenUserIsPMO() throws Exception {
        String teamId = "team1";
        MaturityModel model = new MaturityModel();
        model.setName("New Model");
        model.setTeamId(teamId);
        
        List<Level> levels = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            levels.add(new Level(i, "Level " + i));
        }
        Question question = new Question("Question 1", levels);
        model.setQuestions(Collections.singletonList(question));

        Team team = new Team();
        team.setId(teamId);
        User owner = new User();
        owner.setId("ownerId");
        team.setOwner(owner);

        User user = new User();
        user.setId("userId");
        TeamMember member = new TeamMember(user, team, ERole.ROLE_PMO);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByUser_IdAndTeam_Id("userId", teamId)).thenReturn(Optional.of(member));
        when(maturityModelRepository.existsByName("New Model")).thenReturn(false);
        when(maturityModelRepository.save(any(MaturityModel.class))).thenReturn(model);

        mockMvc.perform(post("/api/models")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(model)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void createModel_ShouldReturnForbidden_WhenUserIsNotPMO() throws Exception {
        String teamId = "team1";
        MaturityModel model = new MaturityModel();
        model.setName("New Model");
        model.setTeamId(teamId);

        List<Level> levels = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            levels.add(new Level(i, "Level " + i));
        }
        Question question = new Question("Question 1", levels);
        model.setQuestions(Collections.singletonList(question));

        Team team = new Team();
        team.setId(teamId);
        User owner = new User();
        owner.setId("ownerId");
        team.setOwner(owner);

        User user = new User();
        user.setId("userId");
        TeamMember member = new TeamMember(user, team, ERole.ROLE_TEAM_MEMBER);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByUser_IdAndTeam_Id("userId", teamId)).thenReturn(Optional.of(member));

        mockMvc.perform(post("/api/models")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(model)))
                .andExpect(status().isForbidden());
    }
}
