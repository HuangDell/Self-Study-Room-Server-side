package com.studyroom.dto;

import lombok.Data;

@Data
public class SeatRequest {
    private Long roomId;
    private String seatNumber;
    private Boolean hasSocket;
    private String status;
    private Integer maxBookingTime;
}