package ca.mcgill.ecse428.UniRide_Backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ca.mcgill.ecse428.UniRide_Backend.dto.PersonResponse;
import ca.mcgill.ecse428.UniRide_Backend.dto.RegisterUserRequest;
import ca.mcgill.ecse428.UniRide_Backend.service.PersonService;

@RestController
@RequestMapping("/api/users")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonResponse register(@RequestBody RegisterUserRequest request) {
        return personService.registerUser(request);
    }
}
