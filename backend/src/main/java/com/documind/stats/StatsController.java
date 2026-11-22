package com.documind.stats;

import com.documind.auth.AuthFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService stats;
    private final AuthFacade auth;

    public StatsController(StatsService stats, AuthFacade auth) {
        this.stats = stats;
        this.auth = auth;
    }

    @GetMapping("/me")
    public PersonalStatsResponse mine() {
        return stats.personal(auth.current());
    }
}
