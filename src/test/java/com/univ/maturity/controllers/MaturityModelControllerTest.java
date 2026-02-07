package com.univ.maturity.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.maturity.MaturityModel;
import com.univ.maturity.MaturityModelRepository;
import com.univ.maturity.security.jwt.AuthEntryPointJwt;
import com.univ.maturity.security.services.UserDetailsServiceImpl;
import com.univ.maturity.security.jwt.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(MaturityModelController.class)
@SuppressWarnings("null")
public class MaturityModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaturityModelRepository maturityModelRepository;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @MockBean
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "PMO")
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
    @WithMockUser(roles = "PMO")
    public void createModel_ShouldReturnOk_WhenUserIsPMO() throws Exception {
        MaturityModel model = new MaturityModel();
        model.setName("New Model");
        model.setQuestions(Collections.emptyList());

        when(maturityModelRepository.existsByName("New Model")).thenReturn(false);
        when(maturityModelRepository.save(any(MaturityModel.class))).thenReturn(model);

        mockMvc.perform(post("/api/models")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(model)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    public void createModel_ShouldReturnForbidden_WhenUserIsNotPMO() throws Exception {
        MaturityModel model = new MaturityModel();
        model.setName("New Model");

        mockMvc.perform(post("/api/models")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(model)))
                .andExpect(status().isForbidden());
    }
}
