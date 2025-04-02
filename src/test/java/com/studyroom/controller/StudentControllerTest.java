package com.studyroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.dto.BookingRequest;
import com.studyroom.dto.LoginRequest;
import com.studyroom.model.*;
import com.studyroom.service.CompositeUserDetailsService;
import com.studyroom.util.JwtUtil;
import com.studyroom.service.RoomService;
import com.studyroom.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;  // Add this import
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
public class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CompositeUserDetailsService compositeUserDetailsService;

    @MockBean
    private StudentService studentService;

    @MockBean
    private RoomService roomService;

    private Student testStudent;
    private Room testRoom;
    private Seat testSeat;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setUsername("student");
        testStudent.setPassword("password");

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
        testBooking.setStartTime(LocalDateTime.now().plusHours(1));
        testBooking.setEndTime(LocalDateTime.now().plusHours(3));
        testBooking.setStatus(Booking.BookingStatus.ACTIVE);

        // 改进认证模拟
        UserDetails userDetails = User.withUsername("student")
                .password("password")
                .roles("STUDENT")  // 使用roles而不是authorities
                .build();
        
        
        // 配置UserDetailsService模拟
        when(compositeUserDetailsService.loadUserByUsername("student")).thenReturn(userDetails);
        when(compositeUserDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        
        // 配置JWT验证
        when(jwtUtil.validateToken(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.extractUsername(anyString())).thenReturn("student");
        when(jwtUtil.generateToken(anyString())).thenReturn("test-jwt-token");

        
        // 设置安全上下文
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // 配置学生服务
        when(studentService.findByUsername(anyString())).thenReturn(testStudent);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    }

    @Test
    void login_ShouldReturnToken() throws Exception {
        // 设置模拟行为
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("student");
        loginRequest.setPassword("password");

        // 创建更完整的认证对象
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                "student", "password", Collections.singletonList(() -> "ROLE_STUDENT"));
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(jwtUtil.generateToken(anyString())).thenReturn("test-jwt-token");
        
        // 设置安全上下文
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        // 添加请求头中的Authorization
        System.out.println(objectMapper.writeValueAsString(loginRequest));
        mockMvc.perform(post("/api/v1.0/student/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        // .header("Authorization", "Bearer test-jwt-token")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"));
    }

    @Test
    void leaveSeat_ShouldReturnSuccess() throws Exception {
        // 执行测试
        mockMvc.perform(post("/api/v1.0/student/seats/1/leave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seat set to leave status"));

        // 验证服务调用
        Mockito.verify(roomService).temporaryLeaveSeat(testStudent, 1L);
    }

    @Test
    void releaseSeat_ShouldReturnSuccess() throws Exception {
        // 执行测试
        mockMvc.perform(post("/api/v1.0/student/seats/1/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seat released successfully"));

        // 验证服务调用
        Mockito.verify(roomService).releaseSeat(testStudent, 1L);
    }

    @Test
    void getBookingHistory_ShouldReturnHistory() throws Exception {
        // 设置模拟行为
        when(roomService.getBookingHistory(testStudent)).thenReturn(Collections.singletonList(testBooking));

        // 执行测试
        mockMvc.perform(get("/api/v1.0/student/bookings/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[0].booking_id").value("1"))
                .andExpect(jsonPath("$.history[0].room_id").value("1"))
                .andExpect(jsonPath("$.history[0].seat_id").value("A1"));
    }

    @Test
    void searchSeats_ShouldReturnSeats() throws Exception {
        // 设置模拟行为
        when(roomService.searchSeats("test")).thenReturn(Collections.singletonList(testSeat));

        // 执行测试
        mockMvc.perform(get("/api/v1.0/student/seats?query=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[0].seat_id").value("1"))
                .andExpect(jsonPath("$.seats[0].room_id").value("1"))
                .andExpect(jsonPath("$.seats[0].status").value("available"));
    }

    @Test
    void checkInSeat_ShouldReturnSuccess() throws Exception {
        // 执行测试
        mockMvc.perform(post("/api/v1.0/student/seats/1/checkin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Checked in successfully"));

        // 验证服务调用
        Mockito.verify(roomService).checkInSeat(testStudent, 1L);
    }

    @Test
    void getRooms_ShouldReturnRooms() throws Exception {
        // 设置模拟行为
        when(roomService.getRoomsWithAvailableSeats()).thenReturn(
                Collections.singletonMap(testRoom, 10L));

        // 执行测试
        mockMvc.perform(get("/api/v1.0/student/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].room_id").value("1"))
                .andExpect(jsonPath("$.rooms[0].name").value("Test Room"))
                .andExpect(jsonPath("$.rooms[0].available_seats").value(10));
    }

    @Test
    void bookRoom_ShouldReturnSuccess() throws Exception {
        // 设置请求对象
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setSeatId("1");
        bookingRequest.setStartTime(LocalDateTime.now().plusHours(1));
        bookingRequest.setEndTime(LocalDateTime.now().plusHours(3));

        // 执行测试
        mockMvc.perform(post("/api/v1.0/student/rooms/1/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Room booked successfully"));

        // 验证服务调用
        Mockito.verify(roomService).bookSeat(
                testStudent, 1L, 1L,
                bookingRequest.getStartTime(),
                bookingRequest.getEndTime());
    }

    @Test
    void cancelBooking_ShouldReturnSuccess() throws Exception {
        // 执行测试
        mockMvc.perform(delete("/api/v1.0/student/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Booking cancelled successfully"));

        // 验证服务调用
        Mockito.verify(roomService).cancelBooking(testStudent, 1L);
    }

    @Test
    void cancelBooking_NotFound_ShouldReturn404() throws Exception {
        // 设置模拟行为 - 抛出异常
        Mockito.doThrow(new RuntimeException("Booking not found"))
                .when(roomService).cancelBooking(testStudent, 2L);

        // 执行测试
        mockMvc.perform(delete("/api/v1.0/student/bookings/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Booking not found"));
    }
}