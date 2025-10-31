package com.example.hotelreservationsystem.entity;

public enum  RoomType {
    NONE(0, "None", "error home", 0,0f),

    SINGLE(1, "Single", "sssss", 1, 100.0f),
    DOUBLE(2, "Double", "sssss", 2, 150.0f),
    TWIN(3, "Twin", "sssss", 2, 150.0f),
    TRIPLE(4, "Triple", "sssss", 3, 250.0f),
    FAMILY(5, "Family", "sssss", 5, 500.0f),
    SUITE(6, "Suite", "sssss", 8, 800.0f),

    ;

    public final int roomTypeId;
    public final String name;
    public final String description;
    public final int capacity;
    public final float basePrice;

    RoomType(int roomTypeId, String name, String description, int capacity, float basePrice) {
        this.roomTypeId = roomTypeId;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.basePrice = basePrice;
    }
}
