package ca.mcgill.ecse428.UniRide_Backend.dto;

import java.util.List;

/** licenseNumber is only used when roles contains DRIVER. */
public record RegisterUserRequest(
        String firstName,
        String lastName,
        String studentId,
        String email,
        String phoneNumber,
        String password,
        String passwordConfirmation,
        List<RoleType> roles,
        String licenseNumber) {
}
