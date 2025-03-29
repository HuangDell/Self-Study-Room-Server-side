package com.studyroom.controller;

import com.studyroom.util.JwtUtil;
import com.studyroom.dto.*;
import com.studyroom.model.Booking;
import com.studyroom.model.Room;
import com.studyroom.service.AdminService;
import com.studyroom.service.BookingService;
import com.studyroom.service.RoomService;
import com.studyroom.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1.0/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final RoomService roomService;
    private final SeatService seatService;
    private final BookingService bookingService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            if (authentication.isAuthenticated()) {
                String token = jwtUtil.generateToken(loginRequest.getUsername());
                return ResponseEntity.ok(new LoginResponse(token));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(@RequestBody RoomRequest roomRequest) {
        try {
            roomService.createRoom(roomRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("Room created successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Room already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long roomId) {
        try {
            roomService.deleteRoom(roomId);
            return ResponseEntity.ok(new ApiResponse("Room deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<?> getAllRooms() {
        List<Room> rooms = roomService.getAllRooms();
        List<Map<String, Object>> roomsResponse = rooms.stream()
                .map(room -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("room_id", room.getId().toString());
                    map.put("name", room.getName());
                    map.put("location", room.getLocation());
                    map.put("campus", room.getCampus());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(Map.of("rooms", roomsResponse));
    }

    @PatchMapping("/rooms/{roomId}")
    public ResponseEntity<?> updateRoom(@PathVariable Long roomId, @RequestBody RoomRequest roomRequest) {
        try {
            roomService.updateRoom(roomId, roomRequest);
            return ResponseEntity.ok(new ApiResponse("Room updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/seats")
    public ResponseEntity<?> addSeat(@RequestBody SeatRequest seatRequest) {
        try {
            seatService.addSeat(seatRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("Seat added successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/seats/{seatId}")
    public ResponseEntity<?> deleteSeat(@PathVariable Long seatId) {
        try {
            seatService.deleteSeat(seatId);
            return ResponseEntity.ok(new ApiResponse("Seat deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/seats/{seatId}")
    public ResponseEntity<?> updateSeat(@PathVariable Long seatId, @RequestBody SeatRequest seatRequest) {
        try {
            seatService.updateSeat(seatId, seatRequest);
            return ResponseEntity.ok(new ApiResponse("Seat updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        List<Map<String, Object>> bookingsResponse = bookings.stream()
                .map(booking -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("room_id", booking.getRoom().getId().toString());
                    map.put("seat_id", booking.getSeat().getSeatNumber());
                    map.put("user_id", booking.getStudent().getStudentId());
                    map.put("start_time", booking.getStartTime());
                    map.put("end_time", booking.getEndTime());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(Map.of("bookings", bookingsResponse));
    }
}