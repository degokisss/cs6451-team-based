package com.example.hotelreservationsystem.events.observers;

import com.example.hotelreservationsystem.events.LockEvent;
import com.example.hotelreservationsystem.events.LockEventObserver;
import com.example.hotelreservationsystem.events.SearchEvent;
import com.example.hotelreservationsystem.events.SearchEventObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Combined observer that tracks both search and lock events to provide
 * comprehensive analytics on user behavior, conversion rates, and demand patterns.
 * <p>
 * This observer correlates search activity with booking attempts to:
 * - Track search-to-lock conversion rates
 * - Identify popular search filters and room types
 * - Detect high-demand periods and rooms
 * - Monitor abandonment patterns
 * - Provide insights for pricing and inventory optimization
 */
@Component
@Slf4j
public class CombinedAnalyticsObserver implements LockEventObserver, SearchEventObserver {

    @Override
    public String getObserverName() {
        return "CombinedAnalyticsObserver";
    }

    // Metrics tracking
    private final AtomicInteger totalSearches = new AtomicInteger(0);
    private final AtomicInteger searchesWithResults = new AtomicInteger(0);
    private final AtomicInteger searchesWithNoResults = new AtomicInteger(0);
    private final AtomicInteger totalLocks = new AtomicInteger(0);
    private final AtomicInteger lockConflicts = new AtomicInteger(0);
    private final AtomicInteger lockExpirations = new AtomicInteger(0);

    // Room-level analytics
    private final ConcurrentHashMap<Long, AtomicInteger> roomSearchCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> roomLockCount = new ConcurrentHashMap<>();

    // Customer journey tracking (customerId -> last search time)
    private final ConcurrentHashMap<Long, LocalDateTime> customerLastSearch = new ConcurrentHashMap<>();

    @Override
    public void onSearchEvent(SearchEvent event) {
        totalSearches.incrementAndGet();

        switch (event.getEventType()) {
            case SEARCH_PERFORMED:
                log.info("Combined Analytics: Search performed by customer {} with {} results",
                    event.getCustomerId(), event.getResultsCount());

                if (event.getResultsCount() > 0) {
                    searchesWithResults.incrementAndGet();
                } else {
                    searchesWithNoResults.incrementAndGet();
                    log.warn("Combined Analytics: No results found for search criteria - potential demand gap");
                }

                // Track customer journey
                if (event.getCustomerId() != null) {
                    customerLastSearch.put(event.getCustomerId(), event.getTimestamp());
                }

                // Track search patterns
                trackSearchCriteria(event);
                break;

            case SEARCH_NO_RESULTS:
                searchesWithNoResults.incrementAndGet();
                log.warn("Combined Analytics: Search with no results - criteria: {}",
                    event.getSearchCriteria());
                // TODO: Send alert for potential inventory gaps or pricing issues
                break;

            case SEARCH_WITH_FILTERS:
                log.info("Combined Analytics: Filtered search by customer {} - filters: {}",
                    event.getCustomerId(), event.getSearchCriteria());
                // TODO: Track most popular filters for UX optimization
                break;

            case SEARCH_ALL_ROOMS:
                log.info("Combined Analytics: Generic search (no filters) by customer {}",
                    event.getCustomerId());
                break;
        }

        logCurrentMetrics();
    }

    @Override
    public void onLockEvent(LockEvent event) {
        switch (event.getEventType()) {
            case LOCK_CREATED:
                totalLocks.incrementAndGet();
                roomLockCount.computeIfAbsent(event.getRoomId(), _ -> new AtomicInteger(0))
                    .incrementAndGet();

                log.info("Combined Analytics: Lock created for room {} by customer {}",
                    event.getRoomId(), event.getCustomerId());

                // Calculate search-to-lock conversion
                analyzeConversion(event);
                break;

            case LOCK_RELEASED:
                log.info("Combined Analytics: Lock released for room {} - reason: {}",
                    event.getRoomId(), event.getReason());

                if ("booking_completed".equals(event.getReason())) {
                    log.info("Combined Analytics: Successful conversion - booking completed for room {}",
                        event.getRoomId());
                    // TODO: Track full funnel conversion (search -> lock -> booking)
                }
                break;

            case LOCK_EXPIRED:
                lockExpirations.incrementAndGet();
                log.warn("Combined Analytics: Lock expired for room {} - abandoned cart detected",
                    event.getRoomId());
                // TODO: Trigger remarketing/notification for abandoned bookings
                break;

            case LOCK_CONFLICT_DETECTED:
                lockConflicts.incrementAndGet();
                roomLockCount.computeIfAbsent(event.getRoomId(), _ -> new AtomicInteger(0))
                    .incrementAndGet();

                log.warn("Combined Analytics: Lock conflict for room {} - HIGH DEMAND SIGNAL",
                    event.getRoomId());
                // TODO: Alert pricing engine for dynamic pricing adjustment
                break;
        }

        logCurrentMetrics();
    }

