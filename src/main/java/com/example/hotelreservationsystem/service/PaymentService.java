package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.base.payment.PaymentFactory;
import com.example.hotelreservationsystem.base.payment.PaymentStrategy;

import com.example.hotelreservationsystem.base.payment.observer.PaymentAuditObserver;
import com.example.hotelreservationsystem.base.payment.observer.PaymentNotificationObserver;
import com.example.hotelreservationsystem.base.payment.observer.PaymentObserver;
import com.example.hotelreservationsystem.base.payment.observer.PaymentStatusUpdateObserver;

import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;

import com.example.hotelreservationsystem.enums.PaymentType;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PaymentService {

    private final PaymentFactory paymentFactory;

    //After first meeting , I start to rebuild this part for strictly following the OCP
   /*
    private final List<PaymentObserver> observers = new ArrayList<>();

   public PaymentService(
            PaymentFactory paymentFactory,
            PaymentNotificationObserver notificationObserver,
            PaymentAuditObserver auditObserver,
            PaymentStatusUpdateObserver statusUpdateObserver
    ) {
        this.paymentFactory = paymentFactory;

        this.addObserver(notificationObserver);
        this.addObserver(statusUpdateObserver);
        this.addObserver(auditObserver);


        log.info("PaymentService initialized with {} manual observers.", observers.size());
    }
    */

    // Spring injects all observers automatically
        private final List<PaymentObserver> observers;

        public PaymentService(PaymentFactory paymentFactory, List<PaymentObserver> observers) {
            this.paymentFactory = paymentFactory;
            this.observers = observers;
        }


    public void addObserver(PaymentObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }


    public void removeObserver(PaymentObserver observer) {
        observers.remove(observer);
    }



    public PaymentResponse executePayment(Long orderId, PaymentType paymentType) {
        // 1. Prepare Request
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setPaymentType(paymentType);

        // 2. Factory + Decorator: Get the strategy
        PaymentStrategy strategy = paymentFactory.getStrategy(paymentType);

        // 3. Execute (Follow Validation ->retrying-> Real Payment)
        PaymentResponse response = strategy.pay(request);

        // 4. Observer Pattern: Notify all listeners if successful
        if ("SUCCESS".equals(response.getStatus())) {
            notifyObservers(orderId, response);
        }

        return response;
    }

    private void notifyObservers(Long orderId, PaymentResponse response) {
        log.info(">>> Notifying {} observers...", observers.size());

        for (PaymentObserver observer : observers) {
            try {
                observer.onPaymentSuccess(orderId, response);
            } catch (Exception e) {
                log.error("Observer failed: {}", e.getMessage());
    }
}
    }
}