// TID075
package ca.mcgill.ecse428.UniRide_Backend.dto;

import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;

public class SeatRequestDto {

    public static class Request {
        private Integer rideId;
        private int seatsRequested;
        private String status; // Added for ID07

        public Integer getRideId() { return rideId; }
        public void setRideId(Integer rideId) { this.rideId = rideId; }

        public int getSeatsRequested() { return seatsRequested; }
        public void setSeatsRequested(int seatsRequested) { this.seatsRequested = seatsRequested; }

        public String getStatus() { return status; } // Added for ID07
        public void setStatus(String status) { this.status = status; } // Added for ID07
    }

    public static class Response {
        private final Integer id;
        private final Integer passengerId;
        private final Integer rideId;
        private final int seatsRequested;
        private final String status;

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