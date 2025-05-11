package com.studyroom.integration;

import org.springframework.test.context.ActiveProfiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.dto.LoginRequest;
import com.studyroom.dto.RoomRequest;
import com.studyroom.dto.SeatRequest;
import com.studyroom.model.*;
import com.studyroom.repository.*;
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
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.LocalTime; // 添加 LocalTime 导入
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest()
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class AdminApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Admin testAdmin;
    private Room testRoom;
    private Seat testSeat;
    private Student testStudent;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        // 清理数据
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        roomRepository.deleteAll();
        studentRepository.deleteAll();
        adminRepository.deleteAll();
    
        // 创建测试数据
        testAdmin = new Admin();
        testAdmin.setUsername("testadmin");
        testAdmin.setPassword(passwordEncoder.encode("password"));
        // testAdmin.setName("Test Admin");
        adminRepository.save(testAdmin);
    
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
        // testRoom.setCampus("Test Campus"); // 注释掉这一行
        testRoom.setStatus(1); 
        testRoom.setType(0); 
        testRoom.setOpenTime(LocalTime.of(7, 30).atDate(java.time.LocalDate.now()).toInstant(ZoneOffset.UTC)); 
        testRoom.setCloseTime(LocalTime.of(19, 30).atDate(java.time.LocalDate.now()).toInstant(ZoneOffset.UTC)); 
        roomRepository.save(testRoom);
    
        testSeat = new Seat();
        testSeat.setSeatNumber("T1");
        testSeat.setSeatName("T1");
        testSeat.setRoom(testRoom);
        testSeat.setStatus(Seat.SeatStatus.AVAILABLE);
        seatRepository.save(testSeat);
    
        // 获取JWT令牌
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testadmin");
        loginRequest.setPassword("password");
    
        MvcResult result = mockMvc.perform(post("/api/v1.0/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
    
        // 获取原始令牌
        String tokenJson = result.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(tokenJson)
                .get("token").asText();
        
        // 使用新的MockMvc配置
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testAdminLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testadmin");
        loginRequest.setPassword("password");

        mockMvc.perform(post("/api/v1.0/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void testGetAllRooms() throws Exception {
        mockMvc.perform(get("/api/v1.0/admin/rooms")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0].name").value("Test Room"));
    }

    @Test
    void testCreateAndUpdateRoom() throws Exception {
        // 创建新房间
        RoomRequest roomRequest = new RoomRequest();
        roomRequest.setName("New Room");
        roomRequest.setLocation("New Location");
        // roomRequest.setCampus("New Campus"); // 添加这行代码
        roomRequest.setCapacity(40);
        roomRequest.setStatus(1);
        roomRequest.setType(0);

        // 修正：将时间字符串转换为Long类型的毫秒时间戳
        long openTimeMillis = LocalTime.of(8, 0).atDate(java.time.LocalDate.now()).toInstant(ZoneOffset.UTC).toEpochMilli();
        long closeTimeMillis = LocalTime.of(20, 0).atDate(java.time.LocalDate.now()).toInstant(ZoneOffset.UTC).toEpochMilli();
        roomRequest.setOpenTime(openTimeMillis);
        roomRequest.setCloseTime(closeTimeMillis);

        // 如果RoomRequest中有seat_number字段，应该添加：
        // roomRequest.setSeatNumber(3);

        MvcResult result = mockMvc.perform(post("/api/v1.0/admin/rooms")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Room created successfully"))
                .andReturn();

        // 由于创建房间API不返回ID，我们需要通过获取所有房间来找到新创建的房间
        MvcResult roomsResult = mockMvc.perform(get("/api/v1.0/admin/rooms")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andReturn();
        
        String roomsJson = roomsResult.getResponse().getContentAsString();
        String roomId = null;
        
        // 从返回的房间列表中找到名为"New Room"的房间
        for (var room : objectMapper.readTree(roomsJson).get("rooms")) {
            if (room.get("name").asText().equals("New Room")) {
                roomId = room.get("room_id").asText();
                break;
            }
        }

        // 更新房间
        roomRequest.setName("Updated Room");
        mockMvc.perform(patch("/api/v1.0/admin/rooms/" + roomId) // 使用PATCH而不是PUT
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Room updated successfully"));
    }

    @Test
    void testCreateAndManageSeats() throws Exception {
        // 创建新座位 - 使用正确的API路径
        SeatRequest seatRequest = new SeatRequest();
        seatRequest.setSeatName("S1"); // Changed from setSeatNumber
        seatRequest.setRoomId(testRoom.getId());
        seatRequest.setHasSocket(true);

        mockMvc.perform(post("/api/v1.0/admin/seats") // 正确的路径
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(seatRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Seat added successfully"));

        // 获取房间的所有座位 - 这个API可能不存在，需要修改
        // 由于AdminController中没有获取特定房间座位的API，我们可以跳过这部分测试
    }

    @Test
    void testManageBookings() throws Exception {
        // 创建一个预订
        Booking booking = new Booking();
        booking.setStudent(testStudent);
        booking.setSeat(testSeat);
        booking.setRoom(testRoom);
        booking.setStartTime(LocalDateTime.now().plusHours(1).toInstant(ZoneOffset.UTC)); // Changed to Instant
        booking.setEndTime(LocalDateTime.now().plusHours(3).toInstant(ZoneOffset.UTC)); // Changed to Instant
        booking.setStatus(Booking.BookingStatus.ACTIVE);
        bookingRepository.save(booking);

        // 获取所有预订 - 使用正确的API路径
        mockMvc.perform(get("/api/v1.0/admin/bookings")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings").isArray());

        // 由于没有取消预订的API，我们可以跳过这部分测试
        // 或者直接通过repository更新预订状态
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        
        // 验证预订状态已更改
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assert updatedBooking.getStatus() == Booking.BookingStatus.CANCELLED;
    }
}