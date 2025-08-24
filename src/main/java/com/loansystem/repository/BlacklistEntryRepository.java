package com.loansystem.repository;

import com.loansystem.entity.BlacklistEntry;
import com.loansystem.entity.BlacklistType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistEntryRepository extends JpaRepository<BlacklistEntry, Long> {
  boolean existsByTypeAndValueIgnoreCaseAndActiveTrue(BlacklistType type, String value);
  Optional<BlacklistEntry> findFirstByTypeAndValueIgnoreCaseAndActiveTrue(BlacklistType type, String value);
}
