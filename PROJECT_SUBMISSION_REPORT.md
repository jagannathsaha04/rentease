# RentEase – Project Submission Report

---

## 1. Application Name and Problem Description

### Application Name:
**RentEase – Vehicle Rental Management System**

### Problem Description:
RentEase is a lightweight, full-stack Spring Boot RESTful web application designed to manage vehicle rental operations for small-to-medium rental agencies. Customers and vehicles are registered in the system, and users can browse currently available vehicles. Customers can rent an available vehicle for a specified number of days.

The system automatically enforces business logic:
- Calculates the total rental cost based on the vehicle's daily rate and rental duration.
- Marks the vehicle as unavailable (`available = false`) while rented out.
- Prevents double-booking or renting unavailable vehicles.
- Restores vehicle availability (`available = true`) when returned or when a rental is canceled.
- Exposes structured REST APIs for integration with web clients, mobile apps, or Postman.
- Employs an in-memory H2 database, requiring no external database installation.

---

## 2. Architecture Diagram

The application adheres to a clean **Three-Tier Architecture** (Presentation → Business Logic → Data Access):

```text
┌─────────────────────────────────────────────────────────┐
│                    Client Layer                         │
│           (Postman / Web Browser / Mobile App)           │
└────────────────────────────┬────────────────────────────┘
                             │ HTTP Request (JSON)
                             ▼
┌─────────────────────────────────────────────────────────┐
│                 Presentation Layer                      │
│     CustomerController / VehicleController / RentalController │
└────────────────────────────┬────────────────────────────┘
                             │ Delegates to Service Layer
                             ▼
┌─────────────────────────────────────────────────────────┐
│                    Business Layer                       │
│       CustomerService / VehicleService / RentalService  │
│  (Business rules validation, cost calculation, status)  │
└────────────────────────────┬────────────────────────────┘
                             │ Calls Spring Data JPA
                             ▼
┌─────────────────────────────────────────────────────────┐
│                   Data Access Layer                     │
│  CustomerRepository / VehicleRepository / RentalRepository │
└────────────────────────────┬────────────────────────────┘
                             │ Hibernate ORM / JDBC
                             ▼
┌─────────────────────────────────────────────────────────┐
│                    Database Layer                       │
│           H2 In-Memory Database (renteasedb)             │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Domain Model / Classes

The system consists of three core JPA domain entities (`Customer`, `Vehicle`, `Rental`), one Enumeration (`RentalStatus`), and one Data Transfer Object (`RentalRequest`).

### Entity Relationship Diagram (ERD):
```text
┌──────────────┐             ┌──────────────┐             ┌──────────────┐
│   Customer   │ 1         * │    Rental    │ *         1 │   Vehicle    │
├──────────────┤─────────────┼──────────────┼─────────────┤──────────────┤
│ id (PK)      │             │ id (PK)      │             │ id (PK)      │
│ name         │             │ customer_id  │             │ vehicleNumber│
│ email (UNIQ) │             │ vehicle_id   │             │ model        │
│ phone        │             │ rentalDays   │             │ type         │
└──────────────┘             │ totalAmount  │             │ dailyRate    │
                             │ status (ENUM)│             │ available    │
                             └──────────────┘             └──────────────┘
