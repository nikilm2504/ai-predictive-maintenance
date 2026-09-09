package com.pmp.predictivemaintenance.dashboard.service;

import com.pmp.predictivemaintenance.dashboard.dto.MachineUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    
    // Keep alive connection for 30 minutes. The frontend will automatically reconnect if it drops.
    private static final Long DEFAULT_TIMEOUT = 30L * 60 * 1000;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitter.onCompletion(() -> removeEmitter(emitter));
        emitter.onTimeout(() -> removeEmitter(emitter));
        emitter.onError((e) -> removeEmitter(emitter));

        emitters.add(emitter);
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            removeEmitter(emitter);
        }

        log.info("New SSE client subscribed. Total clients: {}", emitters.size());
        return emitter;
    }

    private void removeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
        log.debug("SSE client disconnected. Total clients: {}", emitters.size());
    }

    @EventListener
    public void handleMachineUpdate(MachineUpdateEvent event) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("MACHINE_UPDATE")
                        .data(event.getUpdateData()));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.info("Removed {} dead SSE emitters.", deadEmitters.size());
        }
    }
}
