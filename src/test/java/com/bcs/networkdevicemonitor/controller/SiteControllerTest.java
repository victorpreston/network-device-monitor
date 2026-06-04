package com.bcs.networkdevicemonitor.controller;

import com.bcs.networkdevicemonitor.dto.request.RegisterSiteRequest;
import com.bcs.networkdevicemonitor.dto.response.SiteResponse;
import com.bcs.networkdevicemonitor.service.SiteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SiteController.class)
class SiteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SiteService siteService;

    @Nested
    class RegisterSite {

        @Test
        void returns201_whenRequestIsValid() throws Exception {
            SiteResponse response = new SiteResponse(UUID.randomUUID(), "London-01", "1 Tech Street", null, null, OffsetDateTime.now());
            when(siteService.register(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/sites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterSiteRequest("London-01", "1 Tech Street", null, null))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("London-01"));
        }

        @Test
        void returns400_whenNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/v1/sites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterSiteRequest("", null, null, null))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ListSites {

        @Test
        void returns200_withSiteList() throws Exception {
            when(siteService.listAll()).thenReturn(List.of(
                    new SiteResponse(UUID.randomUUID(), "London-01", null, null, null, OffsetDateTime.now()),
                    new SiteResponse(UUID.randomUUID(), "Manchester-01", null, null, null, OffsetDateTime.now())
            ));

            mockMvc.perform(get("/api/v1/sites"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }
    }
}
