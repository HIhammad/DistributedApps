package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.BookingManager;
import be.kuleuven.distributedsystems.cloud.auth.WebSecurityConfig;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.cloud.firestore.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
public class RestRcpController {

    WebClient.Builder webClient;
    String baseUrl = "http://reliable.westeurope.cloudapp.azure.com";
    String key = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";
    BookingManager bookingManager;

    @Autowired
    RestRcpController(WebClient.Builder webClientBuilder, BookingManager bookingManager)
    {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl);
        this.bookingManager = bookingManager;
    }

    @GetMapping("/api/getFlights")
    Collection<Flight> getFlights() {
        var flights = this.webClient
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights")
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Flight>>() {})
                .block().getContent();

        return flights;
    }

    @GetMapping("/api/getFlight")
    Flight getFlightById(@RequestParam String airline, @RequestParam String flightId) {
        var flight = this.webClient
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights", flightId)
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<EntityModel<Flight>>() {})
                .block().getContent();

        return flight;
    }

    @GetMapping("/api/getFlightTimes")
    Collection<String> getTimes(@RequestParam String airline, @RequestParam String flightId) {
        var times = this.webClient
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights", flightId, "times")
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<String>>() {})
                .block().getContent();

        return times;
    }

    @GetMapping("/api/getAvailableSeats")
    HashMap<String, Collection<Seat>> getAvailableSeats(@RequestParam String airline, @RequestParam String flightId, @RequestParam String time) {
        var seats = this.webClient
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights", flightId, "seats")
                        .queryParam("time", time)
                        .queryParam("available",true)
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                .block().getContent();

        seats = seats.stream().sorted(new Comparator<Seat>() {
            @Override
            public int compare(Seat seat, Seat t1) {

                for(int i=0; i<seat.getName().length(); i++) {
                    Character row1 = seat.getName().charAt(i);
                    Character row2 = t1.getName().charAt(i);
                    if (row1 > row2) return 1;
                    if (row1 < row2) return -1;
                }
                return 0;
            }
        }).toList();

        HashMap<String, Collection<Seat>> answer = new HashMap<String, Collection<Seat>>(); // NEEDS SORTING

        for (Seat seat : seats)
        {
            if(answer.containsKey(seat.getType()))
            {
                answer.get(seat.getType()).add(seat);
            }
            else
            {
                ArrayList<Seat> newArray = new ArrayList<>();
                newArray.add(seat);
                answer.put(seat.getType(), newArray);
            }
        }

        return answer;
    }

    @GetMapping("/api/getSeat")
    Seat getSeat(@RequestParam String airline, @RequestParam String flightId, @RequestParam String seatId) {
        var seat = this.webClient
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights", flightId,"seats", seatId)
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<EntityModel<Seat>>() {})
                .block().getContent();

        return seat;
    }

    @PostMapping("/api/confirmQuotes")
    void confirmQuotes(@RequestBody Collection<Quote> quotes) {

        boolean allQuotesConfirmed = true;

        UUID bookingRef = bookingManager.getNewUUID();
        ArrayList<Ticket> tickets = new ArrayList<Ticket>();
        ArrayList<Ticket> ticketsProcess = new ArrayList<Ticket>();


        for (Quote quote : quotes) {
                var ticket = this.webClient
                        .build()
                        .put()
                        .uri(uriBuilder -> uriBuilder
                                .pathSegment("flights", quote.getFlightId().toString(), "seats", quote.getSeatId().toString(), "ticket")
                                .queryParam("customer", WebSecurityConfig.getUser().getEmail())
                                .queryParam("bookingReference", bookingRef)
                                .queryParam("key", key)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<EntityModel<Ticket>>() {
                        })
                        .block().getContent();
                if(ticket !=null) {
                    ticketsProcess.add(ticket);
                } else{
                    allQuotesConfirmed = false;
                    ticketsProcess.clear();
                    break;
                }
            }

        if(allQuotesConfirmed) {
            tickets.addAll(ticketsProcess);
            Booking booking = new Booking(bookingRef, LocalDateTime.now(), tickets, WebSecurityConfig.getUser().getEmail());
            bookingManager.addBooking(booking);
            } else {
               System.err.println("Failed to reserve all quotes.");
            }

    }

    @GetMapping("/api/getBookings")
    Collection<Booking> getBookings() {
        return bookingManager.getBookingOfUser(WebSecurityConfig.getUser().getEmail());
    }

    @GetMapping("/api/getAllBookings")
    Collection<Booking> getAllBookings() {
        if (WebSecurityConfig.getUser().isManager()) {
            return bookingManager.getBookings();
        }
        return new ArrayList<>();
    }

    @GetMapping("/api/getBestCustomers")
    Collection<String> getBestCustomers() {
        if (WebSecurityConfig.getUser().isManager()) {
            return bookingManager.getBestCustomer();
        }
        return new ArrayList<>();
    }
}
