package com.example.hotelreservationsystem.base.pattern;

import com.example.hotelreservationsystem.base.notification.EmailNotification;
import com.example.hotelreservationsystem.base.notification.INotification;
import com.example.hotelreservationsystem.base.notification.SMSNotification;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceFactory {
    private final EmailNotification emailNotification;

    public NotificationServiceFactory(EmailNotification emailNotification) {
        this.emailNotification = emailNotification;
    }

    @SuppressWarnings("unchecked")
    public <T> INotification<T> createNotificationService(String type) {
        if (type.equalsIgnoreCase("email")) {
            return (INotification<T>) emailNotification;
        } else if (type.equalsIgnoreCase("sms")) {
            return (INotification<T>) new SMSNotification();
        }
        return null;
    }
}
