package com.example.hotelreservationsystem.base.notification;

public interface INotification<T> {
    boolean sendNotification(T message);
}
