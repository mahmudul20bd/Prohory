package com.cyberaware.backend.repository;

import com.cyberaware.backend.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // 🔥 নাম পরিবর্তন: findBySender -> findByReporter
    List<Report> findByReporter(String reporter);
}