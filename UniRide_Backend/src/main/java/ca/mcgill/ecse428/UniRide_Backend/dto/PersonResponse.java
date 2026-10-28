package ca.mcgill.ecse428.UniRide_Backend.dto;

import java.util.List;

public record PersonResponse(
        Integer id,
        String studentId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        List<RoleType> roles) {
}
