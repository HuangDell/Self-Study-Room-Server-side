package com.studyroom.service;

import com.studyroom.dto.SeatRequest;
import com.studyroom.model.Room;
import com.studyroom.model.Seat;
import com.studyroom.repository.RoomRepository;
import com.studyroom.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;

    public Seat addSeat(SeatRequest seatRequest) {
        Room room = roomRepository.findById(seatRequest.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // 检查座位是否已存在
        if (seatRepository.findByRoomAndSeatNumber(room, seatRequest.getSeatNumber()).isPresent()) {
            throw new RuntimeException("Seat already exists in this room");
        }

        Seat seat = new Seat();
        seat.setRoom(room);
        seat.setSeatNumber(seatRequest.getSeatNumber());
        seat.setHasSocket(seatRequest.getHasSocket());

        return seatRepository.save(seat);
    }

    public void deleteSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        seatRepository.delete(seat);
    }

    public Seat updateSeat(Long seatId, SeatRequest seatRequest) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (seatRequest.getStatus() != null) {
            seat.setStatus(Seat.SeatStatus.valueOf(seatRequest.getStatus().toUpperCase()));
        }

        if (seatRequest.getMaxBookingTime() != null) {
            seat.setMaxBookingTime(seatRequest.getMaxBookingTime());
        }

        return seatRepository.save(seat);
    }
}