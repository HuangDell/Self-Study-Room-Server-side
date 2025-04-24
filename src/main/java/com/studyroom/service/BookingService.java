package com.studyroom.service;

import com.studyroom.model.Booking;
import com.studyroom.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        return bookingRepository.findTodayBookingsBySeatId(seatId, startOfDay, endOfDay);
    }

    public List<Booking> getAllBookingsByStudentId(Long studentId) {
        return bookingRepository.findByStudentIdOrderByStartTimeDesc(studentId);
    }
}