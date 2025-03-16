package com.studyroom.repository;

import com.studyroom.model.Room;
import com.studyroom.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    Optional<Seat> findByRoomAndSeatNumber(Room room, String seatNumber);
}