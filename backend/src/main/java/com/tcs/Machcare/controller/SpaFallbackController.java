package com.tcs.Machcare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaFallbackController {
  @RequestMapping({
    "/{path:[^\\.]*}",
    "/**/{path:[^\\.]*}"
  })
  public String forwardToIndex() {
    return "forward:/index.html";
  }
}