```

### Domain Classes Summary:

1. **`Customer` Entity**:
   - `id` (Long, PK, Auto-generated)
   - `name` (String, `@NotBlank`)
   - `email` (String, `@NotBlank`, `@Email`, Unique)
   - `phone` (String, `@NotBlank`)

2. **`Vehicle` Entity**:
   - `id` (Long, PK, Auto-generated)
   - `vehicleNumber` (String, `@NotBlank`, Unique)
   - `model` (String, `@NotBlank`)
   - `type` (String, `@NotBlank` - `CAR`, `BIKE`, `SUV`)
   - `dailyRate` (Double, `@NotNull`, `@Positive`)
   - `available` (Boolean, Default `true`)

3. **`Rental` Entity**:
   - `id` (Long, PK, Auto-generated)
   - `customer` (Customer, `@ManyToOne`, optional = false)
   - `vehicle` (Vehicle, `@ManyToOne`, optional = false)
   - `rentalDays` (Integer)
   - `totalAmount` (Double - calculated internally)
   - `status` (RentalStatus Enum - `ACTIVE`, `RETURNED`)

4. **`RentalStatus` Enum**:
   - `ACTIVE`
   - `RETURNED`

5. **`RentalRequest` DTO**:
   - `customerId` (Long, `@NotNull`)
   - `vehicleId` (Long, `@NotNull`)
   - `rentalDays` (Integer, `@NotNull`, `@Min(1)`)

---

## 4. Project / Package Structure

```text
rentease
├── pom.xml
├── README.md
├── API_DOCUMENTATION.md
├── RentEase_Postman_Collection.json
└── src
    ├── main
    │   ├── java
    │   │   └── com.example.rentease
    │   │       ├── RentEaseApplication.java
    │   │       ├── controller
    │   │       │   ├── CustomerController.java
    │   │       │   ├── RentalController.java
    │   │       │   └── VehicleController.java
    │   │       ├── service
    │   │       │   ├── CustomerService.java
    │   │       │   ├── RentalService.java
    │   │       │   └── VehicleService.java
    │   │       ├── repository
    │   │       │   ├── CustomerRepository.java
    │   │       │   ├── RentalRepository.java
    │   │       │   └── VehicleRepository.java
    │   │       ├── model
    │   │       │   ├── Customer.java
    │   │       │   ├── Rental.java
    │   │       │   ├── RentalStatus.java
    │   │       │   └── Vehicle.java
    │   │       ├── dto
    │   │       │   └── RentalRequest.java
    │   │       └── exception
    │   │           ├── BusinessRuleException.java
    │   │           ├── GlobalExceptionHandler.java
    │   │           └── ResourceNotFoundException.java
    │   └── resources
    │       └── application.properties
    └── test
        └── java
            └── com.example.rentease
                └── RentalServiceTest.java
```

---

## 5. Controller Code

### `RentalController.java`
```java
package com.example.rentease.controller;

import java.util.List;

import com.example.rentease.dto.RentalRequest;
import com.example.rentease.model.Rental;
import com.example.rentease.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller: receives the request, calls the service, returns the response. */
@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Rental createRental(@Valid @RequestBody RentalRequest request) {
        return rentalService.createRental(request);
    }

    @GetMapping
    public List<Rental> getAllRentals() {
        return rentalService.getAllRentals();
    }

    @GetMapping("/{id}")
    public Rental getRentalById(@PathVariable Long id) {
        return rentalService.getRentalById(id);
    }

    @PostMapping("/{id}/return")
    public Rental returnVehicle(@PathVariable Long id) {
        return rentalService.returnVehicle(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRental(@PathVariable Long id) {
        rentalService.deleteRental(id);
    }
}
```

### `VehicleController.java`
```java
package com.example.rentease.controller;

import java.util.List;

import com.example.rentease.model.Vehicle;
import com.example.rentease.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vehicle createVehicle(@Valid @RequestBody Vehicle vehicle) {
        return vehicleService.createVehicle(vehicle);
    }

    @GetMapping
    public List<Vehicle> getAllVehicles() {
        return vehicleService.getAllVehicles();
    }

    @GetMapping("/available")
    public List<Vehicle> getAvailableVehicles() {
        return vehicleService.getAvailableVehicles();
    }
}
```

### `CustomerController.java`
```java
package com.example.rentease.controller;

import java.util.List;

import com.example.rentease.model.Customer;
import com.example.rentease.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer createCustomer(@Valid @RequestBody Customer customer) {
        return customerService.createCustomer(customer);
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }
}
```

---

## 6. Service Code

### `RentalService.java`
```java
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

    @Transactional
    public Rental returnVehicle(Long rentalId) {
        Rental rental = getRentalById(rentalId);

        if (rental.getStatus() == RentalStatus.RETURNED) {
            throw new BusinessRuleException(
                    "Rental " + rentalId + " has already been returned",
                    HttpStatus.CONFLICT);
        }

        rental.setStatus(RentalStatus.RETURNED);

        Vehicle vehicle = rental.getVehicle();
        vehicle.setAvailable(true);
        vehicleRepository.save(vehicle);

        return rentalRepository.save(rental);
    }

    public List<Rental> getAllRentals() {
        return rentalRepository.findAll();
    }

    public Rental getRentalById(Long id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found with id " + id));
    }

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
```

---

## 7. Repository Code

### `RentalRepository.java`
```java
package com.example.rentease.repository;

