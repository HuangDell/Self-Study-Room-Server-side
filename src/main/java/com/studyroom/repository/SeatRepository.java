package com.studyroom.repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;
import com.studyroom.model.Room;
import com.studyroom.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    Optional<Seat> findByRoomAndSeatNumber(Room room, String seatNumber);
    List<Seat> findByRoomId(Long roomId);

    @Query("SELECT s FROM Seat s WHERE s.room.name LIKE %:query% OR s.seatNumber LIKE %:query%")
    List<Seat> searchSeats(String query);

}
