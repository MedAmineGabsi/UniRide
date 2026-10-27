package ca.mcgill.ecse428.UniRide_Backend.dto;

import java.math.BigDecimal;

/**
 * Body of POST /api/rides.
 *
 * <p>Fields are boxed/strings on purpose so that missing or malformed values reach
 * {@code RideValidator} and come back as field errors instead of a parse failure.
 *
 * @param departureDateTime "yyyy-MM-dd HH:mm" (or "yyyy-MM-ddTHH:mm"), local Montréal time
 * @param durationMinutes   estimated trip duration in minutes
 */
public record CreateRideRequest(
        String origin,
        String destination,
        String departureDateTime,
        Integer durationMinutes,
        Integer totalSeats,
        BigDecimal pricePerSeat) {
}
