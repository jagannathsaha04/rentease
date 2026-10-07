# RentEase - REST API Reference Documentation

**RentEase** is a Spring Boot RESTful web service for managing vehicle rentals, customer registrations, and vehicle availability tracking.

---

## 🌐 General API Information

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **Authentication**: None (Open API)
- **Database**: H2 In-Memory Database (`jdbc:h2:mem:renteasedb`)

---

## 📌 Summary of Endpoints

| Category | Method | Endpoint | Description | Success Status |
|---|---|---|---|---|
| **Customers** | `POST` | `/api/customers` | Register a new customer | `201 Created` |
| | `GET` | `/api/customers` | Retrieve all registered customers | `200 OK` |
| | `GET` | `/api/customers/{id}` | Retrieve details of a specific customer | `200 OK` |
| **Vehicles** | `POST` | `/api/vehicles` | Add a new vehicle | `201 Created` |
| | `GET` | `/api/vehicles` | Retrieve all vehicles | `200 OK` |
| | `GET` | `/api/vehicles/available` | Retrieve all available vehicles | `200 OK` |
| **Rentals** | `POST` | `/api/rentals` | Rent an available vehicle | `201 Created` |
| | `GET` | `/api/rentals` | Retrieve all rental transactions | `200 OK` |
| | `GET` | `/api/rentals/{id}` | Retrieve details of a specific rental | `200 OK` |
| | `POST` | `/api/rentals/{id}/return` | Return a rented vehicle | `200 OK` |
| | `DELETE` | `/api/rentals/{id}` | Cancel / delete a rental record | `204 No Content` |

---

## 👥 Customer Management APIs (`/api/customers`)

### 1. Create a Customer
- **Method**: `POST`
- **Path**: `/api/customers`
- **Description**: Registers a new customer in the system.

#### Request Body:
```json
{
  "name": "Rahul Sharma",
  "email": "rahul@example.com",
  "phone": "9876543210"
}
```

#### Field Specifications & Constraints:
| Field | Type | Required | Constraints / Validation |
|---|---|---|---|
| `name` | String | Yes | Cannot be blank (`@NotBlank`) |
| `email` | String | Yes | Must be a valid email format & unique (`@NotBlank`, `@Email`, `unique = true`) |
| `phone` | String | Yes | Cannot be blank (`@NotBlank`) |

#### Responses:
- **`201 Created`** – Customer successfully registered.
  ```json
  {
    "id": 1,
    "name": "Rahul Sharma",
    "email": "rahul@example.com",
    "phone": "9876543210"
  }
  ```
- **`400 Bad Request`** – Validation error (missing or invalid email/name/phone).
  ```json
  {
    "timestamp": "2026-10-07T13:30:00.123",
    "status": 400,
    "error": "Bad Request",
    "message": "email: Email must be valid"
  }
  ```
- **`409 Conflict`** – Duplicate email.
  ```json
  {
    "timestamp": "2026-10-07T13:30:00.123",
    "status": 409,
    "error": "Conflict",
    "message": "Duplicate value: email or vehicle number already exists"
  }
  ```

#### cURL Example:
```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Rahul Sharma","email":"rahul.new@example.com","phone":"9876543210"}'
```

---

### 2. Get All Customers
- **Method**: `GET`
- **Path**: `/api/customers`
- **Description**: Fetches all registered customers.

#### Responses:
- **`200 OK`** – Returns array of customers.
  ```json
  [
    {
      "id": 1,
      "name": "Rahul Sharma",
      "email": "rahul@example.com",
      "phone": "9876543210"
    },
    {
      "id": 2,
      "name": "Ananya Das",
      "email": "ananya@example.com",
      "phone": "9876543211"
    }
  ]
  ```

#### cURL Example:
```bash
curl http://localhost:8080/api/customers
```

---

### 3. Get Customer by ID
- **Method**: `GET`
- **Path**: `/api/customers/{id}`
- **Description**: Retrieves details for a single customer by ID.

#### Path Parameters:
| Parameter | Type | Description |
|---|---|---|
| `id` | Long | Unique identifier of the customer |

