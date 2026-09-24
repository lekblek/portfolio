package com.scalke.portfolio.backend.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({ContainersConfiguration.class, FixedClockConfiguration.class})
public abstract class AbstractIntegrationTest {
}
