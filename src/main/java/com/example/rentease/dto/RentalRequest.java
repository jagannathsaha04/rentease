package com.example.rentease.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/rentals.
 * Note: there is no totalAmount here - the Service calculates it.
 */
public class RentalRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "vehicleId is required")
    private Long vehicleId;

    @NotNull(message = "rentalDays is required")
    @Min(value = 1, message = "rentalDays must be at least 1")
    private Integer rentalDays;

    public RentalRequest() {
    }

    public RentalRequest(Long customerId, Long vehicleId, Integer rentalDays) {
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.rentalDays = rentalDays;
    }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public Integer getRentalDays() { return rentalDays; }
    public void setRentalDays(Integer rentalDays) { this.rentalDays = rentalDays; }
}
