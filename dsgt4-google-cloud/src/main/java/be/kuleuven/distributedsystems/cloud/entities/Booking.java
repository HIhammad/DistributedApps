package be.kuleuven.distributedsystems.cloud.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Booking {
    private UUID id;
    private LocalDateTime time;
    private List<Ticket> tickets;
    private String customer;



    public Booking(UUID id, LocalDateTime time, List<Ticket> tickets, String customer) {
        this.id = id;
        this.time = time;
        this.tickets = tickets;
        this.customer = customer;
    }

    public Booking() {
        // Empty constructor
    }
    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDateTime getTime() {
        return this.time;
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public String getCustomer() {
        return this.customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public void setTicketsFromMap(List<Map<String, String>> ticketMaps) {
        this.tickets = ticketMaps.stream()
                .map(map -> {
                    Ticket ticket = new Ticket();
                    ticket.setAirline( map.get("airline"));
                    ticket.setFlightId(UUID.fromString((map.get("flightId"))));
                    ticket.setSeatId(UUID.fromString(map.get("seatId")));
                    ticket.setTicketId(UUID.fromString(map.get("ticketId")));
                    ticket.setCustomer(map.get("customer"));
                    ticket.setBookingReference(map.get("bookingReference"));
                    return ticket;
                })
                .collect(Collectors.toList());
    }

    public void setBookingInfoFromMap(Map<String, Object> bookingMap) {
        this.customer = (bookingMap.get("customer")).toString();
        this.id = UUID.fromString(bookingMap.get("id").toString());

        String timeString = bookingMap.get("time").toString();
        String truncatedTimeString = timeString.substring(0, 19);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        this.time = LocalDateTime.parse(truncatedTimeString, formatter);
    }

}
