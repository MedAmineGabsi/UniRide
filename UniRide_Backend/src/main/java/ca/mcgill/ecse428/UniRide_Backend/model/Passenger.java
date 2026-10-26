package ca.mcgill.ecse428.UniRide_Backend.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "passenger")
@DiscriminatorValue("PASSENGER")
public class Passenger extends PersonRole {
}
