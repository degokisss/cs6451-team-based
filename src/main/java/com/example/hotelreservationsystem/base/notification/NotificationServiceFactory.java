package com.example.hotelreservationsystem.base.notification;

import com.example.hotelreservationsystem.enums.NotificationType;
import org.springframework.stereotype.Service;

/**
 * Factory responsible for providing concrete {@link Notification} implementations
 * based on {@link NotificationType}.
 *
 * <p>Because the service implementations use different payload types (for example,
 * {@link EmailNotification} expects {@code SimpleMailMessage} while {@link SMSNotification}
 * expects {@code String}), this factory performs an unchecked cast when returning
 * a {@code Notification<T>} instance. Callers are responsible for using the matching
 * payload type for the selected {@link NotificationType}.</p>
 */
@Service
public class NotificationServiceFactory {
    private final EmailNotification emailNotification;
    private final SMSNotification smsNotification;

    public NotificationServiceFactory(EmailNotification emailNotification, SMSNotification smsNotification) {
        this.emailNotification = emailNotification;
        this.smsNotification = smsNotification;
    }

    /**
     * Create a notification service for the given type.
     *
     * @param type the notification type to create
     * @param <T>  the expected payload type for the returned service
     * @return a concrete {@link Notification} implementation for the requested type
     * @throws Exception if the requested type is not supported
     */
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