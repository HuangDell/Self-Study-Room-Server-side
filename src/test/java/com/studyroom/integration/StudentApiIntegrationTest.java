package com.studyroom.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.dto.BookingRequest;
import com.studyroom.dto.LoginRequest;
import com.studyroom.model.*;
import com.studyroom.repository.BookingRepository;
import com.studyroom.repository.RoomRepository;
import com.studyroom.repository.SeatRepository;
import com.studyroom.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class StudentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Student testStudent;
    private Room testRoom;
    private Seat testSeat;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        // 清理数据
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        roomRepository.deleteAll();
        studentRepository.deleteAll();

        // 创建测试数据
        testStudent = new Student();
        testStudent.setUsername("teststudent");
        testStudent.setPassword(passwordEncoder.encode("password"));
        testStudent.setName("Test Student");
        testStudent.setStudentId("ST123456");
        studentRepository.save(testStudent);

        testRoom = new Room();
        testRoom.setName("Test Room");
        testRoom.setCapacity(20);
        testRoom.setDescription("Test Description");
        roomRepository.save(testRoom);

        testSeat = new Seat();
        testSeat.setSeatNumber("T1");
        testSeat.setRoom(testRoom);
        testSeat.setStatus(Seat.SeatStatus.AVAILABLE);
        seatRepository.save(testSeat);

        // 获取JWT令牌
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("teststudent");
        loginRequest.setPassword("password");

        MvcResult result = mockMvc.perform(post("/api/v1.0/student/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        jwtToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void testStudentLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("teststudent");
        loginRequest.setPassword("password");

        mockMvc.perform(post("/api/v1.0/student/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void testGetRooms() throws Exception {
        mockMvc.perform(get("/api/v1.0/student/rooms")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0].name").value("Test Room"));
    }

    @Test
    void testBookAndCancelRoom() throws Exception {
        // 预订座位
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setSeatId(testSeat.getId().toString());
        bookingRequest.setStartTime(LocalDateTime.now().plusHours(1));
        bookingRequest.setEndTime(LocalDateTime.now().plusHours(3));

        MvcResult bookResult = mockMvc.perform(post("/api/v1.0/student/rooms/" + testRoom.getId() + "/book")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Room booked successfully"))
                .andReturn();

        // 获取预订记录
        mockMvc.perform(get("/api/v1.0/student/bookings/history")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history.length()").value(1));

        // 暂时离开座位
        Long bookingId = bookingRepository.findAll().get(0).getId();
        mockMvc.perform(post("/api/v1.0/student/seats/" + testSeat.getId() + "/leave")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seat set to leave status"));

        // 签到座位
        mockMvc.perform(post("/api/v1.0/student/seats/" + testSeat.getId() + "/checkin")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Checked in successfully"));

        // 释放座位
        mockMvc.perform(post("/api/v1.0/student/seats/" + testSeat.getId() + "/release")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seat released successfully"));
    }

    @Test
    void testSearchSeats() throws Exception {
        mockMvc.perform(get("/api/v1.0/student/seats?query=Test")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats").isArray())
                .andExpect(jsonPath("$.seats[0].seat_id").value(testSeat.getId().toString()));
    }
}