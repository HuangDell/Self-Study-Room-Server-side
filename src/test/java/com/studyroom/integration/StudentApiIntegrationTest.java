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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class StudentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

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
        testRoom.setLocation("Test Location");
        roomRepository.save(testRoom);
    
        testSeat = new Seat();
        testSeat.setSeatNumber("T1");
        testSeat.setSeatName("T1");
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
    
        // 获取令牌
        String tokenJson = result.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(tokenJson)
                .get("token").asText();
    
        // 不要重新配置 MockMvc，使用自动注入的实例
        // 删除以下代码行：
        // mockMvc = MockMvcBuilders.webAppContextSetup(context)
        //         .apply(springSecurity())
        //         .build();
    }

    @Test
    void testGetRooms() throws Exception {
        mockMvc.perform(get("/api/v1.0/student/rooms")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0].name").value("Test Room"));
    }

    @Test
    void testBookAndCancelRoom() throws Exception {
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setSeatId(testSeat.getId());
        bookingRequest.setStartTime(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli());
        bookingRequest.setEndTime(Instant.now().plus(3, ChronoUnit.HOURS).toEpochMilli());
    
        mockMvc.perform(post("/api/v1.0/student/seats/book")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchSeats() throws Exception {
        // 首先获取自习室列表
        mockMvc.perform(get("/api/v1.0/student/rooms")
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms").isArray());
        
        // 然后获取特定自习室的座位
        mockMvc.perform(get("/api/v1.0/student/rooms/{roomId}/seats", testRoom.getId())
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats").isArray())
                .andExpect(jsonPath("$.seats[0].seat_id").value(testSeat.getId().toString()));
    }
}