package com.example.hotelreservationsystem.base.notification;

/**
 * Generic notification contract used by the application.
 *
 * <p>Implementations of this interface send messages of type {@code T} (for example,
 * a {@link org.springframework.mail.SimpleMailMessage} for email or {@code String} for SMS)
 * and return whether sending succeeded.</p>
 *
 * @param <T> the message payload type accepted by the notification implementation
 */
public interface Notification<T> {
    /**
     * Send a notification with the given message payload.
     *
     * @param message the message payload to send; implementations may decide how to interpret or validate it
     * @return {@code true} if the notification was successfully sent, {@code false} otherwise
     */
    boolean sendNotification(T message);
}