package com.studyroom.repository;

import com.studyroom.model.Booking;
import com.studyroom.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudentOrderByStartTimeDesc(Student student);

    Optional<Booking> findByIdAndStudent(Long id, Student student);
}