package com.khaata.controller;

import com.khaata.entity.ReminderLog;
import com.khaata.repository.ReminderLogRepository;
import com.khaata.service.ReminderService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets the owner see the reminder audit log and manually trigger a run
 * (useful for demos and testing without waiting for the daily cron).
 */
@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final ReminderLogRepository reminderLogRepository;

    public ReminderController(ReminderService reminderService,
                              ReminderLogRepository reminderLogRepository) {
        this.reminderService = reminderService;
        this.reminderLogRepository = reminderLogRepository;
    }

    /** Trigger a reminder run immediately. Returns how many messages were sent. */
    @PostMapping("/run")
    public Map<String, Object> runNow() {
        int sent = reminderService.runOnce();
        return Map.of("sent", sent);
    }

    /** Send a reminder to one specific customer now (bypasses overdue/idempotency). */
    @PostMapping("/customer/{id}")
    public Map<String, Object> remindCustomer(@org.springframework.web.bind.annotation.PathVariable Long id) {
        boolean sent = reminderService.remindCustomerNow(id);
        return Map.of("sent", sent);
    }

    /** Most recent reminder attempts (audit trail). */
    @GetMapping("/log")
    public List<ReminderLog> recentLog() {
        return reminderLogRepository.findTop100ByOrderByCreatedAtDesc();
    }
}
