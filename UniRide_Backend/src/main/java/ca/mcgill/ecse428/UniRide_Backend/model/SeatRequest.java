package ca.mcgill.ecse428.UniRide_Backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "seat_request")
public class SeatRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(name = "seats_requested", nullable = false)
    private int seatsRequested;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatRequestStatus status = SeatRequestStatus.PENDING;

    protected SeatRequest() {
    }

    public SeatRequest(Passenger passenger, Ride ride, int seatsRequested) {
        this.passenger = passenger;
        this.ride = ride;
        this.seatsRequested = seatsRequested;
    }

    public Integer getId() {
        return id;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public int getSeatsRequested() {
        return seatsRequested;
    }

    public void setSeatsRequested(int seatsRequested) {
        this.seatsRequested = seatsRequested;
    }

    public SeatRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SeatRequestStatus status) {
        this.status = status;
    }
}
