package com.cyberaware.backend.repository;

import com.cyberaware.backend.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 🔥 এই মেথডটি মিসিং ছিল, তাই লাল দাগ আসছিল
    List<Report> findBySender(String sender);

}