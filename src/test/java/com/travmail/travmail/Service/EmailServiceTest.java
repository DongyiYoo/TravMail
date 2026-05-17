package com.travmail.travmail.Service;

import com.travmail.travmail.Entity.Companion;
import com.travmail.travmail.Entity.TravMail;
import com.travmail.travmail.Entity.User;
import com.travmail.travmail.Repository.CompanionRepository;
import com.travmail.travmail.Repository.TravMailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private TravMailRepository travMailRepository;
    @Mock
    private CompanionRepository companionRepository;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "mailgunDomain", "test.mailgun.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:0000");
        ReflectionTestUtils.setField(emailService, "mailgunApiKey", "api-key");
    }

    @Test
    @DisplayName("Create Email - Success")
    public void testCreateTravMail_Success() {

        User user = new User();
        emailService.createTravMail(user, "Seoul", LocalDate.now().plusDays(5));

        // travMailRepository.save(travMail); is called
        verify(travMailRepository, times(1)).save(any(TravMail.class));
    }

    @Test
    @DisplayName("Create Email - Fail for label not containing valid characters(empty)")
    public void testCreateTravMail_EmptyLabel() {

        User user = new User();

        // exception is thrown when label is empty
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            emailService.createTravMail(user, "   ", LocalDate.now());
        });
        assertEquals("TravMail label must contain valid characters", exception.getMessage());
    }

    @Test
    @DisplayName("Delete Email - Success")
    public void testDeleteTravMail_Success() {
        TravMail mail = new TravMail();
        when(travMailRepository.findById(1L)).thenReturn(Optional.of(mail));
        emailService.deleteTravMail(1L);

        // delete method is called
        verify(travMailRepository, times(1)).delete(mail);
    }

    @Test
    @DisplayName("Delete Email - Fail for invalid id")
    public void testDeleteTravMail_InvalidId() {
        Long invalidId = 1283L;
        when(travMailRepository.findById(invalidId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            emailService.deleteTravMail(invalidId);
        });

        // check the error msg
        assertEquals("Invalid mail Id: " + invalidId, exception.getMessage());

        // repository.delete() should not be called
        verify(travMailRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Invite Companion - Success")
    public void testInviteCompanion_Success() {
        // valid travmail and user
        TravMail mail = new TravMail();
        User user = new User();
        user.setName("Sarah Murphy");
        mail.setUser(user);

        when(travMailRepository.findById(1L)).thenReturn(Optional.of(mail));

        // overload check
        when(companionRepository.existsByTravMailAndEmail(mail, "companion@test.com")).thenReturn(false);

        // invite
        assertDoesNotThrow(() -> {
            emailService.inviteCompanion(1L, "companion@test.com");
        });

        // save
        verify(companionRepository, times(1)).save(any(Companion.class));
    }

    @Test
    @DisplayName("Invite Companion - no duplicate invitation")
    public void testInviteCompanion_Duplicate() {

        TravMail mail = new TravMail();
        when(travMailRepository.findById(1L)).thenReturn(Optional.of(mail));
        when(companionRepository.existsByTravMailAndEmail(mail, "test@test.com")).thenReturn(true);

        // expect IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            emailService.inviteCompanion(1L, "test@test.com");
        });
        assertEquals("This companion has already been invited.", exception.getMessage());
    }

    @Test
    @DisplayName("Invite Companion - fail for invalid travMail id")
    public void testInviteCompanion_InvalidTravMailId() {
        Long invalidId = 1283L;
        when(travMailRepository.findById(invalidId)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            emailService.inviteCompanion(invalidId, "test@test.com");
        });

        // check error msg
        assertEquals("Invalid TravMail ID", exception.getMessage());

        // not to call overload check or save
        verify(companionRepository, never()).existsByTravMailAndEmail(any(), any());
        verify(companionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify Companion - change status to ACTIVE when success")
    public void testVerifyCompanion_Success() {

        Companion comp = new Companion();
        comp.setStatus("PENDING");
        when(companionRepository.findByVerificationToken("t123345345123")).thenReturn(Optional.of(comp));

        emailService.verifyCompanion("t123345345123");

        // status is updated to ACTIVE and saved
        assertEquals("ACTIVE", comp.getStatus());
        verify(companionRepository, times(1)).save(comp);
    }

    @Test
    @DisplayName("Remove Companion - Success")
    public void testRemoveCompanion_Success() {

        TravMail mail = new TravMail();
        mail.setCompanions(new ArrayList<>());
        Companion comp = new Companion();

        when(travMailRepository.findById(1L)).thenReturn(Optional.of(mail));
        when(companionRepository.findByTravMailAndEmail(mail, "companion@test.com")).thenReturn(Optional.of(comp));

        emailService.removeCompanion(1L, "companion@test.com");

        // companion is deleted from the DB
        verify(companionRepository, times(1)).delete(comp);
    }

    @Test
    @DisplayName("Pause - Success")
    public void testPause_Success() {
        TravMail mail = new TravMail();
        mail.setPaused(false);
        when(travMailRepository.findById(1L)).thenReturn(Optional.of(mail));

        emailService.pause(1L, true);

        // pause status is toggled to true
        assertTrue(mail.isPaused());
        verify(travMailRepository, times(1)).save(mail);
    }

    @Test
    @DisplayName("Auto Pause Expired Mails - Scheduler test")
    public void testAutoPauseExpiredMails() {

        TravMail expiredMail = new TravMail();
        expiredMail.setPaused(false);

        // mock DB returns expired mail
        when(travMailRepository.findByPausedFalseAndExpiresAtBefore(any(LocalDate.class)))
                .thenReturn(List.of(expiredMail));

        // execute the scheduler method
        emailService.autoPauseExpiredMails();

        // status changed to true (paused) and saved to DB
        assertTrue(expiredMail.isPaused());
        verify(travMailRepository, times(1)).saveAll(anyList());
    }
}