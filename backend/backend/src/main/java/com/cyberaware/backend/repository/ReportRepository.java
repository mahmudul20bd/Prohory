package com.cyberaware.backend.repository; // প্যাকেজ চেঞ্জ হয়েছে

import com.cyberaware.backend.model.Report; // সঠিক ইমপোর্ট
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
}