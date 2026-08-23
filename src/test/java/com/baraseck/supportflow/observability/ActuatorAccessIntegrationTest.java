package com.baraseck.supportflow.observability;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,info,prometheus",
        "supportflow.observability.prometheus-public=true"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureObservability
class ActuatorAccessIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private SupportFlowMetrics metrics;

    @Test void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test void prometheusIsPublicInDevConfiguration() throws Exception {
        metrics.ticketCreated();
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("supportflow_ticket_creations_total")));
    }
}