import com.example.rentease.model.Rental;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {
}
```

### `VehicleRepository.java`
```java
package com.example.rentease.repository;

import java.util.List;

import com.example.rentease.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // Derived query: Spring Data builds "select ... where available = true" automatically
    List<Vehicle> findByAvailableTrue();
}
```

### `CustomerRepository.java`
```java
package com.example.rentease.repository;

import com.example.rentease.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
```

---

## 8. At Least One REST Endpoint

### Primary Endpoint: `POST /api/rentals`

- **HTTP Method**: `POST`
- **URL Path**: `/api/rentals`
- **Request Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "customerId": 1,
    "vehicleId": 1,
    "rentalDays": 5
  }
  ```
- **Response Status**: `201 Created`
- **Response Body**:
  ```json
  {
    "id": 1,
    "customer": {
      "id": 1,
      "name": "Rahul Sharma",
      "email": "rahul@example.com",
      "phone": "9876543210"
    },
    "vehicle": {
      "id": 1,
      "vehicleNumber": "TN01AB1234",
      "model": "Honda City",
      "type": "CAR",
      "dailyRate": 1500.0,
      "available": false
    },
    "rentalDays": 5,
    "totalAmount": 7500.0,
    "status": "ACTIVE"
  }
  ```

---

## 9. Business Rules Implemented

| Rule # | Business Rule | Enforced Location | Violation HTTP Status & Result |
|---|---|---|---|
| **1** | **Vehicle Availability Check**: An unavailable vehicle (`available = false`) cannot be rented. | `RentalService.createRental()` | `409 CONFLICT` – "Vehicle TN01AB1234 is not available for rent" |
| **2** | **Automatic Cost Calculation**: `totalAmount = dailyRate × rentalDays` calculated internally by the service layer. Client cannot send `totalAmount`. | `RentalService.createRental()` | N/A (Guarantees data integrity against client tampering) |
| **3** | **Minimum Rental Days**: `rentalDays` must be greater than zero (`>= 1`). | `RentalService.createRental()` & `@Min(1)` on `RentalRequest` | `400 BAD REQUEST` – "rentalDays must be at least 1" |
| **4** | **Return Vehicle Status Update**: Returning a rental updates status to `RETURNED` and restores vehicle `available = true`. | `RentalService.returnVehicle()` | `409 CONFLICT` if rental is already returned |
| **5** | **Deletion Availability Restoration**: Deleting an active rental frees up the vehicle for future rentals. | `RentalService.deleteRental()` | `404 NOT FOUND` if rental ID does not exist |

---

## 10. Test Cases

| Test Case ID | Feature Tested | Input Request / Command | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|
| **TC01** | Create Rental (Valid) | `POST /api/rentals` `{"customerId":1, "vehicleId":1, "rentalDays":5}` | `201 Created`; `totalAmount = 7500.0`; vehicle `available = false` | `201 Created`; `totalAmount = 7500.0`; vehicle `available = false` | **PASS** |
| **TC02** | Duplicate Rental (Conflict) | Repeat TC01 request for Vehicle 1 | `409 Conflict`; vehicle unavailable error message | `409 Conflict`; "Vehicle TN01AB1234 is not available for rent" | **PASS** |
| **TC03** | Invalid Rental Days (Validation) | `POST /api/rentals` `{"customerId":1, "vehicleId":2, "rentalDays":0}` | `400 Bad Request`; validation error message | `400 Bad Request`; "rentalDays must be at least 1" | **PASS** |
| **TC04** | Return Vehicle | `POST /api/rentals/1/return` | `200 OK`; status `RETURNED`; vehicle `available = true` | `200 OK`; status `RETURNED`; vehicle `available = true` | **PASS** |
| **TC05** | Resource Not Found | `GET /api/customers/99` | `404 Not Found`; customer not found error message | `404 Not Found`; "Customer not found with id 99" | **PASS** |
| **TC06** | Delete Active Rental | `DELETE /api/rentals/1` | `204 No Content`; rental deleted; vehicle `available = true` | `204 No Content`; rental deleted; vehicle `available = true` | **PASS** |

---

## 11. At Least One Automated Test

