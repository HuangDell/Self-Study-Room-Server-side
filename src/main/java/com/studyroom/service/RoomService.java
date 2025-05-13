package com.studyroom.service;

import com.studyroom.dto.RoomRequest;
import com.studyroom.model.Booking;
import com.studyroom.model.Room;
import com.studyroom.model.Seat;
import com.studyroom.model.Student;
import com.studyroom.repository.BookingRepository;
import com.studyroom.repository.RoomRepository;
import com.studyroom.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;

    public Room createRoom(RoomRequest roomRequest) {
        // 检查自习室是否已存在
        if (roomRepository.findByName(roomRequest.getRoomName()).isPresent()) {
            throw new RuntimeException("Room already exists");
        }

        Room room = new Room();
        room.setName(roomRequest.getRoomName());
        room.setType(roomRequest.getType());
        room.setCapacity(roomRequest.getCapacity());
        room.setOpenTime(Instant.ofEpochMilli(roomRequest.getOpenTime()));
        room.setCloseTime(Instant.ofEpochMilli(roomRequest.getCloseTime()));
        room.setLocation(roomRequest.getLocation());
        room.setStatus(roomRequest.getStatus());

        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        List<Seat> seats = seatRepository.findByRoomId(roomId);
        for (Seat seat : seats) {
            bookingRepository.deleteBySeatId(seat.getId());
            seatRepository.delete(seat);
        }
        roomRepository.delete(room);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room updateRoom(Long roomId, RoomRequest roomRequest) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (roomRequest.getOpenTime() != null) {
            room.setOpenTime(Instant.ofEpochMilli(roomRequest.getOpenTime()));
        }

        if (roomRequest.getCloseTime() != null) {
            room.setCloseTime(Instant.ofEpochMilli(roomRequest.getCloseTime()));
        }

        if (roomRequest.getStatus() != null) {
            room.setStatus(roomRequest.getStatus());
        }

        if (roomRequest.getCapacity() != null){
            room.setCapacity(roomRequest.getCapacity());
        }

        if (roomRequest.getLocation() != null) {
            room.setLocation(roomRequest.getLocation());
        }

        if (roomRequest.getType() != null) {
            room.setType(roomRequest.getType());
        }

        if (roomRequest.getRoomName() != null) {
            room.setName(roomRequest.getRoomName());
        }

        return roomRepository.save(room);
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }

    public Map<Room, Long> getRoomsWithAvailableSeats() {
        List<Room> rooms = roomRepository.findAll();
        return rooms.stream().collect(Collectors.toMap(
                room -> room,
                room -> seatRepository.findByRoomId(room.getId()).stream()
                        .filter(seat -> seat.getStatus() == Seat.SeatStatus.AVAILABLE)
                        .count()
        ));
    }

    public List<Seat> searchSeats(String query) {
        return seatRepository.searchSeats(query);
    }

//    @Transactional
//    public Booking bookSeat(Student student, Long roomId, Long seatId,
//                            Long startTime, Long endTime) {
//        Room room = roomRepository.findById(roomId)
//                .orElseThrow(() -> new RuntimeException("Room not found"));
//
//        Seat seat = seatRepository.findById(seatId)
//                .orElseThrow(() -> new RuntimeException("Seat not found"));
//
//        if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
//            throw new RuntimeException("Seat is not available");
//        }
//
//        seat.setStatus(Seat.SeatStatus.OCCUPIED);
//        seatRepository.save(seat);
//
//        Booking booking = new Booking();
//        booking.setStudent(student);
//        booking.setSeat(seat);
//        booking.setStartTime(Instant.ofEpochMilli(startTime));
//        booking.setEndTime(Instant.ofEpochMilli(endTime));
//
//        return bookingRepository.save(booking);
//    }





}