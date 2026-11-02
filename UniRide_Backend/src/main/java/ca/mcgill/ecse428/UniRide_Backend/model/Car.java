package ca.mcgill.ecse428.UniRide_Backend.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * A driver's car. A car always has a driver, and a driver has at most one car
 * (enforced by the unique {@code driver_id} column).
 */
@Entity
@Table(name = "car")
public class Car {

    public static final String DRIVER_REQUIRED_MESSAGE = "A car must have a driver";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver;

    @Column(nullable = false, length = 50)
    private String make;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false, length = 30)
    private String color;

    /**
     * Required and unique, ignoring case, spaces and hyphens (unique index
     * {@code car_license_plate_key}). Stored trimmed and uppercase, e.g. "ABC 123".
     */
    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    /** Total seats in the car, driver's seat included (always greater than 1). */
    @Column(name = "seat_capacity", nullable = false)
    private int seatCapacity;

    protected Car() {
    }

    public Car(Driver driver, String make, String model, String color, String licensePlate, int seatCapacity) {
        this.driver = Objects.requireNonNull(driver, DRIVER_REQUIRED_MESSAGE);
        this.make = make;
        this.model = model;
        this.color = color;
        this.licensePlate = licensePlate;
        this.seatCapacity = seatCapacity;
    }

    public Integer getId() {
        return id;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = Objects.requireNonNull(driver, DRIVER_REQUIRED_MESSAGE);
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public int getSeatCapacity() {
        return seatCapacity;
    }

    public void setSeatCapacity(int seatCapacity) {
        this.seatCapacity = seatCapacity;
    }
}