#### Responses:
- **`200 OK`** – Customer details.
  ```json
  {
    "id": 1,
    "name": "Rahul Sharma",
    "email": "rahul@example.com",
    "phone": "9876543210"
  }
  ```
- **`404 Not Found`** – Customer with specified ID does not exist.
  ```json
  {
    "timestamp": "2026-10-07T13:30:00.123",
    "status": 404,
    "error": "Not Found",
    "message": "Customer not found with id 99"
  }
  ```

#### cURL Example:
```bash
curl http://localhost:8080/api/customers/1
```

---

## 🚘 Vehicle Management APIs (`/api/vehicles`)

### 1. Create a Vehicle
- **Method**: `POST`
- **Path**: `/api/vehicles`
- **Description**: Adds a new vehicle to the inventory.

#### Request Body:
```json
{
  "vehicleNumber": "TN01AB1234",
  "model": "Honda City",
  "type": "CAR",
  "dailyRate": 1500.0,
  "available": true
}
```

#### Field Specifications & Constraints:
| Field | Type | Required | Default | Constraints / Validation |
|---|---|---|---|---|
| `vehicleNumber` | String | Yes | – | Must be unique & not blank (`@NotBlank`, `unique = true`) |
| `model` | String | Yes | – | Cannot be blank (`@NotBlank`) |
| `type` | String | Yes | – | Category (`CAR`, `BIKE`, `SUV`, etc.) (`@NotBlank`) |
| `dailyRate` | Double | Yes | – | Daily rental cost, must be > 0 (`@NotNull`, `@Positive`) |
| `available` | Boolean | No | `true` | Vehicle rental availability status |

#### Responses:
- **`201 Created`** – Vehicle created successfully.
  ```json
  {
    "id": 1,
    "vehicleNumber": "TN01AB1234",
    "model": "Honda City",
    "type": "CAR",
    "dailyRate": 1500.0,
    "available": true
  }
  ```
- **`400 Bad Request`** – Validation failure (e.g. dailyRate <= 0 or missing fields).
- **`409 Conflict`** – Duplicate `vehicleNumber`.

#### cURL Example:
```bash
curl -X POST http://localhost:8080/api/vehicles \
  -H "Content-Type: application/json" \
  -d '{"vehicleNumber":"KA01XY9999","model":"Tata Nexon","type":"SUV","dailyRate":1800,"available":true}'
```

---

### 2. Get All Vehicles
- **Method**: `GET`
- **Path**: `/api/vehicles`
- **Description**: Returns all registered vehicles regardless of availability status.

#### Responses:
- **`200 OK`** – Returns array of all vehicles.

#### cURL Example:
```bash
curl http://localhost:8080/api/vehicles
```

---

### 3. Get Available Vehicles
- **Method**: `GET`
- **Path**: `/api/vehicles/available`
- **Description**: Returns only vehicles currently available for rent (`available = true`).

#### Responses:
- **`200 OK`** – Returns list of available vehicles.
  ```json
  [
    {
      "id": 1,
      "vehicleNumber": "TN01AB1234",
      "model": "Honda City",
      "type": "CAR",
      "dailyRate": 1500.0,
      "available": true
    }
  ]
  ```

#### cURL Example:
```bash
curl http://localhost:8080/api/vehicles/available
```

---

## 🔑 Rental Management APIs (`/api/rentals`)

### 1. Create a Rental
- **Method**: `POST`
- **Path**: `/api/rentals`
- **Description**: Rents a vehicle to a customer. Automatically calculates `totalAmount = dailyRate * rentalDays`, changes status to `ACTIVE`, and sets the vehicle's `available` property to `false`.

#### Request Body:
```json
{
  "customerId": 1,
  "vehicleId": 1,
  "rentalDays": 5
}
```

#### Field Specifications & Constraints:
| Field | Type | Required | Constraints / Validation |
|---|---|---|---|
| `customerId` | Long | Yes | Must reference an existing customer (`@NotNull`) |
| `vehicleId` | Long | Yes | Must reference an available vehicle (`@NotNull`) |
| `rentalDays` | Integer | Yes | Must be at least 1 (`@NotNull`, `@Min(1)`) |

