package com.example.hotelreservationsystem.base.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotification implements INotification<SimpleMailMessage>{

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public boolean sendNotification(SimpleMailMessage message) {
        System.out.println("send email:" + message);
        try {
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return false;
        }
    }
}
