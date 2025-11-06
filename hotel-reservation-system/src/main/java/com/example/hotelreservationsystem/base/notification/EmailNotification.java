package com.example.hotelreservationsystem.base.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailNotification implements Notification<SimpleMailMessage> {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public boolean sendNotification(SimpleMailMessage message) {
        log.info("send email: {}", message);
        try {
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("error sending email: {}", e.getMessage());
            return false;
        }
    }
}
