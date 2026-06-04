package com.bcs.networkdevicemonitor;

import com.bcs.networkdevicemonitor.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class NetworkDeviceMonitorApplicationTests {

    @Test
    void contextLoads() {
    }
}
