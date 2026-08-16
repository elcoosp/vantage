package com.vantage.core.ratelimiter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ratelimit-test")
public class RateLimitTestController {

    @GetMapping
    public String test() {
        return "OK";
    }
}
