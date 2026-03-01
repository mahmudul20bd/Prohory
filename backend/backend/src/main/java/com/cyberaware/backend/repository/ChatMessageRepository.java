package com.cyberaware.backend.repository;

import com.cyberaware.backend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // নির্দিষ্ট ইউজারের চ্যাট হিস্ট্রি খুঁজে বের করার জন্য কাস্টম মেথড
    List<ChatMessage> findBySenderUsernameOrReceiverUsername(String sender, String receiver);
}