package com.example.hotelreservationsystem.base.notification;

public interface Notification<T> {
    boolean sendNotification(T message);
}
