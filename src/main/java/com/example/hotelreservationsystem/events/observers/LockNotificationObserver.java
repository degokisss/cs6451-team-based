package com.example.hotelreservationsystem.events.observers;

import com.example.hotelreservationsystem.events.LockEvent;
import com.example.hotelreservationsystem.events.LockEventObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Observer that sends notifications to customers about lock events
 * Example: Email/SMS notifications, WebSocket updates, push notifications
 */
@Component
@Slf4j
public class LockNotificationObserver implements LockEventObserver {

    @Override
    public void onLockEvent(LockEvent event) {
        switch (event.getEventType()) {
            case LOCK_CREATED:
                sendLockConfirmation(event);
                break;

            case LOCK_EXPIRED:
                sendLockExpiredNotification(event);
                break;

            case LOCK_RELEASED:
                handleLockRelease(event);
                break;

            case LOCK_CONFLICT_DETECTED:
                sendConflictNotification(event);
                break;
        }
    }

    private void sendLockConfirmation(LockEvent event) {
        log.info("Notification: Sending lock confirmation to customer {} for room {}",
            event.getCustomerId(), event.getRoomId());

        // TODO: Send email/SMS/push notification
        // Example: "Your room {roomId} is reserved for 10 minutes. Complete your booking!"
        // notificationService.sendEmail(customer, "Room Reserved", emailTemplate);

        // TODO: Send WebSocket message for real-time UI update
        // webSocketService.sendToUser(customerId, new LockCreatedMessage(lockId, expiresAt));
    }

    private void sendLockExpiredNotification(LockEvent event) {
        log.warn("Notification: Lock expired for customer {} - room {} is now available again",
            event.getCustomerId(), event.getRoomId());

        // TODO: Send expiration notification
        // Example: "Your reservation for room {roomId} has expired. Would you like to try again?"
        // notificationService.sendEmail(customer, "Reservation Expired", emailTemplate);

        // TODO: Send to waiting list - notify next customer that room is available
        // waitingListService.notifyNextCustomer(roomId);
    }

    private void handleLockRelease(LockEvent event) {
        if ("manual".equals(event.getReason())) {
            log.info("Notification: Customer {} released lock for room {} - likely completed booking",
                event.getCustomerId(), event.getRoomId());

            // TODO: Send booking confirmation or cancellation acknowledgment
        } else if ("admin_release".equals(event.getReason())) {
            log.info("Notification: Admin released lock for room {}",
                event.getRoomId());

            // TODO: Notify customer their lock was released by admin
            // notificationService.sendEmail(customer, "Lock Released", emailTemplate);
        }
    }

    private void sendConflictNotification(LockEvent event) {
        log.warn("Notification: Lock conflict detected for room {} - notifying customer {} that room is unavailable",
            event.getRoomId(), event.getCustomerId());

        // TODO: Send real-time notification to customer
        // Example: "This room is currently being booked by another guest. Here are similar alternatives..."
        // webSocketService.sendToUser(customerId, new RoomUnavailableMessage(roomId, alternatives));
    }
}
