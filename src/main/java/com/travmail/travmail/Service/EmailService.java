package com.travmail.travmail.Service;

import com.travmail.travmail.Entity.TravMail;
import com.travmail.travmail.Entity.User;
import com.travmail.travmail.Repository.TravMailRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.travmail.travmail.Entity.Companion;
import com.travmail.travmail.Repository.CompanionRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final TravMailRepository travMailRepository;
    private final CompanionRepository companionRepository;

    @Value("${mailgun.domain}")
    private String mailgunDomain;
    @Value("${app.base-url}")
    private String baseUrl;
    @Value("${mailgun.api.key}")
    private String mailgunApiKey;

    @Transactional
    public void createTravMail(User user, String label, LocalDate expiresAt) {

        String sanitizedLabel = label.trim().replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase();

        if (sanitizedLabel.isEmpty()) {
            throw new IllegalArgumentException("TravMail label must contain valid characters");
        }
        // Create new travmail
        String randomId = UUID.randomUUID().toString().substring(0, 5);
        String newAddress = sanitizedLabel + "_" + randomId + "@" + mailgunDomain;

        // save
        TravMail travMail = new TravMail();
        travMail.setLabel(label);
        travMail.setNewAddress(newAddress);
        travMail.setUser(user);
        travMail.setPaused(false);
        travMail.setExpiresAt(expiresAt);

        travMailRepository.save(travMail);
    }

    @Transactional
    public void deleteTravMail(Long id) {
        // search email from DB to delete
        TravMail mail = travMailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid mail Id: " + id));

        // delete from the db
        travMailRepository.delete(mail);
    }

    @Transactional
    public void inviteCompanion(Long travMailId, String companionEmail) {
        // check id to invite
        TravMail travMail = travMailRepository.findById(travMailId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid TravMail ID"));

        // overload check
        if (companionRepository.existsByTravMailAndEmail(travMail, companionEmail)) {
            throw new IllegalStateException("This companion has already been invited.");
        }
        // add companion data
        Companion companion = new Companion();
        companion.setTravMail(travMail);
        companion.setEmail(companionEmail);
        companionRepository.save(companion);

        // generate verification link
        String inviter = travMail.getUser().getEmail();
        String verificationLink = baseUrl + "/api/email/companion/verify?token=" + companion.getVerificationToken();
        System.out.println("Send Email to: [" + companionEmail + "]" + verificationLink);

        try {
            kong.unirest.HttpResponse<kong.unirest.JsonNode> request = kong.unirest.Unirest
                    .post("https://api.eu.mailgun.net/v3/" + mailgunDomain + "/messages")
                    .basicAuth("api", mailgunApiKey)
                    .field("from", "TravMail <travmail.noreply@" + mailgunDomain + ">")
                    .field("to", companionEmail) // companion
                    .field("subject", "[TravMail] You are invited as a companion!")
                    .field("text", "Hello!\n\nYou have been invited to be a companion on TravMail from " + inviter + ".\n"
                            + "Please click the link below to accept the invitation. Once accepted, you will receive booking emails together.\n\n"
                            + "Accept Invitation: " + verificationLink)
                    .asJson();

            System.out.println(" Invitation email sent! Status: " + request.getStatus());

        } catch (Exception e) {
            System.err.println("Failed to send invitation email.");
            e.printStackTrace();
            throw new RuntimeException("Failed to send invitation email", e);
        }
    }

    @Transactional
    public void verifyCompanion(String token) {
        // find companion with token
        Companion companion = companionRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));

        // change status when accepted
        companion.setStatus("ACTIVE");
        companionRepository.save(companion);
    }

    @Transactional
    public void removeCompanion(Long travMailId, String email) {
        // find Travmail
        TravMail travMail = travMailRepository.findById(travMailId)
                .orElseThrow(() -> new IllegalArgumentException("TravMail not found."));

        // get certain companions
        Companion companion = companionRepository.findByTravMailAndEmail(travMail, email)
                .orElseThrow(() -> new IllegalArgumentException("Companion not found."));

        travMail.getCompanions().remove(companion);
        companionRepository.delete(companion);
    }

    @Transactional
    public void pause(Long travMailId, boolean pause) {
        TravMail travMail = travMailRepository.findById(travMailId)
                .orElseThrow(() -> new IllegalArgumentException("TravMail not found."));
        travMail.setPaused(pause);
        travMailRepository.save(travMail);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void autoPauseExpiredMails() {
        LocalDate today = LocalDate.now();

        // get emails when still active
        List<TravMail> expiredMails = travMailRepository.findByPausedFalseAndExpiresAtBefore(today);

        for (TravMail mail : expiredMails) {
            mail.setPaused(true); // turn off the switch
            System.out.println("Paused expired TravMail: " + mail.getNewAddress());
        }

        travMailRepository.saveAll(expiredMails);
    }
}