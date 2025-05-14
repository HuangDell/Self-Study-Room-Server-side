package com.studyroom.controller;

import com.studyroom.dto.BookingRequest;
import com.studyroom.dto.LoginRequest;
import com.studyroom.dto.LoginResponse;
import com.studyroom.dto.ApiResponse;
import com.studyroom.model.*;
import com.studyroom.service.*;
import com.studyroom.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.springframework.web.servlet.resource.NoResourceFoundException; // 添加导入

@ExtendWith(MockitoExtension.class)
public class StudentControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StudentService studentService;

    @Mock
    private RoomService roomService;

    @Mock
    private SeatService seatService;

    @Mock
    private BookingService bookingService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private StudentController studentController;

    private Student testStudent;
    private Room testRoom;
    private Seat testSeat;
    private Booking testBooking;
    private LoginRequest loginRequest;
    private BookingRequest bookingRequest;

    @BeforeEach
    void setUp() {
        // 登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("student");
        loginRequest.setPassword("password");

        // 模拟 Authentication 放入 SecurityContext
        lenient().when(authentication.getName()).thenReturn("student"); // 使用 lenient()
        lenient().when(authentication.isAuthenticated()).thenReturn(true); // 使用 lenient()
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 测试学生
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setUsername("student");
        lenient().when(studentService.findByUsername("student")).thenReturn(testStudent); // 使用 lenient()

        // 房间、座位、预订
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setName("Test Room");
        testRoom.setCapacity(20);

        testSeat = new Seat();
        testSeat.setId(1L);
        testSeat.setSeatNumber("A1");
        testSeat.setRoom(testRoom);
        testSeat.setStatus(Seat.SeatStatus.AVAILABLE);

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setStudent(testStudent);
        testBooking.setSeat(testSeat);
        testBooking.setRoom(testRoom);
        testBooking.setStartTime(LocalDateTime.now().plusHours(1).toInstant(ZoneOffset.UTC));
        testBooking.setEndTime(LocalDateTime.now().plusHours(3).toInstant(ZoneOffset.UTC));
        testBooking.setStatus(1); // 修改处：使用整数状态替代 Booking.BookingStatus.ACTIVE

        // 预订请求
        bookingRequest = new BookingRequest();
        bookingRequest.setSeatId(1L);
        bookingRequest.setStartTime(LocalDateTime.now().plusHours(1).toEpochSecond(ZoneOffset.UTC));
        bookingRequest.setEndTime(LocalDateTime.now().plusHours(3).toEpochSecond(ZoneOffset.UTC));
    }

    @Test
    void login_ShouldReturnToken() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken("student")).thenReturn("test-jwt-token");

        ResponseEntity<?> response = studentController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map); // 修改处
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody(); // 修改处
        assertEquals("test-jwt-token", responseBody.get("token")); // 修改处
    }

    @Test
    void leaveSeat_ShouldReturnSuccess() {
        doNothing().when(seatService).temporaryLeaveSeat(testStudent, 1L); // 修改处：roomService -> seatService

        ResponseEntity<?> response = studentController.leaveSeat(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map); 
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody(); 
        assertEquals("Seat set to leave status", responseBody.get("message")); 
        verify(seatService).temporaryLeaveSeat(testStudent, 1L); // 修改处：roomService -> seatService
    }

    @Test
    void releaseSeat_ShouldReturnSuccess() {
        doNothing().when(seatService).releaseSeat(testStudent, 1L); // 修改处：roomService -> seatService

        ResponseEntity<?> response = studentController.releaseSeat(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map); 
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody(); 
        assertEquals("Seat released successfully", responseBody.get("message")); 
        verify(seatService).releaseSeat(testStudent, 1L); // 修改处：roomService -> seatService
    }

    @Test
    void getBookingHistory_ShouldReturnHistory() {
        when(bookingService.getAllBookingsByStudentId(1L))
                .thenReturn(Collections.singletonList(testBooking));

        ResponseEntity<?> response = studentController.getBookingHistory();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        List<?> history = (List<?>) ((Map<?, ?>) response.getBody()).get("history");
        assertEquals(1, history.size());
    }

    @Test
    void checkInSeat_ShouldReturnSuccess() {
        doNothing().when(seatService).checkInSeat(testStudent, 1L); // 修改处：roomService -> seatService

        ResponseEntity<?> response = studentController.checkInSeat(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map); 
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody(); 
        assertEquals("Checked in successfully", responseBody.get("message")); 
        verify(seatService).checkInSeat(testStudent, 1L); // 修改处：roomService -> seatService
    }

    @Test
    void getRooms_ShouldReturnRooms() {
        when(roomService.getAllRooms()).thenReturn(Collections.singletonList(testRoom));
        when(seatService.getSeats(1L)).thenReturn(Collections.singletonList(testSeat));

        ResponseEntity<?> response = studentController.getAllRooms(); // 修改此行

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        List<?> rooms = (List<?>) ((Map<?, ?>) response.getBody()).get("rooms");
        assertEquals(1, rooms.size());
    }

    @Test
    void bookRoom_ShouldReturnSuccess() throws NoResourceFoundException { // 修改此行，添加 throws NoResourceFoundException
        doNothing().when(seatService).bookSeat(testStudent, bookingRequest);

        ResponseEntity<?> response = studentController.bookSeat(bookingRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map); // 修改处
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody(); // 修改处
        assertEquals("Seat booked successfully", responseBody.get("message")); // 修改处
        verify(seatService).bookSeat(testStudent, bookingRequest);
    }

    @Test
    void cancelBooking_ShouldReturnSuccess() {
        doNothing().when(seatService).cancelBooking(testStudent, 1L); // 修改处：roomService -> seatService

        ResponseEntity<?> response = studentController.cancelBooking(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map); 
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody(); 
        assertEquals("Booking cancelled successfully", responseBody.get("message")); 
        verify(seatService).cancelBooking(testStudent, 1L); // 修改处：roomService -> seatService
    }

    @Test
    void cancelBooking_NotFound_ShouldReturn404() {
        doThrow(new RuntimeException("Booking not found"))
                .when(seatService).cancelBooking(testStudent, 2L); // 修改处：roomService -> seatService

        ResponseEntity<?> response = studentController.cancelBooking(2L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        assertEquals("Booking not found", ((Map<?, ?>) response.getBody()).get("error"));
    }
}
