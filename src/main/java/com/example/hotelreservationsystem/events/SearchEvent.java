package com.example.hotelreservationsystem.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event representing a room search action
 * Used with Observer pattern for search behavior tracking and analytics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchEvent {

    private SearchEventType eventType;
    private Long customerId;
    private LocalDateTime timestamp;
    private SearchCriteria searchCriteria;
    private int resultsCount;
    private String sessionId; // Optional: for tracking user sessions

    public enum SearchEventType {
        SEARCH_PERFORMED,      // Any search executed
        SEARCH_NO_RESULTS,     // Search returned zero results
        SEARCH_WITH_FILTERS,   // Search with specific filters applied
        SEARCH_ALL_ROOMS       // Generic search without filters
    }

    /**
     * Search criteria used in the search request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchCriteria {
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private Long roomTypeId;
        private Long hotelId;

        public boolean hasFilters() {
            return checkInDate != null || checkOutDate != null ||
                   roomTypeId != null || hotelId != null;
        }
    }
}
