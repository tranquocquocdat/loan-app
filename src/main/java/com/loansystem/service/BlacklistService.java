package com.loansystem.service;

import com.loansystem.entity.BlacklistEntry;
import com.loansystem.entity.BlacklistType;
import com.loansystem.repository.BlacklistEntryRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlacklistService {

  private final BlacklistEntryRepository repo;

  public Optional<String> check(String email, String phone) {
    if (email != null && !email.isBlank()) {
      var e = repo.findFirstByTypeAndValueIgnoreCaseAndActiveTrue(BlacklistType.EMAIL, email);
      if (e.isPresent()) return Optional.of("Email nằm trong blacklist: " + e.get().getReason());
    }
    if (phone != null && !phone.isBlank()) {
      var p = repo.findFirstByTypeAndValueIgnoreCaseAndActiveTrue(BlacklistType.PHONE, phone);
      if (p.isPresent()) return Optional.of("SĐT nằm trong blacklist: " + p.get().getReason());
    }
    return Optional.empty();
  }

  @Transactional
  public void ensureSample() {
    if (!repo.existsByTypeAndValueIgnoreCaseAndActiveTrue(BlacklistType.EMAIL, "fraud@example.com")) {
      repo.save(BlacklistEntry.builder()
          .type(BlacklistType.EMAIL).value("fraud@example.com")
          .reason("Gian lận trước đây").active(true).createdAt(LocalDateTime.now()).build());
    }
    if (!repo.existsByTypeAndValueIgnoreCaseAndActiveTrue(BlacklistType.PHONE, "0900000000")) {
      repo.save(BlacklistEntry.builder()
          .type(BlacklistType.PHONE).value("0900000000")
          .reason("Nợ xấu nhóm 5").active(true).createdAt(LocalDateTime.now()).build());
    }
  }
}
