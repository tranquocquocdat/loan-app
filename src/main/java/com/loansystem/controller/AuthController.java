package com.loansystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
  // render view login.html cho GET /login
  @GetMapping("/login")
  public String login() { return "login"; }
}
