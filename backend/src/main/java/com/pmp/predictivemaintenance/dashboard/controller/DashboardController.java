package com.pmp.predictivemaintenance.dashboard.controller;

import com.pmp.predictivemaintenance.dashboard.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final SseService sseService;

    public DashboardController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamUpdates() {
        return sseService.subscribe();
    }
}
