package be.kuleuven.distributedsystems.cloud.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Ticket {
    private String airline;
    private UUID flightId;
    private UUID seatId;
    private UUID ticketId;
    private String customer;
    private String bookingReference;

    public Ticket() {
    }

    public Ticket(String airline, UUID flightId, UUID seatId, UUID ticketId, String customer, String bookingReference) {
        this.airline = airline;
        this.flightId = flightId;
        this.seatId = seatId;
        this.ticketId = ticketId;
        this.customer = customer;
        this.bookingReference = bookingReference;
    }

    public String getAirline() {
        return airline;
    }

    public String getFlightId() {
        return flightId.toString();
    }

    public String getSeatId() {
        return this.seatId.toString();
    }

    public String getTicketId() {
        return this.ticketId.toString();
    }

    public String getCustomer() {
        return this.customer;
    }

    public String getBookingReference() {
        return this.bookingReference;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }


    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public void setFlightId(UUID flightId) {
        this.flightId = flightId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public void setSeatId(UUID seatId) {
        this.seatId = seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ticket)) {
            return false;
        }
        var other = (Ticket) o;
        return this.ticketId.equals(other.ticketId)
                && this.seatId.equals(other.seatId)
                && this.flightId.equals(other.flightId)
                && this.airline.equals(other.airline);
    }

    @Override
    public int hashCode() {
        return this.airline.hashCode() * this.flightId.hashCode() * this.seatId.hashCode() * this.ticketId.hashCode();
    }
}
