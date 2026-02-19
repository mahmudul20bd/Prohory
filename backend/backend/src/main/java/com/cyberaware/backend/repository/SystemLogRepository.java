package com.cyberaware.backend.repository;

import com.cyberaware.backend.model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    // এটি লেটেস্ট লগগুলো সবার উপরে দেখানোর জন্য ব্যবহার করা হয়
    List<SystemLog> findAllByOrderByTimestampDesc();
}