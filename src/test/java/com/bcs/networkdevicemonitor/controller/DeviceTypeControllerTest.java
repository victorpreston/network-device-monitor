package com.bcs.networkdevicemonitor.controller;

import com.bcs.networkdevicemonitor.dto.response.DeviceTypeResponse;
import com.bcs.networkdevicemonitor.service.DeviceTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceTypeController.class)
class DeviceTypeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean DeviceTypeService deviceTypeService;

    @Test
    void returns200_withAllDeviceTypes() throws Exception {
        when(deviceTypeService.listAll()).thenReturn(List.of(
                new DeviceTypeResponse(UUID.randomUUID(), "Router"),
                new DeviceTypeResponse(UUID.randomUUID(), "Switch"),
                new DeviceTypeResponse(UUID.randomUUID(), "Firewall")
        ));

        mockMvc.perform(get("/api/v1/device-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("Router"));
    }
}
