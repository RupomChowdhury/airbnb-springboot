# 🏨 AirBNB Clone (Spring Boot)

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Status](https://img.shields.io/badge/Status-In_Development-yellow)

## Overview
A robust backend application simulating the core functionalities of an AirBNB-like platform. Built with modern Java and Spring Boot, this project handles complex logic for hotel management, dynamic inventory pricing, booking orchestration, and payment processing.

---

## Key Features

### Hotel & Room Management
- **Multi-Hotel Support**: Create and manage multiple hotel profiles with rich details (amenities, photos, location).
- **Room Variations**: Define various room types (Deluxe, Suite, Standard) with specific capacities and base prices.
- **Active Status Control**: Toggle hotels or rooms as active/inactive.

### Advanced Inventory & Pricing
- **Dynamic Inventory**: Track daily availability per room type.
- **Surge Pricing**: Implement dynamic pricing strategies using a `surgeFactor` (e.g., higher prices on weekends/holidays).
- **Real-time Availability**: Prevent double bookings with strict unique constraints on `(hotel, room, date)`.

### Booking System (In Progress)
- **Reservation Lifecycle**: Manage bookings from `CONFIRMED` to `CANCELLED` or `COMPLETED`.
- **Guest Management**: Associate multiple guests with a single booking.
- **Stay Duration**: Calculate costs based on check-in/check-out dates.

### Payments & Users (In Progress)
- **Transaction Tracking**: Link payments to specific bookings.
- **Role-Based Access**: Distinguish between Guests, Hosts, and Admins.

---

## Technical Architecture & Data Model

The application is built on a relational database model designed for scalability and data integrity.

### Core Entities
| Entity | Description | Key Relationships |
| :--- | :--- | :--- |
| **Hotel** | Represents a property. | `1:N` Rooms, `1:N` Inventory, `Many:1` Owner (User) |
| **Room** | A specific type of accommodation within a hotel. | `Many:1` Hotel, `1:N` Inventory |
| **Inventory** | **(Crucial)** Controls availability and price for a specific date. | `Many:1` Room, `Many:1` Hotel |
| **Booking** | A reservation made by a user. | `Many:1` User, `Many:1` Room, `1:1` Payment |
| **User** | System actors (Guests, Hosts). | `1:N` Bookings, `1:N` Hotels (if Host) |
| **Payment** | Financial transaction records. | `1:1` Booking |

---

## Daily Progress Log

*Keeping track of active development tasks and recent updates.*

**Current Focus:** **Booking Logic & Payment Integration**

- **[Today]:** Refactoring `Booking` entity to ensure proper relationships with `Guest` and `Payment`.
- **[Recent]:** Implemented `Inventory` logic to handle `surgeFactor` for dynamic pricing.
- **[Recent]:** Completed CRUD operations for `Hotel` and `Room` controllers.
- **[Pending]:** Implementing the `bookRoom()` service method with transactional guarantees.

---

## Roadmap

- [x] **Core Setup**: Spring Boot 4.0.2, PostgreSQL, Maven.
- [x] **Data Layer**: Entities designed (Hotel, Room, Inventory, Booking, User).
- [x] **Basic APIs**: CRUD for Hotels and Rooms.
- [x] **Inventory Logic**: Daily availability tracking.
- [ ] **Booking Engine**: Validation, creation, and status management.
- [ ] **Payment Integration**: Mock payment gateway connection.
- [ ] **Security**: JWT Authentication & Role-based Authorization.

---

## 💻 Getting Started

### Prerequisites
*   Java 25
*   Maven
*   PostgreSQL

### Run Locally
1.  **Clone the repo**
    ```bash
    git clone https://github.com/RupomChowdhury/airbnb-clone.git
    ```
2.  **Configure Database**
    Update `src/main/resources/application.properties` with your PostgreSQL credentials.
3.  **Build & Run**
    ```bash
    mvn spring-boot:run
    ```

---