package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.User;
import com.google.api.client.util.ArrayMap;
import org.springframework.stereotype.Component;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import java.beans.Customizer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

@Component
public class BookingManager {

    public BookingManager()
    {
  
        this.bookings = new ArrayList<>();
    }

    public Collection<Booking> getBookingOfUser(String userEmail)
    {
        ArrayList<Booking> bookingsOfUser = new ArrayList<>();
        for (Booking booking : this.bookings)
        {
            if (booking.getCustomer().equals(userEmail)) bookingsOfUser.add(booking);
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

    public ArrayList<Booking> getBookings() {
        return bookings;
    }

    public void addBooking(Booking booking)
    {


        this.bookings.add(booking);
    }

    ArrayList<Booking> bookings;




}
