package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.cloud.firestore.*;
import com.google.api.core.ApiFuture;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DataInitializer {

    public static boolean isDataInitialized(Firestore firestore) {
        // Check if a known document exists in the Firestore collection
        DocumentSnapshot snapshot;
        try {
            snapshot = firestore.collection("bookings").document("exampleBookingId").get().get();
        } catch (Exception e) {
            return false;
        }
        return snapshot.exists();
    }

    public static void parseAndAddData(Firestore firestore) throws IOException, ExecutionException, InterruptedException {
        // Read data.json file
        InputStream dataInputStream = new ClassPathResource("data.json").getInputStream();
        Gson gson = new Gson();
        JsonElement jsonElement = gson.fromJson(new InputStreamReader(dataInputStream), JsonElement.class);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        System.out.println("inside method parse and add data");

        // Parse and add data to Firestore
        if (jsonObject.has("flights")) {
            JsonArray flightsArray = jsonObject.getAsJsonArray("flights");
            System.out.println("inside if");
            for (JsonElement flightElement : flightsArray) {
                JsonObject flightObject = flightElement.getAsJsonObject();

                // Extract flight information
                String name = flightObject.get("name").getAsString();
                String location = flightObject.get("location").getAsString();
                String image = flightObject.get("image").getAsString();

                // Create a collection and document reference for the flight
                System.out.println("inside for loop");
                CollectionReference InternalFlights = firestore.collection("InternalFlights");
                DocumentReference flightDocRef = InternalFlights.document(name);

                // Check if the flight already exists in Firestore
                DocumentSnapshot flightSnapshot = flightDocRef.get().get();

                if (!flightSnapshot.exists()) {
                    // Flight does not exist, add it to Firestore
                    Map<String, Object> flightData = new HashMap<>();
                    flightData.put("name", name);
                    flightData.put("location", location);
                    flightData.put("image", image);

                    ApiFuture<WriteResult> result = flightDocRef.set(flightData);
                    try {
                        WriteResult writeResult = result.get();
                        // Write operation completed successfully
                        System.out.println("Flight added to Firestore: " + name);
                    } catch (InterruptedException | ExecutionException e) {
                        // Handle exceptions
                        System.err.println("Error adding flight to Firestore: " + e.getMessage());
                    }

                    // Add seats information for the flight
                    if (flightObject.has("seats")) {
                        JsonArray seatsArray = flightObject.getAsJsonArray("seats");
                        CollectionReference seatsCollection = flightDocRef.collection("seats");
                        for (JsonElement seatElement : seatsArray) {
                            JsonObject seatObject = seatElement.getAsJsonObject();

                            // Extract seat information
                            String time = seatObject.get("time").getAsString();
                            String type = seatObject.get("type").getAsString();
                            String seatName = seatObject.get("name").getAsString();
                            double price = seatObject.get("price").getAsDouble();

                            // Create a document reference for the seat
                            DocumentReference seatDocRef = seatsCollection.document(seatName);

                            // Check if the seat already exists in Firestore
                            DocumentSnapshot seatSnapshot = seatDocRef.get().get();
                            if (!seatSnapshot.exists()) {
                                // Seat does not exist, add it to Firestore
                                Map<String, Object> seatData = new HashMap<>();
                                seatData.put("time", time);
                                seatData.put("type", type);
                                seatData.put("name", seatName);
                                seatData.put("price", price);

                                seatDocRef.set(seatData);
                                System.out.println("Added seat: " + seatName + " for flight: " + name);
                            }
                        }
                    } else {
                        System.out.println("No seats found for flight: " + name);
                    }
                }
            }
        }
    }



    public static void initializeDataIfNeeded(Firestore firestore) throws IOException, ExecutionException, InterruptedException {
        System.out.println("inside initializeDataIfNeeded");
        if (!isDataInitialized(firestore)) {
            System.out.println("data not initialized");
            parseAndAddData(firestore);
        }
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        Firestore firestore = Application.firestore();

        // Call the initializeDataIfNeeded method to check and initialize the data
        initializeDataIfNeeded(firestore);
    }
}

