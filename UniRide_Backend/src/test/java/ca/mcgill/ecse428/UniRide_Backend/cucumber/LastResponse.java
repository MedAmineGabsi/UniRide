package ca.mcgill.ecse428.UniRide_Backend.cucumber;

import org.springframework.mock.web.MockHttpServletResponse;

import io.cucumber.spring.ScenarioScope;

/**
 * The last HTTP response of the current scenario, shared by every step definition class.
 * The common {@code a "..." message is issued} step reads it, whichever feature sent the request.
 */
@ScenarioScope
public class LastResponse {

    private MockHttpServletResponse response;

    public MockHttpServletResponse get() {
        if (response == null) {
            throw new IllegalStateException("No request has been sent in this scenario");
        }
        return response;
    }

    public void set(MockHttpServletResponse response) {
        this.response = response;
    }
}
