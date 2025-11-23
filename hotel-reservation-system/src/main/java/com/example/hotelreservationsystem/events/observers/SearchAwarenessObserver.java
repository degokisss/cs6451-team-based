package com.example.hotelreservationsystem.events.observers;

import com.example.hotelreservationsystem.events.LockEvent;
import com.example.hotelreservationsystem.events.LockEventObserver;
import com.example.hotelreservationsystem.events.SearchEvent;
import com.example.hotelreservationsystem.events.SearchEventObserver;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Observer that bridges search and lock events to enable real-time room availability.
 * <p>
 * This observer:
 * 1. Tracks customer searches (what rooms/criteria they searched for)
 * 2. Observes lock release events
 * 3. Notifies customers when rooms matching their search become available
 * 4. Provides cache invalidation signals for search results
 * <p>
 * Use cases:
 * - Customer searches for room type X but it's all locked -> notify when one becomes available
 * - High-demand rooms that were locked -> alert waiting customers immediately
 * - Abandoned booking locks -> make rooms available to other interested customers
 */
@Component
@Slf4j
public class SearchAwarenessObserver implements LockEventObserver, SearchEventObserver {

    @Override
    public String getObserverName() {
        return "SearchAwarenessObserver";
    }

    // Track recent searches by customer (customerId -> list of recent searches)
    private final Map<Long, List<RecentSearch>> customerSearchHistory = new ConcurrentHashMap<>();

    // Track customers interested in specific rooms (roomId -> list of interested customer IDs)
    private final Map<Long, Set<Long>> roomInterestMap = new ConcurrentHashMap<>();

    // Track searches with no results (for notification when rooms become available)
    private final List<SearchEvent> noResultSearches = Collections.synchronizedList(new ArrayList<>());

    private static final int MAX_SEARCH_HISTORY_PER_CUSTOMER = 10;
    private static final int SEARCH_RELEVANCE_MINUTES = 30; // Only notify for recent searches

    @Override
    public void onSearchEvent(SearchEvent event) {
        if (event.getCustomerId() == null) {
            return; // Skip anonymous searches
        }

        log.info("SearchAwareness: Tracking search by customer {} - {} results",
            event.getCustomerId(), event.getResultsCount());

        // Track customer search history
        trackCustomerSearch(event);

        // Track no-result searches for proactive notifications
        if (event.getResultsCount() == 0) {
            noResultSearches.add(event);
            log.info("SearchAwareness: Customer {} got no results - will notify when matching rooms available",
                event.getCustomerId());
        }

        // Register customer interest in specific room types/hotels
        registerCustomerInterest(event);
    }

    @Override
    public void onLockEvent(LockEvent event) {
        if (event.getEventType() != LockEvent.LockEventType.LOCK_RELEASED &&
            event.getEventType() != LockEvent.LockEventType.LOCK_EXPIRED) {
            return; // Only care about rooms becoming available
        }

        log.info("SearchAwareness: Room {} lock released/expired - checking for interested customers",
            event.getRoomId());

        // Check if any customers are interested in this room
        Set<Long> interestedCustomers = roomInterestMap.getOrDefault(event.getRoomId(), Collections.emptySet());

        if (!interestedCustomers.isEmpty()) {
            log.info("SearchAwareness: Found {} customers interested in room {}",
                interestedCustomers.size(), event.getRoomId());

            // Notify interested customers
            notifyInterestedCustomers(event.getRoomId(), interestedCustomers);

            // Remove from interest map (one-time notification)
            roomInterestMap.remove(event.getRoomId());
        }

        // Check for customers who had no results with matching criteria
        notifyCustomersWithNoResults(event.getRoomId());

        // Signal cache invalidation
        signalCacheInvalidation(event.getRoomId());
    }

    /**
     * Track customer search in history
     */
    private void trackCustomerSearch(SearchEvent event) {
        Long customerId = event.getCustomerId();

        customerSearchHistory.compute(customerId, (_, searches) -> {
            if (searches == null) {
                searches = new ArrayList<>();
            }

            searches.add(RecentSearch.builder()
                .criteria(event.getSearchCriteria())
                .timestamp(event.getTimestamp())
                .resultsCount(event.getResultsCount())
                .build());

            // Keep only recent searches
            if (searches.size() > MAX_SEARCH_HISTORY_PER_CUSTOMER) {
                searches.removeFirst();
            }

            return searches;
        });
    }

