package com.studyroom.repository;

import com.studyroom.model.Booking;
import com.studyroom.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudentOrderByStartTimeDesc(Student student);

    Optional<Booking> findByIdAndStudent(Long id, Student student);

    @Query("SELECT b FROM Booking b WHERE b.seat.id = :seatId AND " +
            "((b.startTime >= :dayStart AND b.startTime < :dayEnd) OR " +
            "(b.endTime > :dayStart AND b.endTime <= :dayEnd) OR " +
            "(b.startTime <= :dayStart AND b.endTime >= :dayEnd))")
    List<Booking> findTodayBookingsBySeatId(
            @Param("seatId") Long seatId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    List<Booking> findByStudentIdOrderByStartTimeDesc(Long id);
}