package hotel;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BookingManager implements IBooking {

	private final Room[] rooms;
	//private BookingDetail[] bookings;
	public BookingManager() {
		this.rooms = initializeRooms();
	}

	public Set<Integer> getAllRooms() {
		Set<Integer> allRooms = new HashSet<Integer>();
		Room[] roomIterator = rooms;
		for (Room room : roomIterator) {
			allRooms.add(room.getRoomNumber());
		}
		return allRooms;
	}

	public boolean isRoomAvailable(Integer roomNumber, LocalDate date) {
		//implement this method
		for(Room room: rooms){
			if(room.getRoomNumber().equals(roomNumber)){
				if(getAvailableRooms(date).contains(room.getRoomNumber())){
					return true;
				}
			}
		}
		return false;
	}

	public void addBooking(BookingDetail bookingDetail) {
		for (Room room : rooms) {
			if (room.getRoomNumber().equals(bookingDetail.getRoomNumber())) {
				if (!isRoomAvailable(room.getRoomNumber(), bookingDetail.getDate())) {
					throw new IllegalArgumentException("Room is not available on the specified date.");
				}
				room.getBookings().add(bookingDetail);
				return;
			}
		}
		throw new IllegalArgumentException("Room not found.");
	}

	public Set<Integer> getAvailableRooms(LocalDate date) {
		//implement this method
		Set<Integer> allAvailableRooms = new HashSet<Integer>();
		for (Room room : rooms) {
			boolean available = true;
			for (BookingDetail booking : room.getBookings()) {
				if (booking.getDate().equals(date)) {
					available = false;
					//allAvailableRooms.remove(room.getRoomNumber());
					break;
				}
			}
			if(available){
				allAvailableRooms.add(room.getRoomNumber());
			}
		}
		return allAvailableRooms;
	}

	private static Room[] initializeRooms() {
		Room[] rooms = new Room[4];
		rooms[0] = new Room(101);
		rooms[1] = new Room(102);
		rooms[2] = new Room(201);
		rooms[3] = new Room(203);
		return rooms;
	}


}
