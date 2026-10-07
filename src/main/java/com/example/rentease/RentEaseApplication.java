package com.example.rentease;

import com.example.rentease.model.Customer;
import com.example.rentease.model.Vehicle;
import com.example.rentease.repository.CustomerRepository;
import com.example.rentease.repository.VehicleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 */
@SpringBootApplication
public class RentEaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentEaseApplication.class, args);
    }

    /**
     * Demo data only: runs once at startup so the application can be demonstrated
     * immediately. The application itself does NOT depend on this data.
     * The H2 database is in-memory, so the data is recreated on every start.
     */
    @Bean
    CommandLineRunner loadSampleData(CustomerRepository customerRepository,
                                     VehicleRepository vehicleRepository) {
        return args -> {
            customerRepository.save(new Customer("Rahul Sharma", "rahul@example.com", "9876543210"));
            customerRepository.save(new Customer("Ananya Das", "ananya@example.com", "9123456780"));

            vehicleRepository.save(new Vehicle("TN01AB1234", "Honda City", "CAR", 1500.0, true));
            vehicleRepository.save(new Vehicle("TN02CD5678", "Royal Enfield Classic 350", "BIKE", 800.0, true));
            vehicleRepository.save(new Vehicle("TN03EF9012", "Hyundai Creta", "SUV", 2000.0, true));
        };
    }
}
