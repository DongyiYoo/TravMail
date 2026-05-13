package com.travmail.travmail.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "travmail")
public class TravMail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    private String label;
    private String newAddress;
    private String mailgunRouteId;
    private LocalDate expiresAt;

    @Column(nullable = false)
    private boolean paused = false;
    
    @OneToMany(mappedBy = "travMail", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private java.util.List<Companion> companions = new java.util.ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
