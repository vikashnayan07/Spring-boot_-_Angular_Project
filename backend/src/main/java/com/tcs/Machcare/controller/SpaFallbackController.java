package com.tcs.Machcare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaFallbackController {
  @RequestMapping({
    "/admin/**",
    "/engineer/**",
    "/operator/**",
    "/landing",
    "/login",
    "/setup-account",
    "/forgot-password",
    "/fault-log",
    "/add-fault"
  })
  public String forwardToIndex() {
    return "forward:/index.html";
  }
}
