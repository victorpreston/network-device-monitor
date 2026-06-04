package com.bcs.networkdevicemonitor.controller;

import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import com.bcs.networkdevicemonitor.dto.request.RegisterDeviceRequest;
import com.bcs.networkdevicemonitor.dto.request.SubmitReportRequest;
import com.bcs.networkdevicemonitor.dto.response.DeviceDetailResponse;
import com.bcs.networkdevicemonitor.dto.response.DeviceListResponse;
import com.bcs.networkdevicemonitor.exception.ResourceNotFoundException;
import com.bcs.networkdevicemonitor.service.DeviceService;
import com.bcs.networkdevicemonitor.service.ReportService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean DeviceService deviceService;
    @MockBean ReportService reportService;

    @Nested
    class RegisterDevice {

        @Test
        void returns201_whenRequestIsValid() throws Exception {
            RegisterDeviceRequest request = new RegisterDeviceRequest("Router-01", UUID.randomUUID(), "192.168.1.1", UUID.randomUUID());
            DeviceListResponse response = listResponse("Router-01");

            when(deviceService.register(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Router-01"));
        }

        @Test
        void returns400_whenNameIsBlank() throws Exception {
            RegisterDeviceRequest request = new RegisterDeviceRequest("", UUID.randomUUID(), "192.168.1.1", UUID.randomUUID());

            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void returns400_whenHostnameIsBlank() throws Exception {
            RegisterDeviceRequest request = new RegisterDeviceRequest("Router-01", UUID.randomUUID(), "", UUID.randomUUID());

            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ListDevices {

        @Test
        void returns200_withDeviceList() throws Exception {
            when(deviceService.listAll()).thenReturn(List.of(listResponse("Router-01"), listResponse("Switch-01")));

            mockMvc.perform(get("/api/v1/devices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        void returns200_withEmptyList() throws Exception {
            when(deviceService.listAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/devices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    class GetDevice {

        @Test
        void returns200_whenDeviceExists() throws Exception {
            UUID id = UUID.randomUUID();
            DeviceDetailResponse response = new DeviceDetailResponse(id, "Router-01", "Router", "192.168.1.1", "London-01", OffsetDateTime.now(), DeviceStatus.ONLINE, OffsetDateTime.now(), false, List.of());

            when(deviceService.getById(id)).thenReturn(response);

            mockMvc.perform(get("/api/v1/devices/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id.toString()));
        }

        @Test
        void returns404_whenDeviceNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(deviceService.getById(id)).thenThrow(new ResourceNotFoundException("Device not found"));

            mockMvc.perform(get("/api/v1/devices/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Device not found"));
        }
    }

    @Nested
    class SubmitReport {

        @Test
        void returns201_whenReportIsValid() throws Exception {
            UUID id = UUID.randomUUID();
            SubmitReportRequest request = new SubmitReportRequest(DeviceStatus.ONLINE, "All good");

            doNothing().when(reportService).submit(eq(id), any());

            mockMvc.perform(post("/api/v1/devices/{id}/reports", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void returns400_whenStatusIsMissing() throws Exception {
            mockMvc.perform(post("/api/v1/devices/{id}/reports", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"test\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    private DeviceListResponse listResponse(String name) {
        return new DeviceListResponse(UUID.randomUUID(), name, "Router", "192.168.1.1", "London-01", OffsetDateTime.now(), DeviceStatus.ONLINE, OffsetDateTime.now(), false);
    }
}
