package ca.mcgill.ecse428.UniRide_Backend.dto;

/**
 * Car details sent when a driver registers their car.
 *
 * <p>Fields are boxed/strings on purpose so that missing values reach {@code CarValidator}
 * and come back as field errors instead of a parse failure.
 *
 * @param seatCapacity total seats in the car, driver's seat included
 */
public record CreateCarRequest(
        String make,
        String model,
        String color,
        String licensePlate,
        Integer seatCapacity) {
}
