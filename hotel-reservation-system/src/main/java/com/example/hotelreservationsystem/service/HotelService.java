package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.base.notification.NotificationServiceFactory;
import com.example.hotelreservationsystem.dto.CheckInResponse;
import com.example.hotelreservationsystem.dto.CheckoutResponse;
import com.example.hotelreservationsystem.dto.HotelCreateRequest;
import com.example.hotelreservationsystem.dto.HotelCreateResponse;
import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.enums.NotificationType;
import com.example.hotelreservationsystem.enums.OrderStatus;
import com.example.hotelreservationsystem.exception.HotelAlreadyExistsException;
import com.example.hotelreservationsystem.repository.HotelRepository;
import com.example.hotelreservationsystem.repository.OrderRepository;
import com.example.hotelreservationsystem.service.state.ReservationContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class HotelService {
    private final HotelRepository hotelRepository;
    private final OrderRepository orderRepository;
    private final NotificationServiceFactory notificationServiceFactory;

    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }

    @Transactional
    public HotelCreateResponse create(HotelCreateRequest hotelCreateRequest) {
        var isExisted = hotelRepository.existsByName(hotelCreateRequest.getName());

        if (isExisted)
            throw new HotelAlreadyExistsException(hotelCreateRequest.getName());

        var hotel = Hotel.builder()
                         .name(hotelCreateRequest.getName())
                         .address(hotelCreateRequest.getAddress())
                         .build();
        hotelRepository.save(hotel);

        return HotelCreateResponse.builder()
                                  .id(hotel.getId())
                                  .name(hotelCreateRequest.getName())
                                  .address(hotelCreateRequest.getAddress())
                                  .build();
    }

    public Hotel findById(Long id) {
        return hotelRepository.findById(id).orElse(null);
    }

    @Transactional
    public HotelCreateResponse update(Long id, HotelCreateRequest hotelCreateRequest) {
        var existing = hotelRepository.findById(id);
        if (existing.isEmpty()) {
            return null; // controller will translate to 404
        }

        var hotel = existing.get();

        // if updating name to one that exists on another hotel, prevent it
        if (!hotel.getName().equals(hotelCreateRequest.getName()) && hotelRepository.existsByName(hotelCreateRequest.getName())) {
            throw new HotelAlreadyExistsException(hotelCreateRequest.getName());
        }

        hotel.setName(hotelCreateRequest.getName());
        hotel.setAddress(hotelCreateRequest.getAddress());

        hotelRepository.save(hotel);

        return HotelCreateResponse.builder()
                                  .id(hotel.getId())
                                  .name(hotel.getName())
                                  .address(hotel.getAddress())
                                  .build();
    }

    @Transactional
    public boolean delete(Long id) {
        if (!hotelRepository.existsById(id)) {
            return false;
        }
        hotelRepository.deleteById(id);
        return true;
    }

    // check-in logic
    // validate the check-in code
    // update the order status
    // send notification
    public CheckInResponse checkIn(Long orderId, String checkInCode) {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty()) {
            // order not found
            return CheckInResponse.builder()
                                 .orderId(orderId)
                                 .status("ORDER_NOT_FOUND")
                                 .build();
        } else if (!order.get().getCheckInCode().equals(checkInCode)) {
            // invalid check-in code
            return CheckInResponse.builder()
                                 .orderId(orderId)
                                 .status("INVALID_CHECKIN_CODE")
                                 .build();
        } else if (order.get().getCheckInDate().isBefore(java.time.LocalDate.now().plusDays(1)) ||
                   order.get().getCheckOutDate().isAfter(java.time.LocalDate.now().minusDays(1))) {
            // check-in date is not today
            return CheckInResponse.builder()
                                 .orderId(orderId)
                                 .status("INVALID_CHECKIN_DATE")
                                 .build();
        } else {
            // valid
            // update the status
            Order currentOrder = order.get();
            new ReservationContext(currentOrder).confirm();
            orderRepository.save(currentOrder);

            // send notification
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(order.get().getCustomer().getEmail());
                message.setSubject("Check-in confirmation");
                message.setText("Check-in successful for order ID: " + orderId);
                notificationServiceFactory.createNotificationService(NotificationType.EMAIL)
                        .sendNotification(message);
            } catch (Exception e) {
                // log the error but do not fail the check-in process
                log.error(e.getMessage());
            }

            // return success
            return CheckInResponse.builder()
                                 .orderId(orderId)
                                 .status("CHECKIN_SUCCESS")
                                 .build();

        }
    }

    // check-out logic
    // update the order status
    // send notification
    public CheckoutResponse checkOut(Long roomId) {
        // update the order status
        // and send a notification
        Optional<Order> order = orderRepository.findByRoomId(roomId);
        if (order.isEmpty()) {
            return CheckoutResponse.builder()
                    .status("ORDER_NOT_FOUND")
                    .build();
        } else if (order.get().getOrderStatus() != OrderStatus.CONFIRMED) {
            return CheckoutResponse.builder()
                    .status("INVALID_ORDER_STATUS")
                    .build();
        } else if (order.get().getCheckOutDate().isBefore(java.time.LocalDate.now().minusDays(1))) {
            return CheckoutResponse.builder()
                    .status("INVALID_CHECKOUT_DATE")
                    .build();
        } else {
            // update the status
            Order currentOrder = order.get();
            new ReservationContext(currentOrder).complete();
            orderRepository.save(currentOrder);

            // send notification
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(order.get().getCustomer().getEmail());
                message.setSubject("Check-out confirmation");
                message.setText("Check-out successful for order ID: " + order.get().getId());
                notificationServiceFactory.createNotificationService(NotificationType.EMAIL)
                        .sendNotification(message);
            } catch (Exception e) {
                // log the error but do not fail the check-out process
                log.error(e.getMessage());
            }

            return CheckoutResponse.builder()
                    .status("CHECKOUT_SUCCESS")
                    .build();
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
