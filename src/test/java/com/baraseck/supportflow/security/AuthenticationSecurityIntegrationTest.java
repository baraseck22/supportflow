package com.baraseck.supportflow.security;

import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.entity.User;
import com.baraseck.supportflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = "supportflow.cors.allowed-origins=http://localhost:4200")
@AutoConfigureMockMvc
class AuthenticationSecurityIntegrationTest {
    private static final String SECRET = "test-only-jwt-secret-that-is-at-least-32-bytes-long";
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwtService;
    private User active;

    @BeforeEach void setUp() {
        active = save("security.active@supportflow.local", Role.SUPPORT_N1, true);
        save("security.inactive@supportflow.local", Role.USER, false);
    }

    @Test void validLoginReturnsTokenAndUser() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("security.active@supportflow.local", "SupportFlow2026!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer")).andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.role").value("SUPPORT_N1"));
    }

    @Test void wrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("security.active@supportflow.local", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test void inactiveUserIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("security.inactive@supportflow.local", "SupportFlow2026!"))).andExpect(status().isUnauthorized());
    }

    @Test void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test void developmentAngularOriginIsAllowedByCors() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.containsString("POST")));
    }

    @Test void invalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tickets").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test void validTokenAllowsProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/tickets").header("Authorization", bearer(active, jwtService)))
                .andExpect(status().isOk());
    }

    @Test void expiredTokenIsUnauthorized() throws Exception {
        JwtService expired = new JwtService(SECRET, -1, Clock.systemUTC());
        mockMvc.perform(get("/api/tickets").header("Authorization", bearer(active, expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test void userCannotAssignOrEscalate() throws Exception {
        User regular = save("security.user@supportflow.local", Role.USER, true);
        String bearer = bearer(regular, jwtService);
        mockMvc.perform(put("/api/tickets/" + UUID.randomUUID() + "/assign").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"assignedToUserId\":\"" + active.getId() + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/tickets/" + UUID.randomUUID() + "/escalate").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetUserId\":\"" + active.getId() + "\",\"reason\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void supportN1CanReachAssignAndEscalateOperations() throws Exception {
        String bearer = bearer(active, jwtService);
        mockMvc.perform(put("/api/tickets/" + UUID.randomUUID() + "/assign").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assignedToUserId\":\"" + active.getId() + "\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/tickets/" + UUID.randomUUID() + "/escalate").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetUserId\":\"" + active.getId() + "\",\"reason\":\"test\"}"))
                .andExpect(status().isNotFound());
    }

    @Test void supportN2CanReachStatusOperation() throws Exception {
        User n2 = save("security.n2@supportflow.local", Role.SUPPORT_N2, true);
        mockMvc.perform(put("/api/tickets/" + UUID.randomUUID() + "/status")
                        .header("Authorization", bearer(n2, jwtService)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test void onlyAdminCanReadDevelopmentReferenceEndpoints() throws Exception {
        User regular = save("security.reference.user@supportflow.local", Role.USER, true);
        mockMvc.perform(get("/api/dev/categories").header("Authorization", bearer(regular, jwtService)))
                .andExpect(status().isForbidden());
        User admin = save("security.admin@supportflow.local", Role.ADMIN, true);
        mockMvc.perform(get("/api/dev/categories").header("Authorization", bearer(admin, jwtService)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    private User save(String email, Role role, boolean activeValue) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User(); user.setFirstName("Security"); user.setLastName("Test"); user.setEmail(email);
            user.setPasswordHash(encoder.encode("SupportFlow2026!")); user.setRole(role); user.setActive(activeValue);
            return users.save(user);
        });
    }
    private String login(String email, String password) { return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"; }
    private String bearer(User user, JwtService service) {
        var principal = new SupportFlowPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole(), user.isActive());
        return "Bearer " + service.generate(principal);
    }
}
