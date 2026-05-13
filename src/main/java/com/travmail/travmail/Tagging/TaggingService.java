package com.travmail.travmail.Tagging;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class TaggingService {

    private static final Pattern AIRPORT_CODE = Pattern.compile("\\b[A-Z]{3}\\b");
    private static final Pattern FLIGHT_NO = Pattern.compile("\\b[A-Z]{1,3}\\d{2,5}\\b");
    private static final Pattern TIME_24H = Pattern.compile("\\b\\d{1,2}:\\d{2}\\b");
    private static final Pattern MONEY = Pattern.compile("\\b(€|\\$|£)\\s?\\d+[\\d,.]*\\b");

    private static final int THRESHOLD_MIN_SCORE = 8;
    private static final int THRESHOLD_MIN_GAP = 3;

    public Result classify(String subject, String bodyPlain, String bodyHtml) {
        String text = buildText(subject, bodyPlain, bodyHtml);

        EnumMap<Category, Integer> score = new EnumMap<>(Category.class);
        for (Category c : Category.values())
            score.put(c, 0);

        List<String> evidence = new ArrayList<>();

        boolean hasCheckIn = text.contains("check-in") || text.contains("check in");
        boolean hasCheckOut = text.contains("check-out") || text.contains("check out") || text.contains("checkout");

        // -----------------------
        // FLIGHTS
        // -----------------------
        add(score, evidence, Category.FLIGHTS, text, 12, "boarding pass");
        add(score, evidence, Category.FLIGHTS, text, 10, "flight details", "ticket details", "travel itinerary",
                "e-ticket", "eticket");
        add(score, evidence, Category.FLIGHTS, text, 6, "terminal", "gate");
        add(score, evidence, Category.FLIGHTS, text, 4, "flight", "departure", "arrival", "airline", "boarding");

        if (hasCheckIn && containsAny(text, "flight", "boarding", "gate", "terminal", "airline")) {
            bump(score, evidence, Category.FLIGHTS, 8, "combo: check-in + flight-context");
        } else if (hasCheckIn) {
            bump(score, evidence, Category.FLIGHTS, 2, "weak: check-in (ambiguous)");
        }

        // flight number / airport code
        if (FLIGHT_NO.matcher(text.toUpperCase()).find()) {
            bump(score, evidence, Category.FLIGHTS, 8, "pattern: flight number");
        }
        if (AIRPORT_CODE.matcher(text.toUpperCase()).find()) {
            bump(score, evidence, Category.FLIGHTS, 4, "pattern: airport code");
        }
        if (FLIGHT_NO.matcher(text.toUpperCase()).find() && AIRPORT_CODE.matcher(text.toUpperCase()).find()) {
            bump(score, evidence, Category.FLIGHTS, 6, "combo: flight number + airport code");
        }

        // -----------------------
        // RAIL
        // -----------------------
        add(score, evidence, Category.RAIL, text, 12, "train ticket", "rail ticket");
        add(score, evidence, Category.RAIL, text, 8, "platform", "carriage", "coach", "seat", "station");
        add(score, evidence, Category.RAIL, text, 4, "train", "rail", "timetable", "departure station",
                "arrival station");

        if (containsAny(text, "platform") && containsAny(text, "coach", "carriage", "seat", "wagon")) {
            bump(score, evidence, Category.RAIL, 8, "combo: platform + seat/coach");
        }
        if (containsAny(text, "station") && (TIME_24H.matcher(text).find())) {
            bump(score, evidence, Category.RAIL, 6, "combo: station + time");
        }

        // -----------------------
        // ACCOMMODATION
        // -----------------------
        add(score, evidence, Category.ACCOMMODATION, text, 10, "hotel", "resort", "property", "accommodation");
        add(score, evidence, Category.ACCOMMODATION, text, 8, "room", "rooms", "nights", "stay");
        add(score, evidence, Category.ACCOMMODATION, text, 6, "booking id", "reservation number", "cancellation policy",
                "address");

        if (hasCheckIn && hasCheckOut) {
            bump(score, evidence, Category.ACCOMMODATION, 12, "combo: check-in + check-out");
        } else if (hasCheckIn && containsAny(text, "room", "hotel", "property", "stay", "nights")) {
            bump(score, evidence, Category.ACCOMMODATION, 8, "combo: check-in + accommodation-context");
        } else if (hasCheckOut && containsAny(text, "room", "hotel", "property", "stay", "nights")) {
            bump(score, evidence, Category.ACCOMMODATION, 6, "combo: check-out + accommodation-context");
        }

        // -----------------------
        // GROUND TRANSPORT (Taxi)
        // -----------------------
        add(score, evidence, Category.GROUND_TRANSPORT, text, 12, "uber", "bolt", "lyft");
        add(score, evidence, Category.GROUND_TRANSPORT, text, 8, "trip receipt", "thanks for riding", "driver",
                "pickup", "dropoff", "drop off");
        add(score, evidence, Category.GROUND_TRANSPORT, text, 4, "taxi", "ride", "fare", "transfer", "shuttle");

        if (containsAny(text, "pickup") && (containsAny(text, "dropoff", "drop off"))) {
            bump(score, evidence, Category.GROUND_TRANSPORT, 8, "combo: pickup + dropoff");
        }
        if (MONEY.matcher(text).find() && containsAny(text, "trip", "fare", "receipt", "ride")) {
            bump(score, evidence, Category.GROUND_TRANSPORT, 6, "combo: money + trip/fare");
        }

        // -----------------------
        // CAR RENTAL
        // -----------------------
        add(score, evidence, Category.CAR_RENTAL, text, 12, "car rental", "rental agreement");
        add(score, evidence, Category.CAR_RENTAL, text, 8, "vehicle", "license plate");
        add(score, evidence, Category.CAR_RENTAL, text, 6, "pickup", "dropoff", "drop off", "return location",
                "return time");
        add(score, evidence, Category.CAR_RENTAL, text, 4, "rental", "driver");

        if (text.contains("rental") && containsAny(text, "vehicle", "return", "return time", "return location")) {
            bump(score, evidence, Category.CAR_RENTAL, 6, "combo: rental + vehicle/return");
        }

        // -----------------------
        // DINING
        // -----------------------
        add(score, evidence, Category.DINING, text, 12, "reservation confirmation", "your restaurant reservation");
        add(score, evidence, Category.DINING, text, 10, "table for");
        add(score, evidence, Category.DINING, text, 8, "guests:", "party size", "confirmed");
        add(score, evidence, Category.DINING, text, 6, "restaurant");
        add(score, evidence, Category.DINING, text, 3, "menu");

        if (text.contains("reservation")
                && (containsAny(text, "table", "guests", "party") || TIME_24H.matcher(text).find())) {
            bump(score, evidence, Category.DINING, 8, "combo: reservation + table/guests/time");
        }

        // -----------------------
        // ACTIVITIES
        // -----------------------
        add(score, evidence, Category.ACTIVITIES, text, 10, "tour", "guided tour", "excursion");
        add(score, evidence, Category.ACTIVITIES, text, 8, "admission", "attraction", "museum", "theme park");
        add(score, evidence, Category.ACTIVITIES, text, 6, "ticket", "booking for", "entry");
        add(score, evidence, Category.ACTIVITIES, text, 4, "activity", "experience");

        // -----------------------
        // GENERAL
        // -----------------------
        // "confirmation/booking/voucher/receipt"는 다목적이라 General에 약하게만 줌
        add(score, evidence, Category.GENERAL, text, 2, "confirmation", "confirmed", "booking", "voucher", "receipt",
                "itinerary");

        return decide(score, evidence);
    }

    private Result decide(EnumMap<Category, Integer> score, List<String> evidence) {
        Category best = Category.GENERAL;
        int bestScore = Integer.MIN_VALUE;
        int secondScore = Integer.MIN_VALUE;

        for (var e : score.entrySet()) {
            int s = e.getValue();
            if (s > bestScore) {
                secondScore = bestScore;
                bestScore = s;
                best = e.getKey();
            } else if (s > secondScore) {
                secondScore = s;
            }
        }

        if (bestScore < THRESHOLD_MIN_SCORE) {
            best = Category.GENERAL;
        } else if (bestScore - secondScore < THRESHOLD_MIN_GAP) {
            best = Category.GENERAL;
        }

        return new Result(best, bestScore, score, evidence);
    }

    private String buildText(String subject, String bodyPlain, String bodyHtml) {
        String s = normalize(subject);
        String p = normalize(bodyPlain);
        String h = normalize(stripHtml(bodyHtml));

        return (s + "\n" + p + "\n" + h).trim();
    }

    private String normalize(String s) {
        if (s == null)
            return "";
        return s.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private String stripHtml(String html) {
        if (html == null)
            return "";
        return html.replaceAll("<[^>]+>", " ");
    }

    private void add(Map<Category, Integer> score, List<String> evidence, Category c, String text, int weight,
            String... keys) {
        for (String k : keys) {
            if (text.contains(k)) {
                bump(score, evidence, c, weight, "match: [" + k + "] +" + weight);
            }
        }
    }

    private void bump(Map<Category, Integer> score, List<String> evidence, Category c, int weight, String why) {
        score.merge(c, weight, Integer::sum);
        evidence.add(c.display() + " " + why);
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys)
            if (text.contains(k))
                return true;
        return false;
    }
}