    /**
     * Register customer interest based on search criteria
     * This creates a mapping of roomId -> interested customers
     */
    private void registerCustomerInterest(SearchEvent event) {
        // For now, we register interest at the criteria level
        // In a real system, you'd match room attributes to criteria
        log.debug("SearchAwareness: Registered interest for customer {} with criteria: {}",
            event.getCustomerId(), event.getSearchCriteria());
        // TODO: Implement room matching logic based on criteria
    }

    /**
     * Notify customers who are interested in this specific room
     */
    private void notifyInterestedCustomers(Long roomId, Set<Long> customerIds) {
        for (Long customerId : customerIds) {
            log.info("SearchAwareness: NOTIFICATION -> Customer {}: Room {} is now available!",
                customerId, roomId);

            // TODO: Send actual notification (email, push, WebSocket, SMS)
            // notificationService.sendRoomAvailableNotification(customerId, roomId);
        }
    }

    /**
     * Notify customers who had no results but might be interested in this room
     */
    private void notifyCustomersWithNoResults(Long roomId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(SEARCH_RELEVANCE_MINUTES);

        // Find recent no-result searches that might match this room
        List<SearchEvent> relevantSearches = noResultSearches.stream()
            .filter(search -> search.getTimestamp().isAfter(cutoffTime))
            .toList();

        for (SearchEvent search : relevantSearches) {
            // TODO: Check if room matches search criteria
            log.info("SearchAwareness: Potential match for customer {} who had no results earlier",
                search.getCustomerId());

            // TODO: Send notification if criteria matches
            // if (roomMatchesCriteria(roomId, search.getSearchCriteria())) {
            //     notificationService.sendRoomAvailableNotification(search.getCustomerId(), roomId);
            // }
        }

        // Clean up old no-result searches
        noResultSearches.removeIf(search -> search.getTimestamp().isBefore(cutoffTime));
    }

    /**
     * Signal that search result caches should be invalidated
     */
    private void signalCacheInvalidation(Long roomId) {
        log.info("SearchAwareness: CACHE INVALIDATION signal for room {}", roomId);

        // Since current implementation doesn't use caching (queries DB directly),
        // this is a placeholder for future caching implementation

        // TODO: Invalidate Redis cache keys related to room searches
        // cacheManager.evictCache("room-search:*");

        // TODO: Broadcast cache invalidation to all app instances
        // redisPublisher.publish("cache-invalidation", roomId);
    }

    /**
     * Get customers who recently searched for similar criteria
     * Useful for targeted marketing when rooms become available
     */
    public List<Long> getInterestedCustomers(SearchEvent.SearchCriteria criteria) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(SEARCH_RELEVANCE_MINUTES);

        return customerSearchHistory.entrySet().stream()
            .filter(entry -> hasRecentMatchingSearch(entry.getValue(), criteria, cutoffTime))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private boolean hasRecentMatchingSearch(List<RecentSearch> searches,
                                           SearchEvent.SearchCriteria criteria,
                                           LocalDateTime cutoffTime) {
        return searches.stream()
            .anyMatch(search -> search.getTimestamp().isAfter(cutoffTime) &&
                searchCriteriaMatch(search.getCriteria(), criteria));
    }

    private boolean searchCriteriaMatch(SearchEvent.SearchCriteria c1, SearchEvent.SearchCriteria c2) {
        if (c1 == null || c2 == null) {
            return false;
        }

        return Objects.equals(c1.getRoomTypeId(), c2.getRoomTypeId()) &&
               Objects.equals(c1.getHotelId(), c2.getHotelId());
    }

    /**
     * Recent search tracking
     */
    @Data
    @Builder
    @AllArgsConstructor
    private static class RecentSearch {
        private SearchEvent.SearchCriteria criteria;
        private LocalDateTime timestamp;
        private int resultsCount;
    }
}
