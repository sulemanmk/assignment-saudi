# Product Manager API

A comprehensive Spring Boot application for managing products, users, and orders with dynamic discount computation based on user type and order amount.

## Features

### Product Management

- Full CRUD operations for products
- Soft delete with restore capability
- Search/filter by name, price range, and availability
- Pagination and sorting support
- Caching with Caffeine

### User Management

- Three user roles: `USER`, `PREMIUM_USER`, `ADMIN`
- JWT-based authentication
- Role-based access control (RBAC)
- Secure password hashing with BCrypt

### Order Management

- Place orders for multiple products
- Stock validation and automatic inventory decrease
- Order cancellation with stock restoration

### Dynamic Discount System (Strategy Pattern)

| User Type    | Base Discount | Order > $500 |
| ------------ | ------------- | ------------ |
| USER         | 0%            | +5%          |
| PREMIUM_USER | 10%           | +5%          |
| ADMIN        | 0%            | +5%          |

##  Technology Stack

- **Java 17+**
- **Spring Boot 3.2.x**
- **Spring Security** with JWT
- **Spring Data JPA**
- **H2 Database** (development) / **PostgreSQL** (production)
- **Flyway** for database migrations
- **OpenAPI/Swagger** for API documentation
- **JUnit 5 & Mockito** for testing

## Prerequisites

- Java 17 or higher
- Maven 3.8+

##  Quick Start

### Local Development (H2 Database)

```bash
# Clone the repository
git clone https://github.com/sulemanmk/assignment-saudi.git
cd assignment-saudi

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start at `http://localhost:8080`

## API Endpoints

### Base URL

- Local: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- H2 Console: `http://localhost:8080/h2-console` (dev only)

### Authentication

| Method | Endpoint             | Description             |
| ------ | -------------------- | ----------------------- |
| POST   | `/api/auth/register` | Register new user       |
| POST   | `/api/auth/login`    | Login and get JWT token |

### Products

| Method | Endpoint                     | Access        | Description                  |
| ------ | ---------------------------- | ------------- | ---------------------------- |
| GET    | `/api/products`              | Authenticated | Get all products (paginated) |
| GET    | `/api/products/{id}`         | Authenticated | Get product by ID            |
| GET    | `/api/products/search`       | Authenticated | Search products              |
| GET    | `/api/products/available`    | Authenticated | Get available products       |
| POST   | `/api/products`              | ADMIN         | Create product               |
| PUT    | `/api/products/{id}`         | ADMIN         | Update product               |
| DELETE | `/api/products/{id}`         | ADMIN         | Soft delete product          |
| POST   | `/api/products/{id}/restore` | ADMIN         | Restore product              |

### Orders

| Method | Endpoint                       | Access        | Description               |
| ------ | ------------------------------ | ------------- | ------------------------- |
| POST   | `/api/orders`                  | Authenticated | Create order              |
| GET    | `/api/orders/{id}`             | Owner/ADMIN   | Get order by ID           |
| GET    | `/api/orders/my-orders`        | Authenticated | Get current user's orders |
| GET    | `/api/orders`                  | ADMIN         | Get all orders            |
| GET    | `/api/orders/user/{userId}`    | ADMIN         | Get orders by user        |
| GET    | `/api/orders/status/{status}`  | ADMIN         | Get orders by status      |
| PATCH  | `/api/orders/{id}/status`      | ADMIN         | Update order status       |
| POST   | `/api/orders/{id}/cancel`      | Owner/ADMIN   | Cancel order              |
| POST   | `/api/orders/preview-discount` | Authenticated | Preview discount          |

### Users

| Method | Endpoint               | Access        | Description      |
| ------ | ---------------------- | ------------- | ---------------- |
| GET    | `/api/users/me`        | Authenticated | Get current user |
| GET    | `/api/users`           | ADMIN         | Get all users    |
| GET    | `/api/users/{id}`      | ADMIN         | Get user by ID   |
| PATCH  | `/api/users/{id}/role` | ADMIN         | Update user role |
| DELETE | `/api/users/{id}`      | ADMIN         | Disable user     |

## Default Users

| Username | Password   | Role         |
| -------- | ---------- | ------------ |
| suleman_admin    | suleman@2026!   | ADMIN        |
| suleman     | Password@2026!    | USER         |
| suleman_premium  | suleman@premium! | PREMIUM_USER |


### Project Structure

```
src/main/java/com/productmanager/
├── config/           # Configuration classes
├── controller/       # REST controllers
├── discount/         # Discount strategy pattern
├── dto/              # Data Transfer Objects
├── entity/           # JPA entities
├── exception/        # Exception handling
├── mapper/           # Entity-DTO mappers
├── repository/       # JPA repositories
├── security/         # JWT & Security
└── service/          # Business logic
```

### Design Patterns Used

1. **Strategy Pattern** - Dynamic discount calculation

   - `DiscountStrategy` interface
   - `PremiumUserDiscountStrategy` - 10% for premium users
   - `OrderAmountDiscountStrategy` - 5% for orders > $500
   - `DiscountCalculator` - Orchestrates strategies

2. **Repository Pattern** - Data access abstraction

3. **DTO Pattern** - Separate API contracts from entities

4. **Builder Pattern** - Object construction (via Lombok)

##  Testing

```bash
# Run all tests
mvn test
```


##  Monitoring

### Actuator Endpoints

- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`

## Postman Collection

Import the Postman collection from `postman/Saudi_Assignmnet_Product Manager API.postman_collection.json` for ready-to-use API requests.

##  Configuration

### Environment Variables

| Variable                 | Default      | Description             |
| ------------------------ | ------------ | ----------------------- |
| `SERVER_PORT`            | 8080         | Application port        |
| `JWT_SECRET`             | (configured) | JWT signing key         |
| `JWT_EXPIRATION`         | 86400000     | Token expiration (ms)   |

### Database Configuration

- **Development**: H2 in-memory database


