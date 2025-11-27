package com.example.hotelreservationsystem.base.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SMSNotification implements Notification<String> {
    @Override
    public boolean sendNotification(String message) {
        // sms implementation
        log.info("send SMS: {}", message);
        return true;
    }
}
