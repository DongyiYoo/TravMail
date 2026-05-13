package com.travmail.travmail.Repository;

import com.travmail.travmail.Entity.Companion;
import com.travmail.travmail.Entity.TravMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanionRepository extends JpaRepository<Companion, Long> {

    // find added companions
    List<Companion> findByTravMail(TravMail travMail);

    //find certain companions
    Optional<Companion> findByTravMailAndEmail(TravMail travMail, String email);

    // find companion data by verification
    Optional<Companion> findByVerificationToken(String verificationToken);

    // check overlap
    boolean existsByTravMailAndEmail(TravMail travMail, String email);
}