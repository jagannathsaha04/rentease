package com.example.rentease;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.rentease.dto.RentalRequest;
import com.example.rentease.exception.BusinessRuleException;
import com.example.rentease.exception.ResourceNotFoundException;
import com.example.rentease.model.Customer;
import com.example.rentease.model.Rental;
import com.example.rentease.model.RentalStatus;
import com.example.rentease.model.Vehicle;
import com.example.rentease.repository.CustomerRepository;
import com.example.rentease.repository.VehicleRepository;
import com.example.rentease.service.RentalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the business rules in RentalService.
 * @SpringBootTest starts the full Spring context (with the H2 database).
 * @Transactional rolls back every test, so tests do not affect each other.
 */
@SpringBootTest
@Transactional
class RentalServiceTest {

    private final RentalService rentalService;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;

    private Customer customer;

    @Autowired
    RentalServiceTest(RentalService rentalService,
                      CustomerRepository customerRepository,
                      VehicleRepository vehicleRepository) {
        this.rentalService = rentalService;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @BeforeEach
    void setUp() {
        customer = customerRepository.save(new Customer("Test User", "test.user@example.com", "9999999999"));
    }

    private Vehicle saveVehicle(boolean available) {
        return vehicleRepository.save(new Vehicle("TEST0001", "Test Car", "CAR", 1000.0, available));
    }

    @Test
    void cannotRentUnavailableVehicle() {
        Vehicle vehicle = saveVehicle(false);
        RentalRequest request = new RentalRequest(customer.getId(), vehicle.getId(), 3);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> rentalService.createRental(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void totalAmountIsDailyRateTimesRentalDays() {
        Vehicle vehicle = saveVehicle(true); // dailyRate = 1000

        Rental rental = rentalService.createRental(new RentalRequest(customer.getId(), vehicle.getId(), 5));

        assertEquals(5000.0, rental.getTotalAmount(), 0.001);
        assertEquals(RentalStatus.ACTIVE, rental.getStatus());
    }

    @Test
    void vehicleBecomesUnavailableAfterRental() {
        Vehicle vehicle = saveVehicle(true);

        rentalService.createRental(new RentalRequest(customer.getId(), vehicle.getId(), 2));

        Vehicle updated = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertFalse(updated.getAvailable());
    }

    @Test
    void cannotRentSameVehicleTwice() {
        Vehicle vehicle = saveVehicle(true);
        rentalService.createRental(new RentalRequest(customer.getId(), vehicle.getId(), 2));

        assertThrows(BusinessRuleException.class,
                () -> rentalService.createRental(new RentalRequest(customer.getId(), vehicle.getId(), 2)));
    }

    @Test
    void rentalDaysMustBeGreaterThanZero() {
        Vehicle vehicle = saveVehicle(true);
        RentalRequest request = new RentalRequest(customer.getId(), vehicle.getId(), 0);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> rentalService.createRental(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void returningVehicleMakesItAvailableAgain() {
        Vehicle vehicle = saveVehicle(true);
        Rental rental = rentalService.createRental(new RentalRequest(customer.getId(), vehicle.getId(), 4));

        Rental returned = rentalService.returnVehicle(rental.getId());

        assertEquals(RentalStatus.RETURNED, returned.getStatus());
        assertTrue(vehicleRepository.findById(vehicle.getId()).orElseThrow().getAvailable());
    }

    @Test
    void unknownCustomerThrowsNotFound() {
        Vehicle vehicle = saveVehicle(true);

        assertThrows(ResourceNotFoundException.class,
                () -> rentalService.createRental(new RentalRequest(99999L, vehicle.getId(), 1)));
    }

    @Test
    void deletingActiveRentalFreesVehicleAndRemovesRecord() {
        Vehicle vehicle = saveVehicle(true);
        Rental rental = rentalService.createRental(new RentalRequest(customer.getId(), vehicle.getId(), 3));

        // Before deletion, vehicle is unavailable
        assertFalse(vehicleRepository.findById(vehicle.getId()).orElseThrow().getAvailable());

        // Delete rental
        rentalService.deleteRental(rental.getId());

        // Vehicle should be available again and rental should be deleted
        assertTrue(vehicleRepository.findById(vehicle.getId()).orElseThrow().getAvailable());
        assertThrows(ResourceNotFoundException.class, () -> rentalService.getRentalById(rental.getId()));
    }
}
