package com.bcs.networkdevicemonitor.service.impl;

import com.bcs.networkdevicemonitor.dto.response.DeviceTypeResponse;
import com.bcs.networkdevicemonitor.repository.DeviceTypeRepository;
import com.bcs.networkdevicemonitor.service.DeviceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTypeServiceImpl implements DeviceTypeService {

    private final DeviceTypeRepository deviceTypeRepository;

    @Override
    public List<DeviceTypeResponse> listAll() {
        return deviceTypeRepository.findAll()
                .stream()
                .map(t -> new DeviceTypeResponse(t.getId(), t.getName()))
                .toList();
    }
}
