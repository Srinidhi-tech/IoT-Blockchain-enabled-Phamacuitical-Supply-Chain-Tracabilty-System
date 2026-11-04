package com.pharmatrace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PharmaTraceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Context loads and dashboard home view is reachable")
    void contextLoadsAndIndexRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("batch"))
                .andExpect(model().attributeExists("hubs"))
                .andExpect(content().string(containsString("PharmaTrace")));
    }

    @Test
    @DisplayName("REST API /api/telemetry processes Tag 1 (2AF52B07) scan and returns green buzzer-off response")
    void testIngestTelemetryTag1() throws Exception {
        String payload = """
                {
                    "rfidUid": "2AF52B07",
                    "batchId": "VAX-B4920-COVID19",
                    "temperature": 4.5,
                    "humidity": 45.0,
                    "locationName": "Distributor Hub (College)"
                }
                """;

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.buzzer", is(false)))
                .andExpect(jsonPath("$.led", is("GREEN")))
                .andExpect(jsonPath("$.verificationStatus", is("AUTHENTIC_VERIFIED")))
                .andExpect(jsonPath("$.txHash", startsWith("0x")));
    }

    @Test
    @DisplayName("REST API /api/telemetry processes Tag 2 (659A1707 / Custodian) scan and returns green buzzer-off response")
    void testIngestTelemetryTag2() throws Exception {
        String payload = """
                {
                    "rfidUid": "659A1707",
                    "batchId": "VAX-B4920-COVID19",
                    "temperature": 5.2,
                    "humidity": 46.5,
                    "locationName": "Distributor Hub (College)"
                }
                """;

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.buzzer", is(false)))
                .andExpect(jsonPath("$.led", is("GREEN")))
                .andExpect(jsonPath("$.verificationStatus", is("AUTHENTIC_VERIFIED")))
                .andExpect(jsonPath("$.txHash", startsWith("0x")));
    }

    @Test
    @DisplayName("REST API /api/telemetry processes authentic tag scan and returns green buzzer-off response")
    void testIngestTelemetryAuthentic() throws Exception {
        String payload = """
                {
                    "rfidUid": "2AF52B07",
                    "batchId": "VAX-B4920-COVID19",
                    "temperature": 4.5,
                    "humidity": 45.0,
                    "locationName": "Manufacturer Hub"
                }
                """;

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.buzzer", is(false)))
                .andExpect(jsonPath("$.led", is("GREEN")))
                .andExpect(jsonPath("$.verificationStatus", is("AUTHENTIC_VERIFIED")))
                .andExpect(jsonPath("$.txHash", startsWith("0x")));
    }

    @Test
    @DisplayName("REST API /api/telemetry triggers active buzzer on unauthorized RFID tag")
    void testIngestTelemetryUnauthorized() throws Exception {
        String payload = """
                {
                    "rfidUid": "UNKNOWN-CARD-B-99",
                    "batchId": "VAX-B4920-COVID19",
                    "temperature": 4.5,
                    "humidity": 45.0,
                    "locationName": "Distributor Hub (College)"
                }
                """;

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.buzzer", is(true)))
                .andExpect(jsonPath("$.led", is("RED")))
                .andExpect(jsonPath("$.verificationStatus", is("UNAUTHORIZED_TAG_BREACH")))
                .andExpect(jsonPath("$.txHash", startsWith("0x")));
    }

    @Test
    @DisplayName("REST API /api/batch/active returns current batch custody data")
    void testGetActiveBatch() throws Exception {
        mockMvc.perform(get("/api/batch/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId", is("VAX-B4920-COVID19")))
                .andExpect(jsonPath("$.currentHub", notNullValue()));
    }
}
