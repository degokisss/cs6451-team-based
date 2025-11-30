package com.example.hotelreservationsystem.base.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Notification implementation that sends email messages using Spring's {@link JavaMailSender}.
 *
 * <p>Accepts {@link SimpleMailMessage} as the payload and attempts to send it via the configured
 * mail sender. Errors while sending are logged and result in a {@code false} return value.</p>
 */
@Slf4j
@Service
public class EmailNotification implements Notification<SimpleMailMessage> {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Send an email message.
     *
     * @param message the {@link SimpleMailMessage} to send
     * @return {@code true} if the message was sent successfully; {@code false} on error
     */
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