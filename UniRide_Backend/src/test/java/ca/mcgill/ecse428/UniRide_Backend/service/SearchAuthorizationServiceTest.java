
package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;

class SearchAuthorizationServiceTest {

    private PersonRepository personRepository;
    private SearchAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        personRepository = mock(PersonRepository.class);
        authorizationService =
                new SearchAuthorizationService(personRepository);
    }

    @Test
    void registeredUserCanSearch() {
        when(personRepository.existsByEmailIgnoreCase(
                "student@mcgill.ca")).thenReturn(true);

        assertDoesNotThrow(() ->
                authorizationService.authorizeSearch(
                        "student@mcgill.ca"));

        verify(personRepository).existsByEmailIgnoreCase(
                "student@mcgill.ca");
    }

    @Test
    void unregisteredUserCannotSearch() {
        when(personRepository.existsByEmailIgnoreCase(
                "unknown@mcgill.ca")).thenReturn(false);

        UniRideException exception = assertThrows(
                UniRideException.class,
                () -> authorizationService.authorizeSearch(
                        "unknown@mcgill.ca"));

        assertEquals(HttpStatus.UNAUTHORIZED,
                exception.getStatus());

        assertEquals("Unauthorized request",
                exception.getMessage());
    }

    @Test
    void nullEmailIsRejected() {
        UniRideException exception = assertThrows(
                UniRideException.class,
                () -> authorizationService.authorizeSearch(null));

        assertEquals(HttpStatus.UNAUTHORIZED,
                exception.getStatus());

        verifyNoInteractions(personRepository);
    }

    @Test
    void blankEmailIsRejected() {
        UniRideException exception = assertThrows(
                UniRideException.class,
                () -> authorizationService.authorizeSearch("   "));

        assertEquals(HttpStatus.UNAUTHORIZED,
                exception.getStatus());

        verifyNoInteractions(personRepository);
    }

    @Test
    void emailIsTrimmedBeforeLookup() {
        when(personRepository.existsByEmailIgnoreCase(
                "student@mcgill.ca")).thenReturn(true);

        assertDoesNotThrow(() ->
                authorizationService.authorizeSearch(
                        " student@mcgill.ca "));

        verify(personRepository).existsByEmailIgnoreCase(
                "student@mcgill.ca");
    }
}
