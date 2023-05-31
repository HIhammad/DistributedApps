package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.User;
import com.google.api.client.util.ArrayMap;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.internal.NonNull;
import org.springframework.stereotype.Component;

import java.beans.Customizer;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
public class BookingManager {
    ArrayList<Booking> bookings;

    public Firestore getFirestore() {
        return firestore;
    }
    private Firestore firestore;

    public BookingManager()
    {

        this.bookings = new ArrayList<>();
    }

    public Collection<Booking> getBookingOfUser(String userEmail) {
        CollectionReference bookingsCollection = firestore.collection("bookings");
        Query query = bookingsCollection.whereEqualTo("customer", userEmail);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents;
        try {
            documents = querySnapshot.get().getDocuments();
        } catch (Exception e) {
            System.err.println("Error getting booking documents: " + e.getMessage());
            return new ArrayList<>(); // Return an empty list if an error occurs
        }

        List<Booking> bookingsOfUser = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            Map<String, Object> bookingMap = document.getData();
            if (bookingMap != null) {
                Booking booking = new Booking();
                booking.setBookingInfoFromMap(bookingMap);
                booking.setTicketsFromMap((List<Map<String, String>>) bookingMap.get("tickets"));
                bookingsOfUser.add(booking);
            }
        }

        return bookingsOfUser;
    }

    public Collection<String> getBestCustomer() {
        List<Booking> cBookings = getBookings();
        HashMap<String, Integer> customerMap = new HashMap<>();

        for (Booking booking : cBookings) {
            String customer = booking.getCustomer();
            customerMap.put(customer, customerMap.getOrDefault(customer, 0) + 1);
        }

        List<String> output = new ArrayList<>();
        int maxvalue = 0;

        for (String user : customerMap.keySet()) {
            int count = customerMap.get(user);
            if (count > maxvalue) {
                output.clear();
                output.add(user);
                maxvalue = count;
            } else if (count == maxvalue) {
                output.add(user);
            }
        }
        return output;
    }


    public UUID getNewUUID()
    {
        UUID new_uuid;
        do {
            new_uuid = UUID.randomUUID();
        } while (!isNotPresent(new_uuid));

        return new_uuid;
    }

    private boolean isNotPresent(UUID new_uuid)
    {
        List<Booking> newBookings = getBookings();
        for (Booking booking : newBookings)
        {
            if (booking.getId().equals(new_uuid)) return false;
        }
        return true;
    }

    public List<Booking> getBookings() {
        CollectionReference bookingsCollection = firestore.collection("bookings");
        List<Booking> allBookings = new ArrayList<>();
        try {
            firestore.runTransaction(transaction -> {
                QuerySnapshot querySnapshot = transaction.get(bookingsCollection).get();
                List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();

                for (DocumentSnapshot document : documents) {
                    Map<String, Object> bookingMap = document.getData();
                    if (bookingMap != null) {
                        Booking booking = new Booking();
                        booking.setBookingInfoFromMap(bookingMap);
                        booking.setTicketsFromMap((List<Map<String, String>>) bookingMap.get("tickets"));
                        allBookings.add(booking);
                    }
                }
                return null;
            }).get();
        } catch (Exception e) {
            System.err.println("Error getting booking documents: " + e.getMessage());
            return new ArrayList<>();
        }

        return allBookings;
    }



    public void addBooking(Booking booking) {

        CollectionReference bookingsCollection = firestore.collection("bookings");
        DocumentReference bookingDoc = bookingsCollection.document(booking.getId().toString());

        Map<String, Object> data = new HashMap<>();
        data.put("id", booking.getId().toString());
        data.put("time", booking.getTime().toString());
        data.put("tickets", booking.getTickets());
        data.put("customer", booking.getCustomer());


        ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(bookingDoc).get();
            if (!snapshot.exists()) {
                transaction.set(bookingDoc, data);
                //change log
                CollectionReference changeLogsCollection = firestore.collection("changeLogs");
                DocumentReference changeLogDoc = changeLogsCollection.document();
                Map<String, Object> changeLogData = new HashMap<>();
                changeLogData.put("bookingId", booking.getId().toString());
                changeLogData.put("action", "add");
                changeLogData.put("timestamp", FieldValue.serverTimestamp());
                transaction.set(changeLogDoc, changeLogData);
                //
                return "Booking added to Firestore: " + booking.getId();
            } else {
                throw new Exception("Booking already exists.");
            }
        });

        try {
            System.out.println(futureTransaction.get());
            this.bookings.add(booking);
        } catch (Exception e) {
            System.err.println("Error adding booking to Firestore: " + e.getMessage());
        }

        this.bookings.add(booking);
    }


    public void setFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

}
