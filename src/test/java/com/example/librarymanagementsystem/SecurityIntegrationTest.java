package com.example.librarymanagementsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises the real Spring Security filter chain with the isolated test profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedBookApiRejectsAnonymousRequestsWithUnauthorizedStatus() throws Exception {
        mockMvc.perform(get("/api/books"))
                // API requests receive an HTTP Basic challenge rather than a browser form redirect.
                .andExpect(status().isUnauthorized());
    }

    @Test
    void configuredAdministratorCanAccessProtectedBookApi() throws Exception {
        mockMvc.perform(get("/api/books")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("test-admin", "test-password"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void auditHistoryIsRestrictedToAdministrators() throws Exception {
        mockMvc.perform(get("/audit"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/audit")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("test-admin", "test-password")))
                .andExpect(status().isOk())
                .andExpect(view().name("audit"));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
