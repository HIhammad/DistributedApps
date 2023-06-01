package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.BookingManager;
import be.kuleuven.distributedsystems.cloud.auth.WebSecurityConfig;
import be.kuleuven.distributedsystems.cloud.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.http.HttpStatus;

import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@EnableRetry
@RestController
public class RestRcpController {

    WebClient.Builder webClient;
    String baseUrl = "http://reliable.westeurope.cloudapp.azure.com/";
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
    @Retryable(value = {WebClientResponseException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    Collection<Flight> getFlights() {
        try {
            return this.webClient
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("flights")
                            .queryParam("key", key)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatus::is5xxServerError, response -> {
                        // Retry for 5xx Server Errors
                        return Mono.error((Supplier<? extends Throwable>) response.createException());
                    })
                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<Flight>>() {})
                    .block()
                    .getContent();
        } catch (WebClientResponseException ex) {
            throw ex;
        }
    }

    @Recover
    Collection<Flight> handleGetFlightsFailure(WebClientResponseException ex) {
        return Collections.emptyList(); // Return a fallback value
    }


    @GetMapping("/api/getFlight")
    @Retryable(value = {WebClientResponseException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    Flight getFlightById(@RequestParam String airline, @RequestParam String flightId) {
        try {
            return this.webClient
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("flights", flightId)
                            .queryParam("key", key)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatus::is5xxServerError, response -> {
                        // Retry for 5xx Server Errors
                        return Mono.error(new RuntimeException("Server Error"));
                    })
                    .bodyToMono(new ParameterizedTypeReference<EntityModel<Flight>>() {})
                    .block()
                    .getContent();
        } catch (WebClientResponseException ex) {
            // Log or handle the exception from the Unreliable Airline
            throw new RuntimeException("Exception occurred during WebClient request");
        }
    }

    @Recover
    Flight handleGetFlightByIdFailure(WebClientResponseException ex) {
        return null; // Return a fallback value
    }



    @GetMapping("/api/getFlightTimes")
    Collection<String> getTimes(@RequestParam String airline, @RequestParam String flightId) {
        try {
            return getFlightTimesWithRetries(airline, flightId);
        } catch (WebClientResponseException ex) {
            // Handle the exception or return an appropriate default value
            return Collections.emptyList();
        }
    }

    @Retryable(value = {WebClientResponseException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private Collection<String> getFlightTimesWithRetries(String airline, String flightId) {
        return this.webClient
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("flights", flightId, "times")
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    // Retry for 5xx Server Errors
                    return Mono.error(new RuntimeException("Server Error"));
                })
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<String>>() {})
                .block()
                .getContent();
    }

    @Recover
    Collection<String> handleGetFlightTimesFailure(RuntimeException ex) {
        // Log or handle the exception from the Unreliable Airline
        return Collections.emptyList(); // Return a fallback value or handle the failure appropriately
    }


    @GetMapping("/api/getAvailableSeats")
    @Retryable(value = {WebClientResponseException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    HashMap<String, Collection<Seat>> getAvailableSeats(@RequestParam String airline, @RequestParam String flightId, @RequestParam String time) {
        try {
            var seats = this.webClient
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("flights", flightId, "seats")
                            .queryParam("time", time)
                            .queryParam("available", true)
                            .queryParam("key", key)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatus::is5xxServerError, response -> {
                        // Retry for 5xx Server Errors
                        return Mono.error(new RuntimeException("Server Error"));
                    })
                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                    .block()
                    .getContent();

            seats = seats.stream().sorted(Comparator.comparing(Seat::getName)).toList();

            HashMap<String, Collection<Seat>> answer = new HashMap<>(); // NEEDS SORTING

            for (Seat seat : seats) {
                answer.computeIfAbsent(seat.getType(), k -> new ArrayList<>()).add(seat);
            }

            return answer;
        } catch (WebClientResponseException ex) {
            // Log or handle the exception from the Unreliable Airline
            throw new RuntimeException("Exception occurred during WebClient request");
        }
    }

    @Recover
    HashMap<String, Collection<Seat>> handleGetAvailableSeatsFailure(RuntimeException ex) {
        // Log or handle the exception from the Unreliable Airline
        return new HashMap<>(); // Return a fallback value or handle the failure appropriately
    }



    @GetMapping("/api/getSeat")
    @Retryable(value = {WebClientResponseException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    Seat getSeat(@RequestParam String airline, @RequestParam String flightId, @RequestParam String seatId) {
        try {
            return this.webClient
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("flights", flightId, "seats", seatId)
                            .queryParam("key", key)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatus::is5xxServerError, response -> {
                        // Retry for 5xx Server Errors
                        return Mono.error(new RuntimeException("Server Error"));
                    })
                    .bodyToMono(new ParameterizedTypeReference<EntityModel<Seat>>() {})
                    .block()
                    .getContent();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Exception occurred during WebClient request");
        }
    }

    @Recover
    Seat handleGetSeatFailure(RuntimeException ex) {
        return null; // Return a fallback value
    }



    @PostMapping("/api/confirmQuotes")
    @Retryable(value = {WebClientResponseException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    void confirmQuotes(@RequestBody Collection<Quote> quotes) {
        try {
            UUID bookingRef = bookingManager.getNewUUID();
            ArrayList<Ticket> tickets = new ArrayList<>();
            boolean allQuotesConfirmed = true;
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
                        .onStatus(HttpStatus::is5xxServerError, response -> {
                            // Retry for 5xx Server Errors
                            return Mono.error(new RuntimeException("Server Error"));
                        })
                        .bodyToMono(new ParameterizedTypeReference<EntityModel<Ticket>>() {})
                        .block()
                        .getContent();

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
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Exception occurred during WebClient request");
        }
    }

    @Recover
    void handleConfirmQuotesFailure(RuntimeException ex) {
        System.out.println("Quotes couldn't be confirmed");
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
