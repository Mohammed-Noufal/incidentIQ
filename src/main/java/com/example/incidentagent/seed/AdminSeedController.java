package com.example.incidentagent.seed;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminSeedController {

    private final SeedLoader seedLoader;

    public AdminSeedController(SeedLoader seedLoader) {
        this.seedLoader = seedLoader;
    }

    @PostMapping("/seed")
    public SeedLoader.SeedResult seedDatabase() {
        return seedLoader.loadSeedData();
    }
}

