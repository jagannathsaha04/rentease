package com.example.rentease.service;

import java.util.List;

import com.example.rentease.dto.RentalRequest;
import com.example.rentease.exception.BusinessRuleException;
import com.example.rentease.exception.ResourceNotFoundException;
import com.example.rentease.model.Customer;
import com.example.rentease.model.Rental;
import com.example.rentease.model.RentalStatus;
import com.example.rentease.model.Vehicle;
import com.example.rentease.repository.CustomerRepository;
import com.example.rentease.repository.RentalRepository;
import com.example.rentease.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business layer: all rental rules live here (NOT in the controller). */
@Service
public class RentalService {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final RentalRepository rentalRepository;

    public RentalService(CustomerRepository customerRepository,
                         VehicleRepository vehicleRepository,
                         RentalRepository rentalRepository) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.rentalRepository = rentalRepository;
    }

    /**
     * @Transactional: creating a rental changes TWO tables (vehicle + rental).
     * Both saves must succeed together or both are rolled back, so the data
     * can never end up half-updated.
     */
    @Transactional
    public Rental createRental(RentalRequest request) {
        // 1. Find customer
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id " + request.getCustomerId()));

        // 2. Find vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id " + request.getVehicleId()));

        // 3. Business Rule 1 - vehicle must be available
        if (!Boolean.TRUE.equals(vehicle.getAvailable())) {
            throw new BusinessRuleException(
                    "Vehicle " + vehicle.getVehicleNumber() + " is not available for rent",
                    HttpStatus.CONFLICT);
        }

        // 4. Business Rule 3 - rental days must be greater than zero
        Integer rentalDays = request.getRentalDays();
        if (rentalDays == null || rentalDays <= 0) {
            throw new BusinessRuleException(
                    "Rental days must be greater than zero",
                    HttpStatus.BAD_REQUEST);
        }

        // 5. Business Rule 2 - calculate total amount (client cannot set it)
        double totalAmount = vehicle.getDailyRate() * rentalDays;

        // 6. Create rental
        Rental rental = new Rental();
        rental.setCustomer(customer);
        rental.setVehicle(vehicle);
        rental.setRentalDays(rentalDays);
        rental.setTotalAmount(totalAmount);
        rental.setStatus(RentalStatus.ACTIVE);

        // 7. Mark vehicle as unavailable
        vehicle.setAvailable(false);
        vehicleRepository.save(vehicle);

        // 8. Save rental and return it
        return rentalRepository.save(rental);
    }

    /** Also @Transactional: updates the rental AND the vehicle together. */
    @Transactional
    public Rental returnVehicle(Long rentalId) {
        // 1-2. Find the rental (404 if it does not exist)
        Rental rental = getRentalById(rentalId);

        if (rental.getStatus() == RentalStatus.RETURNED) {
            throw new BusinessRuleException(
                    "Rental " + rentalId + " has already been returned",
                    HttpStatus.CONFLICT);
        }

        // 3. Change status to RETURNED
        rental.setStatus(RentalStatus.RETURNED);

        // 4. Mark vehicle as available again
        Vehicle vehicle = rental.getVehicle();
        vehicle.setAvailable(true);
        vehicleRepository.save(vehicle);

        // 5-6. Save and return the updated rental
        return rentalRepository.save(rental);
    }

    public List<Rental> getAllRentals() {
        return rentalRepository.findAll();
    }

    public Rental getRentalById(Long id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found with id " + id));
    }

    /**
     * Deletes a rental by ID. If the rental is ACTIVE, restores the vehicle's
     * availability status to true before deleting the rental record.
     */
    @Transactional
    public void deleteRental(Long rentalId) {
        Rental rental = getRentalById(rentalId);

        if (rental.getVehicle() != null && rental.getStatus() == RentalStatus.ACTIVE) {
            Vehicle vehicle = rental.getVehicle();
            vehicle.setAvailable(true);
            vehicleRepository.save(vehicle);
        }

        rentalRepository.delete(rental);
    }
}
