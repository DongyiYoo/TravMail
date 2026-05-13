package com.travmail.travmail.Tagging;

public enum Category {
    FLIGHTS("Flights"),
    RAIL("Rail"),
    ACCOMMODATION("Accommodation"),
    GROUND_TRANSPORT("Ground Transport"),
    CAR_RENTAL("Car Rental"),
    DINING("Dining"),
    ACTIVITIES("Activities"),
    GENERAL("General");

    private final String display;

    Category(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}