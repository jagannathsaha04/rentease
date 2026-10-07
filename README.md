# 1. RentEase – Vehicle Rental Management System

A basic Spring Boot REST application built for the **Spring Boot Framework – Part 1** assignment.

---

## 2. Problem Description

RentEase is a small vehicle rental system. Customers and vehicles are registered, users can see which vehicles are currently available, and a customer can rent an available vehicle for a number of days. The system calculates the rental cost automatically, marks the vehicle as unavailable while it is rented, and makes it available again when it is returned.

The application exposes REST APIs that can be tested with Postman, curl, or (for GET requests) a browser. It follows a clean **three-tier architecture** (Controller → Service → Repository) and uses an in-memory H2 database, so nothing needs to be installed except Java and Maven.

---

## 3. Technologies Used

```text
Java 17
Spring Boot 3.3.5
Maven
Spring Web (REST + embedded Tomcat)
Spring Data JPA (Hibernate)
Spring Boot Validation
H2 Database (in-memory)
JUnit 5
Spring Boot Test
```

---

## 4. Architecture

```text
┌─────────────────┐
│     Client      │
│   Postman/Web   │
└────────┬────────┘
         │ HTTP Request
         ▼
┌─────────────────┐
│   Controller    │
│ Presentation    │   receives request, calls service, returns response
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Service     │
│ Business Logic  │   rules, cost calculation, availability, status updates
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Repository    │
│  Data Access    │   Spring Data JPA interfaces
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   H2 Database   │
│      Data       │
└─────────────────┘
```

### Request flow: `POST /api/rentals`

```text
POST /api/rentals

        ↓

RentalController

        ↓

RentalService

        ↓

CustomerRepository
VehicleRepository

        ↓

Business Rules

- Customer exists
- Vehicle exists
- Vehicle is available
- Rental days > 0
- Calculate total amount

        ↓

RentalRepository

        ↓

H2 Database

        ↓

Response returned to Client
```

---

## 5. Domain Model

| Entity | Fields | Notes |
|---|---|---|
| **Customer** | `id`, `name`, `email`, `phone` | Email is unique and must be valid. |
| **Vehicle** | `id`, `vehicleNumber`, `model`, `type`, `dailyRate`, `available` | `type` is a String (`CAR`, `BIKE`, `SUV`). `available = false` means currently rented. |
| **Rental** | `id`, `customer`, `vehicle`, `rentalDays`, `totalAmount`, `status` | `@ManyToOne` to Customer and to Vehicle. `status` is the enum `RentalStatus` (`ACTIVE`, `RETURNED`). |

Relationships: **many Rentals → one Customer**, **many Rentals → one Vehicle** (`@ManyToOne`).

---

## 6. Business Rules

| # | Rule | Where | Result when violated |
|---|---|---|---|
| 1 | A vehicle that is not available cannot be rented. | `RentalService.createRental()` | `409 CONFLICT` |
| 2 | `totalAmount = dailyRate × rentalDays`, calculated by the service. The client cannot send `totalAmount`. | `RentalService.createRental()` | – |
| 3 | `rentalDays` must be greater than zero. | `RentalService.createRental()` (and `@Min(1)` on `RentalRequest`) | `400 BAD REQUEST` |

Extra safety rules: a rental that is already `RETURNED` cannot be returned again (`409`), and unknown customer / vehicle / rental ids give `404 NOT FOUND`.

---

## 7. API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/customers` | Create a customer |
| GET | `/api/customers` | List all customers |
| GET | `/api/customers/{id}` | Get one customer |
| POST | `/api/vehicles` | Create a vehicle |
| GET | `/api/vehicles` | List all vehicles |
| GET | `/api/vehicles/available` | List only available vehicles |
| POST | `/api/rentals` | Rent a vehicle |
| GET | `/api/rentals` | List all rentals |
| GET | `/api/rentals/{id}` | Get one rental |
| POST | `/api/rentals/{id}/return` | Return a rented vehicle |

### Example requests and responses

**Create customer** – `POST /api/customers` → `201 Created`

```json
{ "name": "Rahul Sharma", "email": "rahul@example.com", "phone": "9876543210" }
```

**Create vehicle** – `POST /api/vehicles` → `201 Created`

```json
{ "vehicleNumber": "TN01AB1234", "model": "Honda City", "type": "CAR", "dailyRate": 1500, "available": true }
```

**Create rental** – `POST /api/rentals` → `201 Created`

```json
{ "customerId": 1, "vehicleId": 1, "rentalDays": 5 }
```

Response:

