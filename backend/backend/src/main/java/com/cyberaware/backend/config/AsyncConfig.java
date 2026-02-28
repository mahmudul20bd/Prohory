package com.cyberaware.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // কোর থ্রেড ২০ করে দেওয়া হলো যাতে কোনো কাজ Queue-তে বসে না থাকে
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        // Queue সাইজ কমিয়ে দেওয়া হলো
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("SecurityScanner-");
        executor.initialize();
        return executor;
    }
}