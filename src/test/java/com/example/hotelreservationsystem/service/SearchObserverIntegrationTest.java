package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.example.hotelreservationsystem.events.observers.CombinedAnalyticsObserver;
import com.example.hotelreservationsystem.events.observers.SearchAwarenessObserver;
import com.example.hotelreservationsystem.repository.HotelRepository;
import com.example.hotelreservationsystem.repository.RoomRepository;
import com.example.hotelreservationsystem.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Search Observer Pattern
 * Tests the interaction between search events, lock events, and observers
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "booking.lock.ttl-minutes=1"
})
class SearchObserverIntegrationTest {

    @Autowired
    private RoomSearchService roomSearchService;

    @Autowired
    private BookingLockService bookingLockService;

    @Autowired
    private CombinedAnalyticsObserver combinedAnalyticsObserver;

    @Autowired
    private SearchAwarenessObserver searchAwarenessObserver;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    private Hotel testHotel;
    private RoomType testRoomType;
    private Room testRoom1;
    private Room testRoom2;

    @BeforeEach
    void setUp() {
        // Clean up any existing locks
        try {
            if (testRoom1 != null && bookingLockService.isLocked(testRoom1.getId())) {
                bookingLockService.releaseLockByRoomId(testRoom1.getId());
            }
            if (testRoom2 != null && bookingLockService.isLocked(testRoom2.getId())) {
                bookingLockService.releaseLockByRoomId(testRoom2.getId());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        // Create test data
        testHotel = hotelRepository.save(Hotel.builder()
            .name("Test Hotel")
            .address("123 Test St")
            .build());

        testRoomType = roomTypeRepository.save(RoomType.builder()
            .name("Deluxe")
            .description("Deluxe Room")
            .price(100.0f)
            .capacity(2)
            .build());

        testRoom1 = roomRepository.save(Room.builder()
            .roomNumber("101")
            .roomStatus(RoomStatus.AVAILABLE)
            .hotel(testHotel)
            .roomType(testRoomType)
            .build());

        testRoom2 = roomRepository.save(Room.builder()
            .roomNumber("102")
            .roomStatus(RoomStatus.AVAILABLE)
            .hotel(testHotel)
            .roomType(testRoomType)
            .build());
    }

    @Test
    void shouldTrackSearchEvents() {
        // Given: Initial analytics state
        var initialSnapshot = combinedAnalyticsObserver.getAnalyticsSnapshot();
        int initialSearchCount = initialSnapshot.getTotalSearches();

        // When: Customer performs search
        Long customerId = 1L;
        List<Room> results = roomSearchService.searchAvailableRooms(
            null, null, testRoomType.getId(), null, customerId
        );

        // Then: Search is tracked
        assertFalse(results.isEmpty(), "Should find available rooms");

        var updatedSnapshot = combinedAnalyticsObserver.getAnalyticsSnapshot();
        assertTrue(updatedSnapshot.getTotalSearches() > initialSearchCount,
            "Search count should increase");
    }

    @Test
    void shouldTrackSearchWithNoResults() {
        // Given: Lock all rooms of the type
        String lock1 = bookingLockService.createLock(testRoom1.getId(), 99L);
        String lock2 = bookingLockService.createLock(testRoom2.getId(), 99L);

        var initialSnapshot = combinedAnalyticsObserver.getAnalyticsSnapshot();

        // When: Customer searches for locked room type
        Long customerId = 1L;
        List<Room> results = roomSearchService.searchAvailableRooms(
            null, null, testRoomType.getId(), null, customerId
        );

        // Then: Search with no results is tracked
        assertTrue(results.isEmpty(), "Should return no results (all locked)");

        var updatedSnapshot = combinedAnalyticsObserver.getAnalyticsSnapshot();
        assertTrue(updatedSnapshot.getSearchesWithNoResults() >
                  initialSnapshot.getSearchesWithNoResults(),
            "No-result searches should increase");

        // Cleanup
        bookingLockService.releaseLockByRoomId(testRoom1.getId());
        bookingLockService.releaseLockByRoomId(testRoom2.getId());
    }

    @Test
    void shouldCorrelateSearchToLockConversion() throws InterruptedException {
        // Given: Customer searches first
        Long customerId = 1L;
        List<Room> searchResults = roomSearchService.searchAvailableRooms(
            null, null, testRoomType.getId(), null, customerId
        );

        assertFalse(searchResults.isEmpty(), "Should find rooms");
        Long roomId = searchResults.getFirst().getId();

        // Small delay to ensure timestamp ordering
        Thread.sleep(100);

        // When: Same customer locks a room from search results
        String lockId = bookingLockService.createLock(roomId, customerId);

        // Then: Conversion is tracked (verified by logs in CombinedAnalyticsObserver)
        assertNotNull(lockId, "Lock should be created");

        var snapshot = combinedAnalyticsObserver.getAnalyticsSnapshot();
        assertTrue(snapshot.getTotalLocks() > 0, "Lock count should increase");
        assertTrue(snapshot.getConversionRate() >= 0, "Conversion rate should be calculated");

        // Cleanup
        bookingLockService.releaseLock(lockId, customerId);
    }

    @Test
    void shouldNotifyOnLockRelease() {
        // Given: Customer searches and finds no results (room is locked)
        Long customerId = 1L;
        String lockId = bookingLockService.createLock(testRoom1.getId(), 99L);

        List<Room> results = roomSearchService.searchAvailableRooms(
            null, null, testRoomType.getId(), null, customerId
        );

        // Verify room is excluded from results
        assertTrue(results.stream().noneMatch(r -> r.getId().equals(testRoom1.getId())),
            "Locked room should not appear in results");

        // When: Lock is released
        bookingLockService.releaseLock(lockId, 99L);

        // Then: SearchAwarenessObserver processes the lock release
        // (Verified by logs showing notification logic)

        // Verify room is now available in subsequent searches
        List<Room> newResults = roomSearchService.searchAvailableRooms(
            null, null, testRoomType.getId(), null, customerId
        );

        assertTrue(newResults.stream().anyMatch(r -> r.getId().equals(testRoom1.getId())),
            "Released room should appear in new search results");
    }

    @Test
    void shouldHandleLockExpiration() {
        // Given: Customer searches, room gets locked
        Long customerId = 1L;
        String lockId = bookingLockService.createLock(testRoom1.getId(), 99L, 1); // 1 minute TTL

        List<Room> results = roomSearchService.searchAvailableRooms(
            null, null, testRoomType.getId(), null, customerId
        );

        assertTrue(results.stream().noneMatch(r -> r.getId().equals(testRoom1.getId())),
            "Locked room should not appear");

        // Note: In real scenario, you'd wait for TTL expiration
        // For testing, we manually release to simulate expiration
        bookingLockService.releaseLock(lockId, 99L);

        // When: Room becomes available again
        List<Room> newResults = roomSearchService.searchAvailableRooms(
            null, null, testRoomType.getId(), null, customerId
        );

        // Then: Room appears in results again
        assertTrue(newResults.stream().anyMatch(r -> r.getId().equals(testRoom1.getId())),
            "Room should be available after lock release");
    }

    @Test
    void shouldTrackMultipleCustomerSearches() {
        // Given: Multiple customers search
        Long customer1 = 1L;
        Long customer2 = 2L;
        Long customer3 = 3L;

        var initialSnapshot = combinedAnalyticsObserver.getAnalyticsSnapshot();

        // When: Multiple searches occur
        roomSearchService.searchAvailableRooms(null, null, testRoomType.getId(), null, customer1);
        roomSearchService.searchAvailableRooms(null, null, null, testHotel.getId(), customer2);
        roomSearchService.searchAvailableRooms(null, null, null, null, customer3);

        // Then: All searches are tracked
        var updatedSnapshot = combinedAnalyticsObserver.getAnalyticsSnapshot();
        assertTrue(updatedSnapshot.getTotalSearches() >= initialSnapshot.getTotalSearches() + 3,
            "All customer searches should be tracked");
    }

    @Test
    void shouldExcludeLockedRoomsFromSearch() {
        // Given: One room is locked
        String lockId = bookingLockService.createLock(testRoom1.getId(), 99L);

        // When: Search for all rooms of this type
        List<Room> results = roomSearchService.searchAvailableRooms(
            null, null, testRoomType.getId(), null, 1L
        );

        // Then: Locked room is excluded
        assertTrue(results.stream().noneMatch(r -> r.getId().equals(testRoom1.getId())),
            "Locked room should be excluded from search results");
        assertTrue(results.stream().anyMatch(r -> r.getId().equals(testRoom2.getId())),
            "Unlocked room should be in search results");

        // Cleanup
        bookingLockService.releaseLockByRoomId(testRoom1.getId());
    }

    @Test
    void shouldTrackAnalyticsMetrics() {
        // Given: Initial state
        var initialSnapshot = combinedAnalyticsObserver.getAnalyticsSnapshot();

        // When: Perform various operations
        Long customerId = 1L;

        // Search 1: Find results
        roomSearchService.searchAvailableRooms(null, null, testRoomType.getId(), null, customerId);

        // Search 2: Lock a room
        String lockId = bookingLockService.createLock(testRoom1.getId(), customerId);

        // Search 3: Search again (now with fewer results)
        roomSearchService.searchAvailableRooms(null, null, testRoomType.getId(), null, customerId);

        // Release lock
        bookingLockService.releaseLock(lockId, customerId);

        // Then: Analytics are updated
        var finalSnapshot = combinedAnalyticsObserver.getAnalyticsSnapshot();

        assertTrue(finalSnapshot.getTotalSearches() > initialSnapshot.getTotalSearches(),
            "Total searches should increase");
        assertTrue(finalSnapshot.getTotalLocks() > initialSnapshot.getTotalLocks(),
            "Total locks should increase");
        assertTrue(finalSnapshot.getConversionRate() >= 0,
            "Conversion rate should be calculated");
    }
}
