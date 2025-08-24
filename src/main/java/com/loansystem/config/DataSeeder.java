package com.loansystem.config;

import com.loansystem.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
  private final BlacklistService blacklistService;

  @Override
  public void run(String... args) {
    blacklistService.ensureSample();
  }
}
