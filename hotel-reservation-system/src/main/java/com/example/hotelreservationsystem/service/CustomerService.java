package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    public void saveAll(List<Customer> customers) {
        customerRepository.saveAll(customers);
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }
}
