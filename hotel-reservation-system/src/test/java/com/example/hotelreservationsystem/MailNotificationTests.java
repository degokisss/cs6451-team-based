package com.example.hotelreservationsystem;

import com.example.hotelreservationsystem.base.notification.Notification;
import com.example.hotelreservationsystem.base.pattern.NotificationServiceFactory;
import com.example.hotelreservationsystem.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class MailNotificationTests {
    @Autowired
    NotificationServiceFactory notificationServiceFactory;

    @Autowired
    JavaMailSender javaMailSender;

    @TestConfiguration
    static class MockMailConfig {
        @Bean
        @Primary
        JavaMailSender javaMailSender() {
            return Mockito.mock(JavaMailSender.class);
        }
    }

    @Test
    public void testMailNotification() throws Exception {
        doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

        Notification<SimpleMailMessage> notification = notificationServiceFactory.createNotificationService(NotificationType.EMAIL);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("zanderchim78@test.com");
        message.setSubject("Test mail");
        message.setText("This is a test mail");
        assertTrue(notification.sendNotification(message));

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    public void testSMSNotification() throws Exception {
        Notification<String> notification = notificationServiceFactory.createNotificationService(NotificationType.SMS);
        assertTrue(notification.sendNotification("This is a test SMS"));
    }
}
