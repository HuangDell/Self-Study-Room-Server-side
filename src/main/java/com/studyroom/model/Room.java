package com.studyroom.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int type;

    private int capacity;

    private String description;

    private Instant openTime ;

    private Instant closeTime ;

    private String location;

    // 0 for Available, 1 for unavailable
    private Integer status;



}