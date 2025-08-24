package com.loansystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "BLACKLIST")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlacklistEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BlacklistType type;   // EMAIL | PHONE

  @Column(nullable = false, unique = true, length = 190)
  private String value;

  @Column(length = 400)
  private String reason;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
