package ca.mcgill.ecse428.UniRide_Backend.dto;

import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;

public class SeatRequestDto {

    public static class Request {
        private String passengerEmail;
        private Integer rideId;
        private int seatsRequested;

        public String getPassengerEmail() { return passengerEmail; }
        public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }

        public Integer getRideId() { return rideId; }
        public void setRideId(Integer rideId) { this.rideId = rideId; }

        public int getSeatsRequested() { return seatsRequested; }
        public void setSeatsRequested(int seatsRequested) { this.seatsRequested = seatsRequested; }
    }

    public static class Response {
        private Integer id;
        private Integer passengerId;
        private Integer rideId;
        private int seatsRequested;
        private String status;

        public Response(SeatRequest seatRequest) {
            this.id = seatRequest.getId();
            this.passengerId = seatRequest.getPassenger().getId();
            this.rideId = seatRequest.getRide().getId();
            this.seatsRequested = seatRequest.getSeatsRequested();
            this.status = seatRequest.getStatus().name();
        }

        public Integer getId() { return id; }
        public Integer getPassengerId() { return passengerId; }
        public Integer getRideId() { return rideId; }
        public int getSeatsRequested() { return seatsRequested; }
        public String getStatus() { return status; }
    }
}