package com.studyroom.dto;

import lombok.Data;

@Data
public class RoomRequest {
    private String name;
    private Integer type;
    private String location;
    private String openTime;
    private String closeTime;
    private Integer capacity;
    private String status;
//    private String campus;
}