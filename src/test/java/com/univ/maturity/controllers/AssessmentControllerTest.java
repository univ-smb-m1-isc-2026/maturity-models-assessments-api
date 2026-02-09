package com.univ.maturity.controllers;

import java.util.Collections;
import java.util.List;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.maturity.Answer;
import com.univ.maturity.Assessment;
import com.univ.maturity.AssessmentRepository;
import com.univ.maturity.ERole;
import com.univ.maturity.MaturityModel;
import com.univ.maturity.MaturityModelRepository;
import com.univ.maturity.Team;
import com.univ.maturity.TeamMember;
import com.univ.maturity.TeamMemberRepository;
import com.univ.maturity.TeamRepository;
import com.univ.maturity.User;
import com.univ.maturity.payload.request.StartAssessmentRequest;
import com.univ.maturity.security.WebSecurityConfig;
import com.univ.maturity.security.jwt.AuthEntryPointJwt;
import com.univ.maturity.security.jwt.JwtUtils;
import com.univ.maturity.security.services.UserDetailsImpl;
import com.univ.maturity.security.services.UserDetailsServiceImpl;

@WebMvcTest(AssessmentController.class)
@Import(WebSecurityConfig.class)
@SuppressWarnings("null")
public class AssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssessmentRepository assessmentRepository;

    @MockBean
    private TeamRepository teamRepository;

    @MockBean
    private MaturityModelRepository maturityModelRepository;

    @MockBean
    private TeamMemberRepository teamMemberRepository;

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
    public void startAssessment_ShouldReturnOk_WhenUserIsLeader() throws Exception {
        String teamId = "team1";
        String modelId = "model1";
        StartAssessmentRequest request = new StartAssessmentRequest();
        request.setTeamId(teamId);
        request.setMaturityModelId(modelId);

        Team team = new Team();
        team.setId(teamId);
        User owner = new User();
        owner.setId("ownerId");
        team.setOwner(owner);

        MaturityModel model = new MaturityModel();
        model.setId(modelId);

        TeamMember member = new TeamMember("userId", teamId, ERole.ROLE_TEAM_LEADER);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(maturityModelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(teamMemberRepository.findByUserIdAndTeamId("userId", teamId)).thenReturn(Optional.of(member));
        when(assessmentRepository.save(any(Assessment.class))).thenReturn(new Assessment(team, model));

        mockMvc.perform(post("/api/assessments/start")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void startAssessment_ShouldReturnForbidden_WhenUserIsMember() throws Exception {
        String teamId = "team1";
        String modelId = "model1";
        StartAssessmentRequest request = new StartAssessmentRequest();
        request.setTeamId(teamId);
        request.setMaturityModelId(modelId);

        Team team = new Team();
        team.setId(teamId);
        User owner = new User();
        owner.setId("ownerId");
        team.setOwner(owner);

        TeamMember member = new TeamMember("userId", teamId, ERole.ROLE_TEAM_MEMBER);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByUserIdAndTeamId("userId", teamId)).thenReturn(Optional.of(member));

        mockMvc.perform(post("/api/assessments/start")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void submitAssessment_ShouldReturnOk_WhenUserIsMember() throws Exception {
        String assessmentId = "assessment1";
        String teamId = "team1";
        
        Team team = new Team();
        team.setId(teamId);
        User owner = new User();
        owner.setId("ownerId");
        team.setOwner(owner);
        
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setTeam(team);
        
        TeamMember member = new TeamMember("userId", teamId, ERole.ROLE_TEAM_MEMBER);
        
        List<Answer> answers = Collections.emptyList();

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByUserIdAndTeamId("userId", teamId)).thenReturn(Optional.of(member));
        when(assessmentRepository.save(any(Assessment.class))).thenReturn(assessment);

        mockMvc.perform(put("/api/assessments/" + assessmentId + "/submit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(answers)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void getAssessment_ShouldReturnOk_WhenUserIsMember() throws Exception {
        String assessmentId = "assessment1";
        String teamId = "team1";
        
        Team team = new Team();
        team.setId(teamId);
        User owner = new User();
        owner.setId("ownerId");
        team.setOwner(owner);
        
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setTeam(team);
        
        TeamMember member = new TeamMember("userId", teamId, ERole.ROLE_TEAM_MEMBER);
        
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByUserIdAndTeamId("userId", teamId)).thenReturn(Optional.of(member));

        mockMvc.perform(get("/api/assessments/" + assessmentId))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "user", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void getTeamAssessments_ShouldReturnOk_WhenUserIsMember() throws Exception {
        String teamId = "team1";
        
        Team team = new Team();
        team.setId(teamId);
        User owner = new User();
        owner.setId("ownerId");
        team.setOwner(owner);
        
        Assessment assessment = new Assessment();
        assessment.setId("assessment1");
        assessment.setTeam(team);
        
        TeamMember member = new TeamMember("userId", teamId, ERole.ROLE_TEAM_MEMBER);
        
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByUserIdAndTeamId("userId", teamId)).thenReturn(Optional.of(member));
        when(assessmentRepository.findByTeam(team)).thenReturn(Collections.singletonList(assessment));

        mockMvc.perform(get("/api/assessments/team/" + teamId))
                .andExpect(status().isOk());
    }
}
