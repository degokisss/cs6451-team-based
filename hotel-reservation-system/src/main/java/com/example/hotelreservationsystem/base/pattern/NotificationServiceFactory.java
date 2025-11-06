package com.example.hotelreservationsystem.base.pattern;

import com.example.hotelreservationsystem.base.notification.EmailNotification;
import com.example.hotelreservationsystem.base.notification.INotification;
import com.example.hotelreservationsystem.base.notification.SMSNotification;
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
    public <T> INotification<T> createNotificationService(NotificationType type) {
        if (type == NotificationType.EMAIL) {
            return (INotification<T>) emailNotification;
        } else if (type == NotificationType.SMS) {
            return (INotification<T>) smsNotification;
        }
        return null;
    }
}
