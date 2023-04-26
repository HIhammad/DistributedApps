package staff;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.Set;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import hotel.BookingDetail;
import hotel.BookingManager;
import hotel.IBooking;

public class BookingClient extends AbstractScriptedSimpleTest {

	private BookingManager bm = null;
	private IBooking booking;
	public static void main(String[] args) throws Exception {
		BookingClient client = new BookingClient();
		client.run();
	}

	/***************
	 * CONSTRUCTOR *c
	 ***************/
	public BookingClient() throws NotBoundException, RemoteException {
		try {
			//Look up the registered remote instance
			Registry registry = LocateRegistry.getRegistry(1089);
			booking = (IBooking) registry.lookup("BookingManager");
			//bm = new BookingManager();
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}
/*
	public void run() throws RemoteException, NotBoundException {
		//Registry registry = LocateRegistry.getRegistry(1089);
		//IBooking booking = (IBooking) registry.lookup("BookingManager");
		try {
			super.run();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		//System.out.println("Car Rental Company found.");
		//System.out.println(booking.getAllRooms());
	}
*/
	@Override
	public boolean isRoomAvailable(Integer roomNumber, LocalDate date) throws Exception{
		//Implement this method
		return booking.isRoomAvailable(roomNumber, date);
	}

	@Override
	public void addBooking(BookingDetail bookingDetail) throws Exception {
		//Implement this method
		//BookingDetail bookingDetailRemote = UnicastRemoteObject.exportObject(bookingDetail, 1089);;
		booking.addBooking(bookingDetail);

	}

	@Override
	public Set<Integer> getAvailableRooms(LocalDate date) throws Exception {
		//Implement this method

		return booking.getAvailableRooms(date);
	}

	@Override
	public Set<Integer> getAllRooms() throws Exception{
		return booking.getAllRooms();
	}
}
