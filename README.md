# Patel Hardware

A full-stack Java web application for browsing and purchasing hardware-store products. Customers can register and sign in, search inventory, view item details, add products to a cart, update quantities, and complete a checkout action.

**Repository:** https://github.com/Boipelo-007/PatelHardware

## Features

- User registration with email validation and duplicate-account checks
- Secure sign-in with bcrypt password hashing
- Session-based authentication and logout
- Inventory browsing with category and sorting controls
- Search by item name or item number
- Cart management with quantity updates and item removal
- MySQL persistence for users, inventory, and cart records
- JSON responses for the frontend AJAX flows
- Custom 404 and 500 error pages

## Tech Stack

- Java 11
- Java Servlets
- JAX-RS and Jersey
- JDBC with MySQL Connector/J
- MySQL
- BCrypt
- Jackson
- HTML, CSS, and vanilla JavaScript
- Maven
- Apache Tomcat or another Servlet 4.0-compatible container

## Project Structure

```text
src/main/java/com/patelhardware/
├── model/       # User, item, and API response models
├── servlet/     # Authentication, inventory, search, and cart handlers
└── util/        # Database connection utilities
src/main/webapp/
├── *.html       # Customer-facing pages
├── error/       # Custom error pages
└── WEB-INF/     # Servlet configuration
```

## Database

The application expects MySQL tables named `users`, `items`, and `cart`.

### Minimum `users` columns

| Column        | Purpose               |
| ------------- | --------------------- |
| `user_id`   | Primary key           |
| `username`  | Unique username       |
| `email`     | Unique email address  |
| `password`  | bcrypt password hash  |
| `full_name` | Customer name         |
| `phone`     | Customer phone number |
| `address`   | Customer address      |

### Minimum `items` columns

| Column             | Purpose                    |
| ------------------ | -------------------------- |
| `item_id`        | Primary key                |
| `item_number`    | Unique catalogue number    |
| `name`           | Item name                  |
| `color`          | Item colour                |
| `description`    | Item description           |
| `price`          | Item price                 |
| `available`      | Availability flag          |
| `stock_quantity` | Available stock            |
| `category`       | Search and filter category |

### Minimum `cart` columns

| Column       | Purpose                      |
| ------------ | ---------------------------- |
| `cart_id`  | Primary key                  |
| `user_id`  | References `users.user_id` |
| `item_id`  | References `items.item_id` |
| `quantity` | Requested quantity           |

Before running the application, ensure that each table and required relationship matches the SQL used by the servlet classes.

## Local Setup

### Prerequisites

- JDK 11 or newer
- Maven
- MySQL
- Apache Tomcat 9 or another compatible Servlet 4.0 container

### 1. Create the database

Create a database named `patel_hardware`, then add the `users`, `items`, and `cart` tables and seed the inventory.

### 2. Configure the database connection

Update `src/main/java/com/patelhardware/util/DatabaseUtil.java` with a valid JDBC URL and your local MySQL credentials:

```java
private static final String DB_URL =
        "jdbc:mysql://localhost:3306/patel_hardware?useSSL=false&serverTimezone=UTC";
private static final String DB_USERNAME = "root";
private static final String DB_PASSWORD = "your-password";
```

Database values are also declared in `src/main/webapp/WEB-INF/web.xml`, but the current servlets obtain connections through `DatabaseUtil`, so keep both configurations consistent.

### 3. Build the WAR

```powershell
mvn clean package
```

The generated file will be:

```text
target/patel-hardware.war
```

### 4. Deploy

Copy `target/patel-hardware.war` into the Tomcat `webapps` directory, start Tomcat, and open:

```text
http://localhost:8080/patel-hardware/
```

Alternatively, import the project into an IDE such as IntelliJ IDEA or Eclipse and deploy it to a configured Tomcat server.

## Application Flow

1. A visitor registers or signs in.
2. The servlet validates the credentials and stores the `User` in the HTTP session.
3. The user can browse all inventory or filter/search for a specific item.
4. Available stock can be added to the user's cart.
5. The cart view loads persisted cart entries and allows quantity updates or removal.
6. The checkout flow updates stock and clears the user's cart after a successful checkout.

## Main Routes

| Route              | Purpose                 |
| ------------------ | ----------------------- |
| `/index.html`    | Landing page            |
| `/login.html`    | Sign-in page            |
| `/register.html` | Registration page       |
| `/main.html`     | Authenticated dashboard |
| `/viewall.html`  | Inventory catalogue     |
| `/search.html`   | Search results          |
| `/buyitem.html`  | Purchase flow           |
| `/viewcart.html` | Shopping cart           |

Servlet endpoints include `/login`, `/register`, `/main`, `/viewall`, `/buyitem`, `/viewcart`, and `/search`.

## Status

This is a completed learning project demonstrating traditional Java web application development, MVC-style separation, JDBC, session authentication, and server-side JSON endpoints.
