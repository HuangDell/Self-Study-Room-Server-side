package com.studyroom.service;

import com.studyroom.model.*;
import com.studyroom.repository.BookingRepository;
import com.studyroom.repository.SeatRepository;
import com.studyroom.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.time.Instant; // Added import
import java.time.LocalDate; // Added import
import java.time.LocalTime; // Added import
import java.time.ZoneId;    // Added import

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong; // Added import for anyLong()
import static org.mockito.ArgumentMatchers.eq; // Added import for eq()
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatRepository seatRepository; // This mock is present but not used in current tests for BookingService

    @Mock
    private StudentRepository studentRepository; // This mock is present but not used in current tests for BookingService

    @InjectMocks
    private BookingService bookingService;

    private Student testStudent;
    private Seat testSeat;
    private Booking testBooking;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setUsername("testuser");

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setName("Test Room");

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
        testBooking.setStatus(1); // Changed from Booking.BookingStatus.ACTIVE to 1 (有预定未签到)
    }

    @Test
    void getAllBookings_ShouldReturnAllBookings() {
        // 模拟返回预订列表
        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(testBooking));

        // 执行测试
        List<Booking> result = bookingService.getAllBookings();

        // 验证结果
        assertEquals(1, result.size());
        assertEquals(testBooking.getId(), result.get(0).getId());
        verify(bookingRepository, times(1)).findAll();
    }

    @Test
    void getAllBookingsBySeat_ShouldReturnTodayBookingsForSeat() {
        Long seatId = testSeat.getId();
        Instant dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();

        when(bookingRepository.findTodayBookingsBySeatId(eq(seatId), any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.singletonList(testBooking));

        List<Booking> result = bookingService.getAllBookingsBySeat(seatId);

        assertEquals(1, result.size());
        assertEquals(testBooking.getId(), result.get(0).getId());
        // We use any(Instant.class) because the exact Instant created in the service might differ by milliseconds
        verify(bookingRepository, times(1)).findTodayBookingsBySeatId(eq(seatId), any(Instant.class), any(Instant.class));
    }

    @Test
    void getAllBookingsByStudentId_ShouldReturnBookingsForStudent() {
        Long studentId = testStudent.getId();
        when(bookingRepository.findByStudentIdOrderByStartTimeDesc(studentId))
                .thenReturn(Collections.singletonList(testBooking));

        List<Booking> result = bookingService.getAllBookingsByStudentId(studentId);

        assertEquals(1, result.size());
        assertEquals(testBooking.getId(), result.get(0).getId());
        verify(bookingRepository, times(1)).findByStudentIdOrderByStartTimeDesc(studentId);
    }
}