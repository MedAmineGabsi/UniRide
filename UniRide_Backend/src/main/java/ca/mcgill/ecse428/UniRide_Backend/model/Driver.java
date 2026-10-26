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

    protected Driver() {
    }

    public Driver(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
}
