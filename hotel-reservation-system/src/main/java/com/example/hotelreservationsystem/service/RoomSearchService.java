package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.example.hotelreservationsystem.events.SearchEvent;
import com.example.hotelreservationsystem.events.SearchEventObserver;
import com.example.hotelreservationsystem.repository.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Room Search Service with Observer Pattern support
 * Manages room searches and notifies observers of search events
 */
@Service
@Slf4j
public class RoomSearchService {

    private final RoomRepository roomRepository;
    private final BookingLockService bookingLockService;

    // Observer pattern: List of observers to notify on search events
    private final List<SearchEventObserver> observers = new CopyOnWriteArrayList<>();

    /**
     * Constructor that auto-registers Spring-managed observers
     */
    public RoomSearchService(
        RoomRepository roomRepository,
        BookingLockService bookingLockService,
        List<SearchEventObserver> searchObservers
    ) {
        this.roomRepository = roomRepository;
        this.bookingLockService = bookingLockService;

        // Auto-register all Spring-managed observers
        searchObservers.forEach(this::registerObserver);
        log.info("RoomSearchService initialized with {} observers", this.observers.size());
    }

    /**
     * Register an observer to receive search event notifications
     */
    public void registerObserver(SearchEventObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            log.debug("Registered search observer: {}", observer.getObserverName());
        }
    }

    /**
     * Unregister an observer
     */
    public void unregisterObserver(SearchEventObserver observer) {
        observers.remove(observer);
        log.debug("Unregistered search observer: {}", observer.getObserverName());
    }

    /**
     * Notify all observers of a search event
     */
    private void notifyObservers(SearchEvent event) {
        log.debug("Notifying {} observers of search event: {}", observers.size(), event.getEventType());
        observers.forEach(observer -> {
            try {
                observer.onSearchEvent(event);
            } catch (Exception e) {
                log.error("Observer {} failed to handle search event", observer.getObserverName(), e);
            }
        });
    }

    /**
     * Search for available rooms based on criteria
     * Excludes rooms that are locked or not available
     * Notifies observers of the search event
     *
     * @param checkInDate  Check-in date (future enhancement for date-based booking)
     * @param checkOutDate Check-out date (future enhancement for date-based booking)
     * @param roomTypeId   Room type filter (optional)
     * @param hotelId      Hotel filter (optional)
     * @return List of available rooms
     */
    public List<Room> searchAvailableRooms(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long roomTypeId,
        Long hotelId
    ) {
        return searchAvailableRooms(checkInDate, checkOutDate, roomTypeId, hotelId, null);
    }

    /**
     * Search for available rooms based on criteria with customer tracking
     * Excludes rooms that are locked or not available
     * Notifies observers of the search event
     *
     * @param checkInDate  Check-in date (future enhancement for date-based booking)
     * @param checkOutDate Check-out date (future enhancement for date-based booking)
     * @param roomTypeId   Room type filter (optional)
     * @param hotelId      Hotel filter (optional)
     * @param customerId   Customer performing the search (optional)
     * @return List of available rooms
     */
    public List<Room> searchAvailableRooms(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long roomTypeId,
        Long hotelId,
        Long customerId
    ) {
        log.info("Searching rooms - checkIn: {}, checkOut: {}, roomTypeId: {}, hotelId: {}, customerId: {}",
            checkInDate, checkOutDate, roomTypeId, hotelId, customerId);

        // Build search criteria
        SearchEvent.SearchCriteria criteria = SearchEvent.SearchCriteria.builder()
            .checkInDate(checkInDate)
            .checkOutDate(checkOutDate)
            .roomTypeId(roomTypeId)
            .hotelId(hotelId)
            .build();

        List<Room> rooms;

        // Query based on provided filters
        if (roomTypeId != null) {
            // Filter by room type and available status
            rooms = roomRepository.findByRoomType_IdAndRoomStatus(roomTypeId, RoomStatus.AVAILABLE);
        } else {
            // Get all available rooms
            rooms = roomRepository.findAll().stream()
                                            .filter(room -> room.getRoomStatus() == RoomStatus.AVAILABLE)
                                            .collect(Collectors.toList());
        }

        // Filter by hotel if specified
        if (hotelId != null) {
            rooms = rooms.stream()
                         .filter(room -> room.getHotel() != null && room.getHotel().getId().equals(hotelId))
                         .collect(Collectors.toList());
        }

        // Exclude locked rooms
        rooms = rooms.stream()
                     .filter(room -> !bookingLockService.isLocked(room.getId()))
                     .collect(Collectors.toList());

        log.info("Found {} available rooms", rooms.size());

        // Notify observers of the search event
        notifySearchObservers(customerId, criteria, rooms.size());

        return rooms;
    }

    /**
     * Create and notify observers of a search event
     */
    private void notifySearchObservers(Long customerId, SearchEvent.SearchCriteria criteria, int resultsCount) {
        // Determine event type based on search characteristics
        SearchEvent.SearchEventType eventType;
        if (resultsCount == 0) {
            eventType = SearchEvent.SearchEventType.SEARCH_NO_RESULTS;
        } else if (criteria.hasFilters()) {
            eventType = SearchEvent.SearchEventType.SEARCH_WITH_FILTERS;
        } else {
            eventType = SearchEvent.SearchEventType.SEARCH_ALL_ROOMS;
        }

        // Build and fire search event
        SearchEvent event = SearchEvent.builder()
            .eventType(eventType)
            .customerId(customerId)
            .timestamp(LocalDateTime.now())
            .searchCriteria(criteria)
            .resultsCount(resultsCount)
            .build();

        notifyObservers(event);

        // Also fire generic SEARCH_PERFORMED event for overall metrics
        if (eventType != SearchEvent.SearchEventType.SEARCH_NO_RESULTS) {
            SearchEvent performedEvent = SearchEvent.builder()
                .eventType(SearchEvent.SearchEventType.SEARCH_PERFORMED)
                .customerId(customerId)
                .timestamp(LocalDateTime.now())
                .searchCriteria(criteria)
                .resultsCount(resultsCount)
                .build();
            notifyObservers(performedEvent);
        }
    }

    /**
     * Get all available rooms (without filters)
     *
     * @return List of available, unlocked rooms
     */
    public List<Room> getAllAvailableRooms() {
        return searchAvailableRooms(null, null, null, null);
    }
}
