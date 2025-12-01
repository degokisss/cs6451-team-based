package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("customer/create")
    public Customer createUser(@RequestBody Customer customer) {
        return customerService.save(customer);
    }

    @GetMapping("/customer/mock")
    public List<Customer> addRooms() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Customer> customers = List.of(mapper.readValue(ResourceUtils.getFile("classpath:customers.json"), Customer[].class));
            customerService.saveAll(customers);
            return customers;
        } catch (Exception e) {
            log.error(e.getMessage());
            return List.of();
        }
    }

    @GetMapping("/customers/list")
    public List<Customer> getRooms() {
        try {
            return customerService.findAll();
        } catch (Exception e) {
            log.error(e.getMessage());
            return List.of();
        }
    }
}
