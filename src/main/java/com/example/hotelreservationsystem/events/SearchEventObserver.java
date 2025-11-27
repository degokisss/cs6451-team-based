package com.example.hotelreservationsystem.events;

/**
 * Observer interface for room search events
 * Implement this interface to receive notifications about search actions
 */
public interface SearchEventObserver {

    /**
     * Called when a search event occurs
     *
     * @param event The search event containing details about the search action
     */
    void onSearchEvent(SearchEvent event);

    /**
     * Get observer name for logging purposes
     *
     * @return Observer identifier
     */
    default String getObserverName() {
        return this.getClass().getSimpleName();
    }
}
