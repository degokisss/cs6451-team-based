package com.example.hotelreservationsystem.exception;

public class HotelAlreadyExistsException extends RuntimeException {
    public HotelAlreadyExistsException(String message) {
        super(message);
    }

    public HotelAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
