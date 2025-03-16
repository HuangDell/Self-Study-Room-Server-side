package com.studyroom.dto;

import lombok.Data;

@Data
public class RoomRequest {
    private String name;
    private String location;
    private String openTime;
    private String closeTime;
    private String status;
}