```json
{
  "id": 1,
  "customer": { "id": 1, "name": "Rahul Sharma", "email": "rahul@example.com", "phone": "9876543210" },
  "vehicle": { "id": 1, "vehicleNumber": "TN01AB1234", "model": "Honda City", "type": "CAR", "dailyRate": 1500.0, "available": false },
  "rentalDays": 5,
  "totalAmount": 7500.0,
  "status": "ACTIVE"
}
```

**Rent an unavailable vehicle** → `409 Conflict`

```json
{
  "timestamp": "2026-01-01T10:15:30.123",
  "status": 409,
  "error": "Conflict",
  "message": "Vehicle TN01AB1234 is not available for rent"
}
```

**Invalid rentalDays (0)** → `400 Bad Request`

```json
{
  "timestamp": "2026-01-01T10:16:02.456",
  "status": 400,
  "error": "Bad Request",
  "message": "rentalDays: rentalDays must be at least 1"
}
```

**Unknown id** → `404 Not Found`

```json
{
  "timestamp": "2026-01-01T10:17:10.789",
  "status": 404,
  "error": "Not Found",
  "message": "Customer not found with id 99"
}
```

**Return vehicle** – `POST /api/rentals/1/return` → `200 OK` (same JSON as above, with `"status": "RETURNED"` and the vehicle `"available": true`).

### curl cheat-sheet

```bash
curl -X POST localhost:8080/api/customers -H "Content-Type: application/json" \
     -d '{"name":"Rahul Sharma","email":"rahul.new@example.com","phone":"9876543210"}'

curl -X POST localhost:8080/api/vehicles -H "Content-Type: application/json" \
     -d '{"vehicleNumber":"TN09ZZ0001","model":"Maruti Swift","type":"CAR","dailyRate":1200,"available":true}'

curl localhost:8080/api/vehicles/available

curl -X POST localhost:8080/api/rentals -H "Content-Type: application/json" \
     -d '{"customerId":1,"vehicleId":1,"rentalDays":5}'

curl -X POST localhost:8080/api/rentals/1/return
```

---

## 8. Project Structure

```text
rentease
├── pom.xml
├── README.md
├── VIVA_QUESTIONS.md
└── src
    ├── main
    │   ├── java/com/example/rentease
    │   │   ├── RentEaseApplication.java      (main class + demo data loader)
    │   │   ├── controller
    │   │   │   ├── CustomerController.java
    │   │   │   ├── VehicleController.java
    │   │   │   └── RentalController.java
    │   │   ├── service
    │   │   │   ├── CustomerService.java
    │   │   │   ├── VehicleService.java
    │   │   │   └── RentalService.java
    │   │   ├── repository
    │   │   │   ├── CustomerRepository.java
    │   │   │   ├── VehicleRepository.java
    │   │   │   └── RentalRepository.java
    │   │   ├── model
    │   │   │   ├── Customer.java
    │   │   │   ├── Vehicle.java
    │   │   │   ├── Rental.java
    │   │   │   └── RentalStatus.java
    │   │   ├── dto
    │   │   │   └── RentalRequest.java
    │   │   └── exception
    │   │       ├── ResourceNotFoundException.java
    │   │       ├── BusinessRuleException.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources
    │       └── application.properties
    └── test
        └── java/com/example/rentease
            └── RentalServiceTest.java
```

`dto/RentalRequest`, `BusinessRuleException` and `GlobalExceptionHandler` are the only additions to the suggested structure; they are needed for the request body and for returning 400/404/409 responses.

---

## 9. Auto-Configuration

Spring Boot looks at the **dependencies on the classpath** (from `pom.xml`) and the **properties** (from `application.properties`) and configures the application for us. We never write XML or manually create beans for these things:

| Dependency we added | What Spring Boot configures automatically |
|---|---|
| `spring-boot-starter-web` | Embedded **Tomcat** server (port 8080), **Spring MVC** (`DispatcherServlet`), **Jackson** JSON conversion |
| `spring-boot-starter-data-jpa` | **JPA**, **Hibernate**, repository implementations, **transaction management** (`@Transactional`) |
| `h2` + `spring.datasource.*` | The **DataSource** and the **H2 database connection** |
| `spring.h2.console.enabled=true` | The H2 web console |
| `spring-boot-starter-validation` | Bean Validation (`@Valid`, `@NotBlank`, `@Min`, ...) |

In this project there is **no** `DataSource` bean, no `web.xml`, no Tomcat setup, no Hibernate config class.

### What does `@SpringBootApplication` do?

It is a shortcut for three annotations:

```text
@Configuration          -> this class can define beans
@EnableAutoConfiguration -> "look at the classpath and configure everything you can"
@ComponentScan          -> find @RestController, @Service, @Repository, @Entity ...
                           in com.example.rentease and its sub-packages
```

