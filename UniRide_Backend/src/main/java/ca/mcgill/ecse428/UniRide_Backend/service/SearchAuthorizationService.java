
package ca.mcgill.ecse428.UniRide_Backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;

@Service
public class SearchAuthorizationService {

    private final PersonRepository personRepository;

    public SearchAuthorizationService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    /**
     * TID049: Only registered UniRide users can search for rides.
     *
     * @param authenticatedEmail email obtained from the authenticated token
     */
    public void authorizeSearch(String authenticatedEmail) {

        if (authenticatedEmail == null ||
                authenticatedEmail.isBlank()) {
            throw new UniRideException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized request");
        }

        String email = authenticatedEmail.trim();

        if (!personRepository.existsByEmailIgnoreCase(email)) {
            throw new UniRideException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized request");
        }
    }
}