#### Business Rules Enforced:
1. **Vehicle Availability**: Vehicle must have `available = true`.
2. **Total Amount**: `totalAmount = vehicle.dailyRate * rentalDays` (calculated internally).
3. **Rental Days**: `rentalDays` must be `> 0`.

#### Responses:
- **`201 Created`** – Rental successfully processed.
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
- **`400 Bad Request`** – `rentalDays < 1` or missing customerId / vehicleId.
- **`404 Not Found`** – Customer or Vehicle ID does not exist.
- **`409 Conflict`** – Vehicle is currently rented out (`available = false`).
  ```json
  {
    "timestamp": "2026-10-07T13:30:00.123",
    "status": 409,
    "error": "Conflict",
    "message": "Vehicle TN01AB1234 is not available for rent"
  }
  ```

#### cURL Example:
```bash
curl -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"vehicleId":1,"rentalDays":5}'
```

---

### 2. Get All Rentals
- **Method**: `GET`
- **Path**: `/api/rentals`
- **Description**: Fetches all rental records in the system.

#### Responses:
- **`200 OK`** – Returns array of rentals.

#### cURL Example:
```bash
curl http://localhost:8080/api/rentals
```

---

### 3. Get Rental by ID
- **Method**: `GET`
- **Path**: `/api/rentals/{id}`
- **Description**: Retrieves a specific rental record by ID.

#### Path Parameters:
| Parameter | Type | Description |
|---|---|---|
| `id` | Long | Unique identifier of the rental transaction |

#### Responses:
- **`200 OK`** – Rental details.
- **`404 Not Found`** – Rental ID not found.

#### cURL Example:
```bash
curl http://localhost:8080/api/rentals/1
```

---

### 4. Return Rented Vehicle
- **Method**: `POST`
- **Path**: `/api/rentals/{id}/return`
- **Description**: Marks an `ACTIVE` rental as `RETURNED` and restores the vehicle's `available` status to `true`.

#### Path Parameters:
| Parameter | Type | Description |
|---|---|---|
| `id` | Long | Unique identifier of the rental transaction to return |

#### Responses:
- **`200 OK`** – Vehicle successfully returned.
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
      "available": true
    },
    "rentalDays": 5,
    "totalAmount": 7500.0,
    "status": "RETURNED"
  }
  ```
- **`404 Not Found`** – Rental ID not found.
- **`409 Conflict`** – Rental is already returned.
  ```json
  {
    "timestamp": "2026-10-07T13:30:00.123",
    "status": 409,
    "error": "Conflict",
    "message": "Rental 1 is already returned"
  }
  ```

#### cURL Example:
```bash
curl -X POST http://localhost:8080/api/rentals/1/return
```

---

### 5. Delete / Cancel a Rental
- **Method**: `DELETE`
- **Path**: `/api/rentals/{id}`
- **Description**: Permanently deletes a rental record from the system. If the rental is currently `ACTIVE`, the associated vehicle is automatically set back to `available = true`.

#### Path Parameters:
| Parameter | Type | Description |
|---|---|---|
| `id` | Long | Unique identifier of the rental to delete |

#### Responses:
- **`204 No Content`** – Rental successfully deleted (no response body).
- **`404 Not Found`** – Rental ID not found.

#### cURL Example:
```bash
curl -X DELETE http://localhost:8080/api/rentals/1
```

---

## ⚠️ Global Error Response Structure

All error responses follow a uniform JSON structure produced by `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-10-07T13:30:00.123",
  "status": 404,
  "error": "Not Found",
  "message": "Customer not found with id 99"
}
```

### Common HTTP Status Codes
| HTTP Code | Exception Source | Reason / Action |
|---|---|---|
| **`400 Bad Request`** | `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `BusinessRuleException` | Validation failure (e.g. `rentalDays < 1`) or invalid JSON syntax |
| **`404 Not Found`** | `ResourceNotFoundException` | Specified ID for Customer, Vehicle, or Rental does not exist |
| **`409 Conflict`** | `BusinessRuleException`, `DataIntegrityViolationException` | Vehicle already rented, rental already returned, or duplicate email/vehicle number |
| **`500 Internal Server Error`** | Unhandled server exceptions | Unexpected system error |
