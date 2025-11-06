package com.example.hotelreservationsystem;

import com.example.hotelreservationsystem.base.notification.INotification;
import com.example.hotelreservationsystem.base.pattern.NotificationServiceFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class MailNotificationTests {
    @Autowired NotificationServiceFactory notificationServiceFactory;

    @Test
    public void testMailNotification() {
        INotification<SimpleMailMessage> notification = notificationServiceFactory.createNotificationService("email");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("zanderchim78@gmail.com");
        message.setSubject("Test mail");
        message.setText("This is a test mail");
        assertTrue(notification.sendNotification(message));
    }

    @Test
    public void testSMSNotification() {
        INotification<String> notification = notificationServiceFactory.createNotificationService("sms");
        assertTrue(notification.sendNotification("This is a test SMS"));
    }
}
