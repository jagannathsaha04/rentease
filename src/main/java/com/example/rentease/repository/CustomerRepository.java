package com.example.rentease.repository;

import com.example.rentease.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA generates the implementation: save, findAll, findById, ... */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
