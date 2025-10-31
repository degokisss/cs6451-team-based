package com.example.hotelreservationsystem.router;

import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
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

//    @GetMapping("/customer/mock")
//    public List<Customer> addRooms() {
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            List<Customer> customers = List.of(mapper.readValue(ResourceUtils.getFile("classpath:customers.json"), Customer[].class));
//            customerRepository.saveAll(customers);
//            return customers;
//        } catch (Exception e) {
//            System.console().printf(e.getMessage());
//            return List.of();
//        }
//    }

    @GetMapping("/customers/list")
    public List<Customer> getRooms() {
        try {
            return customerRepository.findAll();
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }
}
