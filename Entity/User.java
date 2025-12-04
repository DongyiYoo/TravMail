package com.travmail.travmail.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "user")
public class User {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ID") 
    private int id;

    @Column(name="EMAIL",unique = true) 
    private String email;

    @Column(name="NAME") 
    private String name;

    @Lob
    @Column(name="ACCESS_TOKEN",columnDefinition = "TEXT") 
    private String accessToken;
}
