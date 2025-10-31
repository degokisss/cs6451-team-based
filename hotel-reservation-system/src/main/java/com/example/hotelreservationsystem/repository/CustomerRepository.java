package com.example.hotelreservationsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.hotelreservationsystem.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
