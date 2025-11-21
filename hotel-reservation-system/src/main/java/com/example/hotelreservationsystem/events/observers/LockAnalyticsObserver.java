package com.example.hotelreservationsystem.events.observers;

import com.example.hotelreservationsystem.events.LockEvent;
import com.example.hotelreservationsystem.events.LockEventObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Observer that tracks analytics for booking locks
 * Example implementation showing how to react to lock events
 */
@Component
@Slf4j
public class LockAnalyticsObserver implements LockEventObserver {

    @Override
    public void onLockEvent(LockEvent event) {
        switch (event.getEventType()) {
            case LOCK_CREATED:
                log.info("Analytics: Lock created for room {} by customer {} at {}",
                    event.getRoomId(), event.getCustomerId(), event.getTimestamp());
                // TODO: Send to analytics service (e.g., track conversion rate)
                break;

            case LOCK_RELEASED:
                log.info("Analytics: Lock released for room {} - reason: {}",
                    event.getRoomId(), event.getReason());
                // TODO: Track lock duration, completion rate
                break;

            case LOCK_EXPIRED:
                log.warn("Analytics: Lock expired for room {} - potential abandoned cart",
                    event.getRoomId());
                // TODO: Track abandonment rate, send remarketing signals
                break;

            case LOCK_CONFLICT_DETECTED:
                log.warn("Analytics: Lock conflict detected for room {} - high demand signal",
                    event.getRoomId());
                // TODO: Track demand, adjust pricing strategy
                break;
        }
    }
}
