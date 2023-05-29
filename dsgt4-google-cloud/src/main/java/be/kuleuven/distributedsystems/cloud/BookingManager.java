package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.User;
import com.google.api.client.util.ArrayMap;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
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
        } catch (InterruptedException | ExecutionException e) {
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

    public Collection<String> getBestCustomer()
    {
        HashMap<String, Integer> customerMap = new HashMap<>();

        for (Booking booking : this.bookings)
        {
            if (customerMap.containsKey(booking.getCustomer()))
            {
                customerMap.put(booking.getCustomer(), customerMap.get(booking.getCustomer()) + 1);
            }
            customerMap.put(booking.getCustomer(), 1);
        }

        ArrayList<String> output = new ArrayList<String>();
        int maxvalue = 0;

        for (String user : customerMap.keySet())
        {
            if (customerMap.get(user) > maxvalue)
            {
                output.clear();
                output.add(user);
            }
            else if (customerMap.get(user) == maxvalue)
            {
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
        for (Booking booking : this.bookings)
        {
            if (booking.getId().equals(new_uuid)) return false;
        }
        return true;
    }

    public List<Booking> getBookings() {
        CollectionReference bookingsCollection = firestore.collection("bookings");
        ApiFuture<QuerySnapshot> querySnapshot = bookingsCollection.get();
        List<QueryDocumentSnapshot> documents;
        try {
            documents = querySnapshot.get().getDocuments();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error getting booking documents: " + e.getMessage());
            return new ArrayList<>(); // Return an empty list if an error occurs
        }

        List<Booking> bookings = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            Booking booking = document.toObject(Booking.class);
            if (booking != null) {
                bookings.add(booking);
            }
        }

        return bookings;
    }


    public void addBooking(Booking booking) {

        CollectionReference bookingsCollection = firestore.collection("bookings");
        DocumentReference bookingDoc = bookingsCollection.document(booking.getId().toString());

        Map<String, Object> data = new HashMap<>();
        data.put("id", booking.getId().toString());
        data.put("time", booking.getTime().toString());
        //data.put("id", booking.getId());
        //data.put("time", booking.getTime()); how to send it as a time
        data.put("tickets", booking.getTickets());
        data.put("customer", booking.getCustomer());

        ApiFuture<WriteResult> result = bookingDoc.set(data);
        try {
            WriteResult writeResult = result.get();
            // Write operation completed successfully
            System.out.println("Booking added to Firestore: " + booking.getId());
        } catch (InterruptedException | ExecutionException e) {
            // Handle exceptions
            System.err.println("Error adding booking to Firestore: " + e.getMessage());
        }

        this.bookings.add(booking);
    }

    public void setFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

}
