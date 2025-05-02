package com.studyroom.service;

import com.studyroom.dto.BookingRequest;
import com.studyroom.dto.SeatRequest;
import com.studyroom.model.Booking;
import com.studyroom.model.Room;
import com.studyroom.model.Seat;
import com.studyroom.model.Student;
import com.studyroom.repository.BookingRepository;
import com.studyroom.repository.RoomRepository;
import com.studyroom.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public Seat addSeat(SeatRequest seatRequest) {
        Room room = roomRepository.findById(seatRequest.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // 检查座位是否已存在
        if (seatRepository.findByRoomAndSeatNumber(room, seatRequest.getSeatName()).isPresent()) {
            throw new RuntimeException("Seat already exists in this room");
        }

        Seat seat = new Seat();
        seat.setRoom(room);
        seat.setSeatName(seatRequest.getSeatName());
        seat.setSeatNumber(seatRequest.getSeatName());
        seat.setHasSocket(seatRequest.getHasSocket());

        return seatRepository.save(seat);
    }

    public void bookSeat(Student student, BookingRequest bookingRequest) {
        Seat seat = seatRepository.findById(bookingRequest.getSeatId())
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
            throw new RuntimeException("Seat is not available");
        }

        seat.setStatus(Seat.SeatStatus.OCCUPIED);
        seatRepository.save(seat);

        Booking booking = new Booking();
        booking.setStudent(student);
        booking.setSeat(seat);
        booking.setStartTime(Instant.ofEpochMilli(bookingRequest.getStartTime()));
        booking.setEndTime(Instant.ofEpochMilli(bookingRequest.getEndTime()));

        bookingRepository.save(booking);
    }

    public void deleteSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        seatRepository.delete(seat);
    }

    public Seat updateSeat(Long seatId, SeatRequest seatRequest) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        return seatRepository.save(seat);
    }

    public List<Seat> getSeats(Long roomId){
        return seatRepository.findByRoomId(roomId);
    }
}