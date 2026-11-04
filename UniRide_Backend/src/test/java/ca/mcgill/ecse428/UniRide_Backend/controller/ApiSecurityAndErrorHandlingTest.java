package ca.mcgill.ecse428.UniRide_Backend.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ca.mcgill.ecse428.UniRide_Backend.TestcontainersConfiguration;

/**
 * HTTP-level checks: which endpoints need a sign-in (SecurityConfig), and that errors come back
 * as JSON {@code {"message": ...}} (GlobalExceptionHandler). No data is written.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApiSecurityAndErrorHandlingTest {

    private static final String UNKNOWN_EMAIL = "nobody.registered@mail.mcgill.ca";

    private static final String VALID_RIDE = """
            {"origin": "252 Laurier Ave E, Ottawa ON, K1P 5G4",
             "destination": "220 Yonge Street, Toronto ON, M5T 1E3",
             "departureDateTime": "2030-01-10 09:00", "durationMinutes": 300,
             "totalSeats": 3, "pricePerSeat": 15}
            """;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ------------------------------------------------------------------ sign-in

    @Test
    void creatingARideRequiresSignIn() throws Exception {
        mockMvc.perform(post("/api/rides").contentType(MediaType.APPLICATION_JSON).content(VALID_RIDE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestingASeatRequiresSignIn() throws Exception {
        mockMvc.perform(post("/seat-requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rideId\": 1, \"seatsRequested\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    /** A malformed body gets past security and is rejected by the app, so no sign-in was needed. */
    @Test
    void registrationIsPublic() throws Exception {
        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void tokenWithoutAnEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/rides").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_RIDE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token does not contain an email"));
    }

    // ------------------------------------------------------------------ error responses

    @Test
    void unknownUserCannotCreateARide() throws Exception {
        mockMvc.perform(post("/api/rides").with(signedInAs(UNKNOWN_EMAIL))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_RIDE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void malformedRideBodyIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/rides").with(signedInAs(UNKNOWN_EMAIL))
                .contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void seatRequestWithoutARideIdIsABadRequest() throws Exception {
        mockMvc.perform(post("/seat-requests").with(signedInAs(UNKNOWN_EMAIL))
                .contentType(MediaType.APPLICATION_JSON).content("{\"seatsRequested\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ride id is required"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor signedInAs(String email) {
        return jwt().jwt(token -> token.claim("email", email));
    }
}