### `RentalServiceTest.java` (JUnit 5 + Spring Boot Test)
```java
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

        assertFalse(vehicleRepository.findById(vehicle.getId()).orElseThrow().getAvailable());

        rentalService.deleteRental(rental.getId());

        assertTrue(vehicleRepository.findById(vehicle.getId()).orElseThrow().getAvailable());
        assertThrows(ResourceNotFoundException.class, () -> rentalService.getRentalById(rental.getId()));
    }
}
```

---

## 12. Screenshot / Console Output of Application Running Successfully

```text
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.3.5)

2026-10-07T13:15:26.437+05:30  INFO 2791 --- [rentease] [           main] c.example.rentease.RentEaseApplication   : Starting RentEaseApplication v1.0.0 using Java 21.0.10 with PID 2791
2026-10-07T13:15:26.679+05:30  INFO 2791 --- [rentease] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-10-07T13:15:26.700+05:30  INFO 2791 --- [rentease] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 18 ms. Found 3 JPA repository interfaces.
2026-10-07T13:15:26.884+05:30  INFO 2791 --- [rentease] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2026-10-07T13:15:26.988+05:30  INFO 2791 --- [rentease] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-10-07T13:15:26.992+05:30  INFO 2791 --- [rentease] [           main] o.s.b.a.h2.H2ConsoleAutoConfiguration    : H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:renteasedb'
2026-10-07T13:15:27.516+05:30  INFO 2791 --- [rentease] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-10-07T13:15:27.778+05:30  INFO 2791 --- [rentease] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2026-10-07T13:15:27.787+05:30  INFO 2791 --- [rentease] [           main] c.example.rentease.RentEaseApplication   : Started RentEaseApplication in 1.476 seconds (process running for 1.702)
```

---

## 13. Screenshot / Output of the REST Endpoint

### Request: `GET /api/vehicles/available`
```bash
curl -s http://localhost:8080/api/vehicles/available
```

### JSON Response Output:
```json
[
  {
    "id": 1,
    "vehicleNumber": "TN01AB1234",
    "model": "Honda City",
    "type": "CAR",
    "dailyRate": 1500.0,
    "available": true
  },
  {
    "id": 2,
    "vehicleNumber": "TN02CD5678",
    "model": "Royal Enfield Classic 350",
    "type": "BIKE",
    "dailyRate": 800.0,
    "available": true
  },
  {
    "id": 3,
    "vehicleNumber": "TN03EF9012",
    "model": "Hyundai Creta",
    "type": "SUV",
    "dailyRate": 2000.0,
    "available": true
  }
]
```

---

## 14. Brief Explanation of How Spring Boot Auto-Configuration is Used

Spring Boot **auto-configuration** automatically detects dependencies present on the application classpath and configures sensible defaults without requiring manual XML files or explicit Java `@Bean` configuration classes.

In RentEase, auto-configuration provides:

1. **Embedded Tomcat Server (`spring-boot-starter-web`)**:
   - Automatically detects Tomcat web server classes on the classpath and starts an embedded web container on port `8080`.
   - Automatically configures `DispatcherServlet` and Jackson `ObjectMapper` for JSON serialization/deserialization.

2. **Spring Data JPA & Hibernate (`spring-boot-starter-data-jpa`)**:
   - Automatically configures `EntityManagerFactory`, `TransactionManager`, and Hibernate ORM.
   - Automatically scans interfaces extending `JpaRepository` and generates proxy implementation beans at runtime.

3. **In-Memory H2 Database (`h2`)**:
   - Automatically detects the H2 driver on the classpath and configures an in-memory `DataSource` (`jdbc:h2:mem:renteasedb`) with credentials `sa`.

4. **H2 Web Console (`spring.h2.console.enabled=true`)**:
   - Automatically provisions the web-based SQL management GUI at `/h2-console`.

5. **Bean Validation (`spring-boot-starter-validation`)**:
   - Automatically configures Hibernate Validator for enforcing JSR-380 annotations (`@NotBlank`, `@Email`, `@Min`, `@Positive`) on `@Valid` request bodies.

6. **`@SpringBootApplication` Annotation**:
   - Combines `@Configuration`, `@EnableAutoConfiguration` (enables classpath scanning for auto-config rules), and `@ComponentScan` (scans package `com.example.rentease` for `@RestController`, `@Service`, and `@Repository` components).
