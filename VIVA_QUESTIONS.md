# RentEase – Viva Questions and Short Answers

**1. What is Spring Boot?**
A framework built on top of Spring that lets us create stand-alone applications quickly, with auto-configuration, starter dependencies and an embedded server, so there is very little manual setup.

**2. What is Spring Boot auto-configuration?**
Spring Boot checks the classpath and properties and automatically creates the beans we need (Tomcat, DataSource, JPA, Jackson ...). Example: H2 + JPA on the classpath → it configures the DataSource and Hibernate for us.

**3. What is `@SpringBootApplication`?**
A combination of `@Configuration`, `@EnableAutoConfiguration` and `@ComponentScan`. It marks the main class, turns on auto-configuration and scans our packages for components.

**4. What is a REST API?**
A way for a client to talk to the server over HTTP using URLs and methods (GET, POST ...), usually exchanging JSON. Example: `GET /api/vehicles/available`.

**5. Why did you use `@RestController`?**
It combines `@Controller` and `@ResponseBody`, so the returned Java objects are automatically converted to JSON and written to the HTTP response.

**6. What is the Service layer?**
The business layer. It holds the rules of the application (availability check, cost calculation, status updates) and calls the repositories.

**7. Why shouldn't business logic be in the Controller?**
The controller should only handle HTTP. Keeping logic in the service makes the code reusable, easier to test (we test `RentalService` without HTTP), and easier to maintain.

**8. What is Spring Data JPA?**
A Spring module that removes boilerplate database code. We write repository interfaces and it generates the implementation using JPA/Hibernate.

**9. What is `JpaRepository`?**
An interface that gives ready-made CRUD methods (`save`, `findAll`, `findById`, `deleteById` ...). `findByAvailableTrue()` is a derived query created from the method name.

**10. What is `@Entity`?**
It marks a Java class as a database table. Each object is a row, each field a column. `@Id` marks the primary key.

**11. What is `@ManyToOne`?**
It defines a many-to-one relationship. Many `Rental` rows can refer to one `Customer` and one `Vehicle`, using a foreign key column.

**12. Why did you use H2?**
It is an in-memory database that needs no installation, starts with the application and is perfect for learning and demos. Data is lost on restart.

**13. What is dependency injection?**
Instead of creating objects with `new`, Spring creates them (beans) and gives them to the classes that need them. This reduces coupling.

**14. What is constructor injection and why use it?**
Dependencies are passed through the constructor (e.g., `RentalService(CustomerRepository, ...)`). Fields can be `final`, objects are never half-built, and it is easy to test. It is the recommended style.

**15. What is `@Transactional`?**
It makes a method run in one database transaction: all changes succeed together or are all rolled back. In `createRental()` we save the vehicle and the rental together, so we never end with an updated vehicle but no rental.

**16. How does a rental request flow through your application?**
`POST /api/rentals` → `RentalController` → `RentalService` (find customer and vehicle, check availability, check days, calculate amount, mark vehicle unavailable) → repositories → H2 → saved `Rental` returned as JSON.

**17. What are your business rules?**
(1) An unavailable vehicle cannot be rented (409). (2) `totalAmount = dailyRate × rentalDays`, calculated by the service. (3) `rentalDays` must be greater than zero (400).

**18. How did you test your business logic?**
With JUnit 5 and `@SpringBootTest` in `RentalServiceTest`: unavailable vehicle is rejected, 1000 × 5 = 5000, zero days is rejected, return makes the vehicle available again.

**19. Why is `totalAmount` not in the request?**
So the client cannot cheat. The service always calculates it from the vehicle's daily rate.

**20. What is `CommandLineRunner` used for here?**
Only to insert demo customers and vehicles at startup. The application does not depend on it.
