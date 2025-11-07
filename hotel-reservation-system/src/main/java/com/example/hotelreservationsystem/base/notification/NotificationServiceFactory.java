package com.example.hotelreservationsystem.base.notification;

import com.example.hotelreservationsystem.enums.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceFactory {
    private final EmailNotification emailNotification;
    private final SMSNotification smsNotification;

    public NotificationServiceFactory(EmailNotification emailNotification, SMSNotification smsNotification) {
        this.emailNotification = emailNotification;
        this.smsNotification = smsNotification;
    }

    @SuppressWarnings("unchecked")
    public <T> Notification<T> createNotificationService(NotificationType type) throws Exception {
        if (type == NotificationType.EMAIL) {
            return (Notification<T>) emailNotification;
        } else if (type == NotificationType.SMS) {
            return (Notification<T>) smsNotification;
        }
        throw new Exception(type + " not yet implement");
    }
}
