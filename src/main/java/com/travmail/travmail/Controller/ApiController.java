package com.travmail.travmail.Controller;

import com.travmail.travmail.Entity.TravMail;
import com.travmail.travmail.Entity.User;
import com.travmail.travmail.Repository.UserRepository;
import com.travmail.travmail.Repository.TravMailRepository;
import com.travmail.travmail.Service.EmailService;
import com.travmail.travmail.Tagging.TaggingService;
import com.travmail.travmail.Tagging.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Controller
@RequestMapping("/api/email")
public class ApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravMailRepository travMailRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TaggingService taggingService;

    // create email
    @PostMapping("/create")
    @Transactional
    public String createEmail(@AuthenticationPrincipal OAuth2User principal, @RequestParam String label,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresAt) {
        if (principal == null)
            return "login";
        // validate the expiry date
        if (expiresAt.isBefore(LocalDate.now())) {
            System.err.println("Validation Error: Expiry date cannot be in the past.");
            return "redirect:/manage?error=invalid_date";
        }

        String email = principal.getAttribute("email");

        try {
            User user = userRepository.findByEmail(email).orElseThrow();
            emailService.createTravMail(user, label, expiresAt);

            return "redirect:/manage";

        } catch (IllegalArgumentException e) {
            System.err.println("Validation Error: " + e.getMessage());
            return "redirect:/manage";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/manage?error=api_failure";
        }
    }

    // delete Email
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteEmail(@PathVariable Long id) {
        try {
            emailService.deleteTravMail(id);
            return ResponseEntity.ok().body("Deleted successfully");
            // if ID doesnt exist
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            // Internal server error
            e.printStackTrace();
            return ResponseEntity.status(500).body("Deletion failed due to server error: " + e.getMessage());
        }
    }

    // get signing key from properties
    @Value("${mailgun.webhook.signing-key}")
    private String mailgunSigningKey;

    @Value("${mailgun.api.key}")
    private String mailgunApiKey;

    @Value("${mailgun.domain}")
    private String mailgunDomain;

    // check the signature
    private boolean isValidSignature(String timestamp, String token, String signature) {
        if (timestamp == null || token == null || signature == null)
            return false;

        try {
            String data = timestamp + token;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(mailgunSigningKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            // check if the value matches
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Receive Email
    @PostMapping("/webhook")
    @ResponseBody

    public ResponseEntity<String> receiveWebhook(HttpServletRequest request) {

        String timestamp = request.getParameter("timestamp");
        String token = request.getParameter("token");
        String signature = request.getParameter("signature");

        // error for invalid signature
        if (!isValidSignature(timestamp, token, signature)) {
            System.out.println("Invalid Webhook signature detected.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        // get data
        String recipient = request.getParameter("recipient");
        String sender = request.getParameter("sender");
        String from = request.getParameter("from");
        String subject = request.getParameter("subject");
        String bodyHtml = request.getParameter("body-html");
        String bodyPlain = request.getParameter("body-plain");

        
        System.out.println("Recipient: " + recipient);

        // find user from db and forward to gmail
        TravMail mailInfo = travMailRepository.findByNewAddress(recipient);

        if (mailInfo != null) {

            // ignore when travmail is paused
            if (mailInfo.isPaused()) {
                System.out.println("This TravMail is paused. Ignoring email to: " + recipient);
                return ResponseEntity.ok("Mail is paused");
            }

            String targetEmail = mailInfo.getUser().getEmail();
            System.out.println("User found: " + targetEmail);

            try {
                // creat recipient list
                java.util.List<String> recipientsList = new java.util.ArrayList<>();
                recipientsList.add(targetEmail); // add travmail

                // add companions
                if (mailInfo.getCompanions() != null && !mailInfo.getCompanions().isEmpty()) {
                    mailInfo.getCompanions().stream()
                            .filter(comp -> "ACTIVE".equals(comp.getStatus())) // only to activated companions
                            .forEach(comp -> recipientsList.add(comp.getEmail()));
                }

                String toAddresses = String.join(",", recipientsList);
                System.out.println("Forwarding to: " + toAddresses);
                String displaySender = (from != null && !from.isEmpty()) ? from : sender;

                // add tagging on subject
                Result result = taggingService.classify(subject, bodyPlain, bodyHtml,displaySender);
                String taggedSubject = "[" + result.category().display() + "] "
                        + (subject == null ? "" : subject);
                System.out.println("Tag scores: " + result.scoreMap());

                // call mailgun api
                kong.unirest.MultipartBody unirestRequest = kong.unirest.Unirest
                        .post("https://api.eu.mailgun.net/v3/" + mailgunDomain + "/messages")
                        .basicAuth("api", mailgunApiKey)
                        .field("from", "TravMail <travmail.noreply@" + mailgunDomain + ">") // sender
                        .field("to", toAddresses)
                        .field("h:Reply-To", sender) // reply to sender
                        .field("subject", taggedSubject);

                // set up mail
                if (bodyHtml != null && !bodyHtml.isEmpty()) {
                    String header = "From: " + displaySender + "<br>" + "To: " + recipient + "<br><br>";
                    unirestRequest.field("html", header + bodyHtml);
                } else {
                    unirestRequest.field("text", "From: " + displaySender + "\n\n" + (bodyPlain != null ? bodyPlain : ""));
                }

                // file attachment
                if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
                    org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest = (org.springframework.web.multipart.MultipartHttpServletRequest) request;
                    java.util.Iterator<String> fileNames = multipartRequest.getFileNames();

                    while (fileNames.hasNext()) {
                        org.springframework.web.multipart.MultipartFile file = multipartRequest
                                .getFile(fileNames.next());
                        if (file != null && !file.isEmpty()) {
                            // send file through API
                            unirestRequest.field("attachment", file.getInputStream(), file.getOriginalFilename());
                            System.out.println("File attached via API: " + file.getOriginalFilename());
                        }
                    }
                }

                // send
                kong.unirest.HttpResponse<kong.unirest.JsonNode> response = unirestRequest.asJson();
                System.out.println("Email forwarded successfully via Mailgun API! Status: " + response.getStatus());

            } catch (Exception e) {
                System.out.println("Failed to forward Email");
                e.printStackTrace();
            }
        } else {
            System.out.println("Unable to find user from the DB " + recipient);
        }
        return ResponseEntity.ok("Webhook received successfully");

    }

    // invite companion
    @PostMapping("/companion/invite")
    public ResponseEntity<?> inviteCompanion(
            @RequestParam("travMailId") Long travMailId,
            @RequestParam("email") String email) {
        try {
            emailService.inviteCompanion(travMailId, email);
            return ResponseEntity.ok("Invitation sent successfully!");

        } catch (IllegalStateException e) {
            // error when overlap
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // other errors
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("An error occurred: " + e.getMessage());
        }
    }

    // email verification
    @GetMapping("/companion/verify")
    public String verifyCompanion(@RequestParam("token") String token) {
        try {
            // check token and change status
            emailService.verifyCompanion(token);

            return "<html><body>" +
                    "<h1>Invitation Accepted!</h1>" +
                    "<p>You have been successfully added as a companion.</p>" +
                    "<p>You will now receive forwarded emails from this TravMail.</p>" +
                    "</body></html>";
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return "<html><body><h1> Verification Failed</h1><p>" + e.getMessage() + "</p></body></html>";
        }
    }

    // pause & resume
    @PostMapping("/pause")
    public ResponseEntity<?> togglePause(
            @RequestParam("travMailId") Long travMailId,
            @RequestParam("pause") boolean pause) {
        try {
            emailService.pause(travMailId, pause);
            return ResponseEntity.ok("Status updated successfully.");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to update status: " + e.getMessage());
        }
    }

    // remove companion
    @DeleteMapping("/companion/remove")
    public ResponseEntity<?> removeCompanion(
            @RequestParam("travMailId") Long travMailId,
            @RequestParam("email") String email) {
        try {
            emailService.removeCompanion(travMailId, email);
            return ResponseEntity.ok("Companion removed successfully.");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to remove companion: " + e.getMessage());
        }
    }

}