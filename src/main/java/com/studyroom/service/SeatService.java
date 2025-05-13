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
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    public void bookSeat(Student student, BookingRequest bookingRequest) throws NoResourceFoundException {
        Seat seat = seatRepository.findById(bookingRequest.getSeatId())
                .orElseThrow(() -> new NoResourceFoundException(HttpMethod.POST,"Seat not found"));
        int type = seat.getRoom().getType();
        if(type != 0 && type !=student.getType())
            throw new AccessDeniedException("This room is not open to you");


        if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
            throw new RuntimeException("Seat is not available");
        }

        seat.setStatus(Seat.SeatStatus.OCCUPIED);
        seatRepository.save(seat);

        Booking booking = new Booking();
        booking.setStudent(student);
        booking.setSeat(seat);
        booking.setRoom(seat.getRoom());
        booking.setStartTime(Instant.ofEpochMilli(bookingRequest.getStartTime()));
        booking.setEndTime(Instant.ofEpochMilli(bookingRequest.getEndTime()));
        booking.setStatus(1);

        bookingRepository.save(booking);
    }

    @Transactional
    public void cancelBooking(Student student, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndStudent(bookingId, student)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(0);
        bookingRepository.save(booking);
    }

    @Transactional
    public void temporaryLeaveSeat(Student student, Long seatId) {
        List<Booking> activeBookings = getLatestBookingForSeat(student, seatId);
        if (activeBookings.isEmpty()) {
            throw new RuntimeException("Student has not booked this seat");
        }
        Booking booking = activeBookings.get(0);
        booking.setStatus(3);
        bookingRepository.save(booking);

    }

    @Transactional
    public void checkInSeat(Student student, Long seatId) {
        List<Booking> activeBookings = getLatestBookingForSeat(student, seatId);
        if (activeBookings.isEmpty()) {
            throw new RuntimeException("Student has not booked this seat");
        }
        Booking booking = activeBookings.get(0);
        booking.setStatus(2);
        bookingRepository.save(booking);

    }

    @Transactional
    public void releaseSeat(Student student, Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        List<Booking> activeBookings = getLatestBookingForSeat(student, seatId);
        if (activeBookings.isEmpty()) {
            throw new RuntimeException("Student has not booked this seat");
        }

        Booking booking = activeBookings.get(0);
        booking.setStatus(2);
        bookingRepository.save(booking);

        seat.setStatus(Seat.SeatStatus.AVAILABLE);
        seatRepository.save(seat);
    }

    @Transactional
    public void deleteSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        bookingRepository.deleteBySeatId(seatId);

        seatRepository.delete(seat);
    }

    public Seat updateSeat(Long seatId, SeatRequest seatRequest) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (seatRequest.getSeatName()!=null)
            seat.setSeatName(seatRequest.getSeatName());
        if (seatRequest.getHasSocket()!=null)
            seat.setHasSocket(seatRequest.getHasSocket());
        if (seatRequest.getStatus()!=null)
            seat.setStatus(seatRequest.getStatus());

        return seatRepository.save(seat);
    }

    public List<Seat> getSeats(Long roomId){
        return seatRepository.findByRoomId(roomId);
    }

    private List<Booking> getLatestBookingForSeat(Student student, Long seatId) {
        return bookingRepository.findByStudentOrderByStartTimeDesc(student).stream()
                .filter(booking -> booking.getSeat().getId().equals(seatId))
                .toList();
    }

    private void validateStudentBooking(Student student, Long seatId) {
        List<Booking> activeBookings = getLatestBookingForSeat(student, seatId);
        if (activeBookings.isEmpty()) {
            throw new RuntimeException("Student has not booked this seat");
        }
    }
}