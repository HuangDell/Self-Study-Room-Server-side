package com.studyroom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.studyroom.model.Seat;
import lombok.Data;

@Data
public class SeatRequest {
    private Long roomId;

    private String seatName;
    private Boolean hasSocket;
    private Seat.SeatStatus status;
}