package com.example.rentease.service;

import java.util.List;

import com.example.rentease.model.Vehicle;
import com.example.rentease.repository.VehicleRepository;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle createVehicle(Vehicle vehicle) {
        vehicle.setId(null); // always create a new row
        if (vehicle.getAvailable() == null) {
            vehicle.setAvailable(true); // a newly registered vehicle is available by default
        }
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByAvailableTrue();
    }
}
