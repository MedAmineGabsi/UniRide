package ca.mcgill.ecse428.UniRide_Backend.dto;

import java.math.BigDecimal;

import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.RideStatus;
import ca.mcgill.ecse428.UniRide_Backend.service.RideValidator;

public record RideResponse(
        Integer id,
        Integer driverId,
        String origin,
        String destination,
        String departureDateTime,
        int durationMinutes,
        int totalSeats,
        BigDecimal pricePerSeat,
        RideStatus status) {

    public static RideResponse from(Ride ride) {
        return new RideResponse(
                ride.getId(),
                ride.getDriver().getId(),
                ride.getOrigin(),
                ride.getDestination(),
                ride.getDepartureDateTime().format(RideValidator.DEPARTURE_FORMAT),
                ride.getDurationMinutes(),
                ride.getTotalSeats(),
                ride.getPricePerSeat(),
                ride.getStatus());
    }
}
