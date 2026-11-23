package ca.mcgill.ecse428.UniRide_Backend.dto;

import ca.mcgill.ecse428.UniRide_Backend.model.Car;

/** Vehicle details returned without serializing the driver's account or JPA relationships. */
public record CarResponse(
        Integer id,
        Integer driverId,
        String make,
        String model,
        String color,
        String licensePlate,
        int seatCapacity) {

    public static CarResponse from(Car car) {
        return new CarResponse(
                car.getId(),
                car.getDriver().getId(),
                car.getMake(),
                car.getModel(),
                car.getColor(),
                car.getLicensePlate(),
                car.getSeatCapacity());
    }
}
