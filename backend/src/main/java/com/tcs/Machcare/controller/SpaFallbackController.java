package com.tcs.Machcare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaFallbackController {
  @RequestMapping({
    "/",
    "/landing",
    "/login",
    "/setup-account",
    "/forgot-password",
    "/fault-log",
    "/add-fault",
    "/admin/**",
    "/operator/**",
    "/engineer/**"
  })
  public String forwardToIndex() {
    return "forward:/index.html";
  }
}
