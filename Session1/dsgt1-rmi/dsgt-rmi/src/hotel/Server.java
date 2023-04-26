package hotel;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
public class Server {
    public Server() {}

    public static void main(String[] args) {
        try {
            IBooking obj = new BookingManager();
            //IBooking stub = (IBooking) UnicastRemoteObject.exportObject(obj, 1089);

            // Bind the remote object's stub in the registry
            //Registry registry = LocateRegistry.getRegistry();
            Registry registry = LocateRegistry.createRegistry(1089);
            registry.bind("BookingManager", obj);

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    
}