    /**
     * Analyze conversion from search to lock for a customer
     */
    private void analyzeConversion(LockEvent lockEvent) {
        if (lockEvent.getCustomerId() == null) {
            return;
        }

        LocalDateTime lastSearchTime = customerLastSearch.get(lockEvent.getCustomerId());
        if (lastSearchTime != null) {
            long secondsBetween = java.time.Duration.between(lastSearchTime, lockEvent.getTimestamp()).getSeconds();
            log.info("Combined Analytics: Customer {} converted from search to lock in {} seconds",
                lockEvent.getCustomerId(), secondsBetween);

            if (secondsBetween < 60) {
                log.info("Combined Analytics: FAST CONVERSION (<1min) - Good UX signal");
            } else if (secondsBetween > 300) {
                log.info("Combined Analytics: SLOW CONVERSION (>5min) - Potential UX friction");
            }

            // TODO: Send conversion metrics to analytics platform
        }
    }

    /**
     * Track search criteria patterns for insights
     */
    private void trackSearchCriteria(SearchEvent event) {
        SearchEvent.SearchCriteria criteria = event.getSearchCriteria();
        if (criteria == null) {
            return;
        }

        if (criteria.getRoomTypeId() != null) {
            log.info("Combined Analytics: Room type {} searched", criteria.getRoomTypeId());
            // TODO: Track most searched room types
        }

        if (criteria.getHotelId() != null) {
            log.info("Combined Analytics: Hotel {} searched", criteria.getHotelId());
            // TODO: Track most popular hotels
        }

        if (criteria.getCheckInDate() != null) {
            log.info("Combined Analytics: Date-based search - check-in: {}", criteria.getCheckInDate());
            // TODO: Track booking lead times and demand patterns
        }
    }

    /**
     * Log current metrics summary
     */
    private void logCurrentMetrics() {
        int searches = totalSearches.get();
        int locks = totalLocks.get();

        double conversionRate = searches > 0 ? (locks * 100.0 / searches) : 0.0;
        double noResultsRate = searches > 0 ? (searchesWithNoResults.get() * 100.0 / searches) : 0.0;

        log.info("=== Combined Analytics Metrics ===");
        log.info("Total Searches: {}", searches);
        log.info("Searches with Results: {}", searchesWithResults.get());
        log.info("Searches with No Results: {} ({:.2}%)", searchesWithNoResults.get(), noResultsRate);
        log.info("Total Locks: {}", locks);
        log.info("Lock Conflicts: {}", lockConflicts.get());
        log.info("Lock Expirations: {}", lockExpirations.get());
        log.info("Search-to-Lock Conversion Rate: {:.2}%", conversionRate);
        log.info("==================================");
    }

    /**
     * Get current analytics snapshot (for API endpoints or dashboards)
     */
    public AnalyticsSnapshot getAnalyticsSnapshot() {
        return AnalyticsSnapshot.builder()
            .totalSearches(totalSearches.get())
            .searchesWithResults(searchesWithResults.get())
            .searchesWithNoResults(searchesWithNoResults.get())
            .totalLocks(totalLocks.get())
            .lockConflicts(lockConflicts.get())
            .lockExpirations(lockExpirations.get())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Analytics snapshot for external consumption
     */
    @lombok.Data
    @lombok.Builder
    public static class AnalyticsSnapshot {
        private int totalSearches;
        private int searchesWithResults;
        private int searchesWithNoResults;
        private int totalLocks;
        private int lockConflicts;
        private int lockExpirations;
        private LocalDateTime timestamp;

        public double getConversionRate() {
            return totalSearches > 0 ? (totalLocks * 100.0 / totalSearches) : 0.0;
        }

        public double getNoResultsRate() {
            return totalSearches > 0 ? (searchesWithNoResults * 100.0 / totalSearches) : 0.0;
        }
    }
}
