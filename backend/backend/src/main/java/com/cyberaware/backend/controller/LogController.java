package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.SystemLog;
import com.cyberaware.backend.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    @Autowired private SystemLogRepository logRepository;

    @GetMapping
    public List<SystemLog> getLogs() {
        return logRepository.findAllByOrderByTimestampDesc();
    }
}