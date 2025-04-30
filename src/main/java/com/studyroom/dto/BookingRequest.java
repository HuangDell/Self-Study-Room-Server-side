package com.studyroom.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingRequest {
    private String seatId;
    private Long startTime;
    private Long endTime;
}