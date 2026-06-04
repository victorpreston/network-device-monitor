package com.bcs.networkdevicemonitor.controller;

import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import com.bcs.networkdevicemonitor.dto.request.RegisterSiteRequest;
import com.bcs.networkdevicemonitor.dto.response.DeviceListResponse;
import com.bcs.networkdevicemonitor.dto.response.SiteResponse;
import com.bcs.networkdevicemonitor.exception.ResourceNotFoundException;
import com.bcs.networkdevicemonitor.service.DeviceService;
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
    @MockBean DeviceService deviceService;

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
                    .andExpect(jsonPath("$.data.name").value("London-01"))
                    .andExpect(jsonPath("$.errors").doesNotExist())
                    .andExpect(jsonPath("$.meta.version").value("v1"));
        }

        @Test
        void returns400_whenNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/v1/sites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterSiteRequest("", null, null, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors[0].field").value("name"))
                    .andExpect(jsonPath("$.data").doesNotExist());
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
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.meta.count").value(2))
                    .andExpect(jsonPath("$.errors").doesNotExist());
        }
    }

    @Nested
    class GetSite {

        @Test
        void returns200_whenSiteExists() throws Exception {
            UUID id = UUID.randomUUID();
            SiteResponse response = new SiteResponse(id, "London-01", "1 Tech St", null, null, OffsetDateTime.now());
            when(siteService.getById(id)).thenReturn(response);

            mockMvc.perform(get("/api/v1/sites/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id.toString()))
                    .andExpect(jsonPath("$.data.name").value("London-01"))
                    .andExpect(jsonPath("$.errors").doesNotExist());
        }

        @Test
        void returns404_whenSiteNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(siteService.getById(id)).thenThrow(new ResourceNotFoundException("Site not found"));

            mockMvc.perform(get("/api/v1/sites/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].message").value("Site not found"))
                    .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    class GetSiteDevices {

        @Test
        void returns200_withDevicesForSite() throws Exception {
            UUID siteId = UUID.randomUUID();
            DeviceListResponse device = new DeviceListResponse(UUID.randomUUID(), "Router-01", "Router", "192.168.1.1", "London-01", OffsetDateTime.now(), DeviceStatus.ONLINE, OffsetDateTime.now(), false);
            when(deviceService.listBySite(siteId)).thenReturn(List.of(device));

            mockMvc.perform(get("/api/v1/sites/{id}/devices", siteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.meta.count").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Router-01"))
                    .andExpect(jsonPath("$.errors").doesNotExist());
        }

        @Test
        void returns404_whenSiteNotFound() throws Exception {
            UUID siteId = UUID.randomUUID();
            when(deviceService.listBySite(siteId)).thenThrow(new ResourceNotFoundException("Site not found"));

            mockMvc.perform(get("/api/v1/sites/{id}/devices", siteId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
}