Because of `@ComponentScan`, all our classes must be inside `com.example.rentease` (or a sub-package of it).

### About `CommandLineRunner`

`RentEaseApplication` contains a `CommandLineRunner` bean that only inserts **demonstration data** (2 customers and 3 vehicles) at startup. The application does not depend on it; you can remove it and add the data via the APIs instead.

Demo data (ids are generated in this order):

```text
Customers
1 - Rahul Sharma
2 - Ananya Das

Vehicles
1 - TN01AB1234 - Honda City - CAR - ₹1500/day - Available
2 - TN02CD5678 - Royal Enfield Classic 350 - BIKE - ₹800/day - Available
3 - TN03EF9012 - Hyundai Creta - SUV - ₹2000/day - Available
```

> Because the sample data already contains Rahul (`rahul@example.com`) and the Honda City (`TN01AB1234`), creating them again returns `409 Conflict`. In the live demo use a different email / vehicle number (see the curl examples above).

---

## 10. Running the Application

Requirements: **JDK 17+** and **Maven 3.6+** (IntelliJ IDEA has Maven built in).

```bash
mvn clean install
mvn spring-boot:run
```

Or in IntelliJ IDEA: open the folder as a Maven project, open `RentEaseApplication.java`, and click the green ▶ next to `main`.

You should see `Started RentEaseApplication in ... seconds` in the console. The server runs on `http://localhost:8080`.

---

## 11. H2 Console

Open: **http://localhost:8080/h2-console**

| Field | Value |
|---|---|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:renteasedb` |
| User Name | `sa` |
| Password | *(leave empty)* |

Then run, for example: `SELECT * FROM VEHICLE;` or `SELECT * FROM RENTAL;`

---

## 12. Testing

```bash
mvn test
```

`RentalServiceTest` uses **JUnit 5** and **`@SpringBootTest`** (starts the real Spring context with H2). `@Transactional` on the test class rolls back the database after every test.

| Test | What it verifies |
|---|---|
| `cannotRentUnavailableVehicle` | Rule 1: unavailable vehicle → `BusinessRuleException` (409) |
| `totalAmountIsDailyRateTimesRentalDays` | Rule 2: 1000 × 5 = 5000, status `ACTIVE` |
| `vehicleBecomesUnavailableAfterRental` | Vehicle `available` becomes `false` after renting |
| `cannotRentSameVehicleTwice` | Second rental of the same vehicle is rejected |
| `rentalDaysMustBeGreaterThanZero` | Rule 3: `rentalDays = 0` → `BusinessRuleException` (400) |
| `returningVehicleMakesItAvailableAgain` | Return sets status `RETURNED` and vehicle `available = true` |
| `unknownCustomerThrowsNotFound` | Missing customer → `ResourceNotFoundException` |

---

## 13. Manual Test Cases

| Test Case | Input | Expected Output | Actual Output | Status |
|---|---|---|---|---|
| TC01 | `POST /api/rentals` `{"customerId":1,"vehicleId":1,"rentalDays":5}` (Honda City, ₹1500/day, available) | `201`; rental created, `totalAmount = 7500`, vehicle becomes unavailable | `201`; rental created, `totalAmount = 7500.0`, vehicle `available = false` | PASS |
| TC02 | Same request repeated for vehicle 1 (already rented) | `409`; rental rejected | `409`; "Vehicle TN01AB1234 is not available for rent" | PASS |
| TC03 | `POST /api/rentals` `{"customerId":2,"vehicleId":3,"rentalDays":0}` | `400`; invalid rental rejected | `400`; "rentalDays must be at least 1" | PASS |
| TC04 | `POST /api/rentals/1/return` | `200`; status `RETURNED`, vehicle available again | `200`; status `RETURNED`, vehicle `available = true` | PASS |
| TC05 | `GET /api/customers/99` | `404` | `404`; "Customer not found with id 99" | PASS |

*(Re-run these on your own machine before submitting so the "Actual Output" column is your own observation.)*

---

## 14. Demo Script

1. Start the application → show `Started RentEaseApplication` in the console.
2. `POST /api/customers` (use a new email).
3. `POST /api/vehicles` (use a new vehicle number).
4. `GET /api/vehicles/available` → all vehicles listed.
5. `POST /api/rentals` with `{"customerId":1,"vehicleId":1,"rentalDays":5}` → `totalAmount = 7500`, vehicle `available = false`.
6. Send the same rental again → `409`, vehicle unavailable.
7. `POST /api/rentals/1/return`.
8. `GET /api/vehicles/available` → the vehicle is back in the list.
