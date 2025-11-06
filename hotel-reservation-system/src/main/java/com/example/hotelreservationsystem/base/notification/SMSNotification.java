package com.example.hotelreservationsystem.base.notification;

public class SMSNotification implements INotification<String>{
    @Override
    public boolean sendNotification(String message) {
        // sms implementation
        System.out.println("send SMS: " + message);
        return true;
    }
}
