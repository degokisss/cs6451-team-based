package com.example.hotelreservationsystem.events;

/**
 * Observer interface for booking lock events
 * Implement this interface to receive notifications about lock state changes
 */
public interface LockEventObserver {

    /**
     * Called when a lock event occurs
     *
     * @param event The lock event containing details about the state change
     */
    void onLockEvent(LockEvent event);

    /**
     * Get observer name for logging purposes
     *
     * @return Observer identifier
     */
    default String getObserverName() {
        return this.getClass().getSimpleName();
    }
}
