package com.example.librarymanagementsystem.service;

import com.example.librarymanagementsystem.model.BorrowingRecord;
import com.example.librarymanagementsystem.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "library.notifications.enabled", havingValue = "true")
public class OverdueReminderService {
    private final JavaMailSender mailSender;
    private final BorrowingRepository borrowingRepository;

    @Value("${library.notifications.from:}")
    private String configuredFrom;
    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Scheduled(cron = "${library.notifications.reminder-cron:0 0 8 * * *}")
    @Transactional
    public void sendOverdueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<BorrowingRecord> records = borrowingRepository.findOverdueBorrowingsForReminder(now.minusHours(24));
        for (BorrowingRecord record : records) {
            String recipient = record.getMember().getEmail();
            if (recipient == null || recipient.isBlank()) {
                log.warn("Skipping overdue reminder for borrowing {} because member email is missing.", record.getId());
                continue;
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                String from = configuredFrom == null || configuredFrom.isBlank() ? mailUsername : configuredFrom;
                if (from != null && !from.isBlank()) message.setFrom(from);
                message.setTo(recipient);
                message.setSubject("Library book overdue: " + record.getBook().getTitle());
                message.setText("Hello " + record.getMember().getFirstName() + ",\n\n"
                        + "Our records show that the book '" + record.getBook().getTitle()
                        + "' was due on " + record.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                        + ". Please return it to the library as soon as possible.\n\nThank you,\nLibrary Management");
                mailSender.send(message);
                record.setLastReminderSentAt(now);
                borrowingRepository.save(record);
                log.info("Sent overdue reminder for borrowing {}", record.getId());
            } catch (RuntimeException ex) {
                log.error("Could not send overdue reminder for borrowing {}.", record.getId(), ex);
            }
        }
    }
}