package com.example.hotelreservationsystem.router;

import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CustomerRouter {

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("customer/create")
    public Customer createUser(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }

    @GetMapping("/customer/mock")
    public List<Customer> addRooms() {
        try {
            List<Customer> customers = getMockUsers(30);
            customerRepository.saveAll(customers);
            return customers;
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }

    @GetMapping("/customer/get")
    public List<Customer> getRooms() {
        try {
            return customerRepository.findAll();
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }

    List<Customer> getMockUsers(int count) {
        List<Customer> customers = new ArrayList<>();
        // generate rooms
        for (int i = 0; i < count; i++) {
            Customer customer = new Customer();
            customer.setEmail("121212@12.com");
            customer.setName("user" + i);
            customers.add(customer);
        }
        return customers;
    }
}
