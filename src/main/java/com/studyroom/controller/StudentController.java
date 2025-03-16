package com.studyroom.controller;

import com.studyroom.dto.BookingRequest;
import com.studyroom.dto.LoginRequest;
import com.studyroom.model.Booking;
import com.studyroom.model.Room;
import com.studyroom.model.Seat;
import com.studyroom.model.Student;
import com.studyroom.util.JwtUtil;
import com.studyroom.service.RoomService;
import com.studyroom.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1.0/student")
@RequiredArgsConstructor
public class StudentController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final StudentService studentService;
    private final RoomService roomService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        String jwt = jwtUtil.generateToken(loginRequest.getUsername());
        Map<String, String> response = new HashMap<>();
        response.put("token", jwt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seats/{seatId}/leave")
    public ResponseEntity<?> leaveSeat(@PathVariable Long seatId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Student student = studentService.findByUsername(authentication.getName());

        roomService.temporaryLeaveSeat(student, seatId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Seat set to leave status");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seats/{seatId}/release")
    public ResponseEntity<?> releaseSeat(@PathVariable Long seatId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Student student = studentService.findByUsername(authentication.getName());

        roomService.releaseSeat(student, seatId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Seat released successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/history")
    public ResponseEntity<?> getBookingHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Student student = studentService.findByUsername(authentication.getName());

        List<Booking> bookings = roomService.getBookingHistory(student);

        List<Map<String, Object>> history = bookings.stream().map(booking -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("booking_id", booking.getId().toString());
            entry.put("room_id", booking.getSeat().getRoom().getId().toString());
            entry.put("seat_id", booking.getSeat().getSeatNumber());
            entry.put("start_time", booking.getStartTime().toString());
            entry.put("end_time", booking.getEndTime().toString());
            return entry;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("history", history);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seats")
    public ResponseEntity<?> searchSeats(@RequestParam String query) {
        List<Seat> seats = roomService.searchSeats(query);

        List<Map<String, String>> seatsList = seats.stream().map(seat -> {
            Map<String, String> seatInfo = new HashMap<>();
            seatInfo.put("seat_id", seat.getId().toString());
            seatInfo.put("room_id", seat.getRoom().getId().toString());
            seatInfo.put("status", seat.getStatus().toString().toLowerCase());
            return seatInfo;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("seats", seatsList);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seats/{seatId}/checkin")
    public ResponseEntity<?> checkInSeat(@PathVariable Long seatId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Student student = studentService.findByUsername(authentication.getName());

        roomService.checkInSeat(student, seatId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Checked in successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms() {
        Map<Room, Long> roomsWithAvailableSeats = roomService.getRoomsWithAvailableSeats();

        List<Map<String, Object>> roomsList = roomsWithAvailableSeats.entrySet().stream().map(entry -> {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("room_id", entry.getKey().getId().toString());
            roomInfo.put("name", entry.getKey().getName());
            roomInfo.put("available_seats", entry.getValue());
            return roomInfo;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("rooms", roomsList);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/{roomId}/book")
    public ResponseEntity<?> bookRoom(
            @PathVariable Long roomId,
            @RequestBody BookingRequest bookingRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Student student = studentService.findByUsername(authentication.getName());

        roomService.bookSeat(
                student,
                roomId,
                Long.parseLong(bookingRequest.getSeatId()),
                bookingRequest.getStartTime(),
                bookingRequest.getEndTime()
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Room booked successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Student student = studentService.findByUsername(authentication.getName());

        try {
            roomService.cancelBooking(student, bookingId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Booking cancelled successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Booking not found");
            return ResponseEntity.status(404).body(response);
        }
    }
}