package com.studyroom.service;

import com.studyroom.model.Booking;
import com.studyroom.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * 返回当前座位今天的所有预定情况
     * @param seatId
     * @return
     */
    public List<Booking> getAllBookingsBySeat(Long seatId) {
        Instant dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();

        return bookingRepository.findTodayBookingsBySeatId(seatId, dayStart, dayEnd);
    }

    public List<Booking> getAllBookingsByStudentId(Long studentId) {
        return bookingRepository.findByStudentIdOrderByStartTimeDesc(studentId);
    }
}