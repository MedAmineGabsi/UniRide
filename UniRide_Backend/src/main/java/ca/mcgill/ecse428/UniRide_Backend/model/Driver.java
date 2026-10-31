package ca.mcgill.ecse428.UniRide_Backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "driver")
@DiscriminatorValue("DRIVER")
public class Driver extends PersonRole {

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    /**
     * Passenger seats in the driver's car (driver's own seat excluded).
     * Null when unknown; a ride cannot offer more seats than this.
     */
    @Column(name = "car_seats")
    private Integer carSeats;

    protected Driver() {
    }

    public Driver(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public Driver(String licenseNumber, Integer carSeats) {
        this.licenseNumber = licenseNumber;
        this.carSeats = carSeats;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public Integer getCarSeats() {
        return carSeats;
    }

    public void setCarSeats(Integer carSeats) {
        this.carSeats = carSeats;
    }
}
