package com.studyroom.service;

import com.studyroom.dto.RoomRequest;
import com.studyroom.model.Room;
import com.studyroom.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public Room createRoom(RoomRequest roomRequest) {
        // 检查自习室是否已存在
        if (roomRepository.findByNameAndLocation(roomRequest.getName(), roomRequest.getLocation()).isPresent()) {
            throw new RuntimeException("Room already exists");
        }

        Room room = new Room();
        room.setName(roomRequest.getName());
        room.setLocation(roomRequest.getLocation());

        return roomRepository.save(room);
    }

    public void deleteRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        roomRepository.delete(room);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room updateRoom(Long roomId, RoomRequest roomRequest) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (roomRequest.getOpenTime() != null) {
            room.setOpenTime(roomRequest.getOpenTime());
        }

        if (roomRequest.getCloseTime() != null) {
            room.setCloseTime(roomRequest.getCloseTime());
        }

        if (roomRequest.getStatus() != null) {
            room.setStatus(Room.RoomStatus.valueOf(roomRequest.getStatus().toUpperCase()));
        }

        return roomRepository.save(room);
    }
}