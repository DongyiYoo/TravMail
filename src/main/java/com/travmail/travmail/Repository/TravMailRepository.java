package com.travmail.travmail.Repository;

import com.travmail.travmail.Entity.TravMail;
import org.springframework.data.jpa.repository.JpaRepository;
import com.travmail.travmail.Entity.User;
import java.time.LocalDate;
import java.util.List;

public interface TravMailRepository extends JpaRepository<TravMail, Long> {
    TravMail findByNewAddress(String newAddress);

    List<TravMail> findByUser(User user);

    List<TravMail> findByPausedFalseAndExpiresAtBefore(LocalDate date);
}