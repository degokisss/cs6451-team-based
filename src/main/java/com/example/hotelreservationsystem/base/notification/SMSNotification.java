package com.example.hotelreservationsystem.base.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Simple SMS notification implementation used for sending text messages.
 *
 * <p>This implementation accepts a {@link String} payload and currently logs the sent message.
 * Replace the logging implementation with a real SMS provider integration when needed.</p>
 */
@Service
@Slf4j
public class SMSNotification implements Notification<String> {
    /**
     * Send an SMS message.
     *
     * @param message the text message to send
     * @return {@code true} if the message was (assumed) sent successfully
     */
    @Override
    public boolean sendNotification(String message) {
        // sms implementation
        log.info("send SMS: {}", message);
        return true;
    }
}