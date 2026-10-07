package com.example.rentease.repository;

import java.util.List;

import com.example.rentease.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // Derived query: Spring Data builds "select ... where available = true" from the method name
    List<Vehicle> findByAvailableTrue();
}
