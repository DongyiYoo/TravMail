package com.travmail.travmail.Entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class Companion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // add multiple companions in one travmail
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travmail_id", nullable = false)
    private TravMail travMail;

    @Column(nullable = false)
    private String email;

    // companion status
    @Column(nullable = false)
    private String status = "PENDING";

    // token for email verification for checking who it is when clicking link
    @Column(nullable = false, unique = true)
    private String verificationToken;

    // generate a unique token when an object is created automatically
    public Companion() {
        this.verificationToken = UUID.randomUUID().toString();
    }

    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TravMail getTravMail() {
        return travMail;
    }

    public void setTravMail(TravMail travMail) {
        this.travMail = travMail;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }
}