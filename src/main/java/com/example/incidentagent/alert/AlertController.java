package com.example.incidentagent.alert;

import com.example.incidentagent.alert.dto.CreateAlertRequest;
import com.example.incidentagent.alert.dto.CreateAlertResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAlertResponse createAlert(@Valid @RequestBody CreateAlertRequest request) {
        return alertService.ingestAlert(request);
    }
}

