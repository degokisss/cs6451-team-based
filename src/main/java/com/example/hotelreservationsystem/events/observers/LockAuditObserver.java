package com.example.hotelreservationsystem.events.observers;

import com.example.hotelreservationsystem.events.LockEvent;
import com.example.hotelreservationsystem.events.LockEventObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Observer that creates audit logs for lock operations
 * Useful for compliance, debugging, and security monitoring
 */
@Component
@Slf4j
public class LockAuditObserver implements LockEventObserver {

    @Override
    public void onLockEvent(LockEvent event) {
        // Create audit log entry
        log.info("AUDIT: Lock event - Type: {}, Room: {}, Customer: {}, LockId: {}, Time: {}, Reason: {}",
            event.getEventType(),
            event.getRoomId(),
            event.getCustomerId(),
            event.getLockId(),
            event.getTimestamp(),
            event.getReason()
        );

        // TODO: Persist to audit log table/service
        // auditLogRepository.save(new AuditLog(event));
    }
}
