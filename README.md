# RideHub — Full Vehicle Rental Platform

This upgraded build keeps the existing Spring Boot vehicle-rental backend and adds a new end-to-end customer/owner experience under `src/main/resources/static`.

## Included

### Customer
- Modern responsive home page
- Location search / browser location permission
- Nearby shop discovery
- Vehicle search
- Vehicle type, price, rating and text filters
- Sorting by price, distance and rating
- Vehicle detail + booking flow
- Date-based booking calculation
- Booking history
- Customer profile
- Wishlist/local saved rides
- Customer support ticket creation + ticket history
- Existing JWT authentication and payment APIs remain available

### Owner
- Owner authentication
- Owner shop list
- Fleet/vehicle overview
- Owner dashboard shell
- Existing shop, vehicle, booking and revenue APIs remain available

### Support
- Persistent `support_tickets` JPA entity
- Create/list support tickets through:
  - `POST /api/user/support/tickets`
  - `GET /api/user/support/tickets`

### UI/UX
- Premium dark green / lime visual system
- Responsive desktop/tablet/mobile layout
- Sticky navigation
- Search hero
- Vehicle cards
- Filter sidebar
- Shop cards + map placeholder
- Booking checkout summary
- Support center
- Profile/settings
- Owner console
- Admin operations shell

## Run

1. Make sure Java 17+ and MySQL are installed.
2. Create/use database `vehicle_rental_dbs`.
3. Update MySQL credentials in `src/main/resources/application.properties` if needed.
4. Start the Spring Boot application from your IDE as `VehicleRentalApplication`.
5. Open:
   `http://localhost:8080/`

The new frontend is served directly by Spring Boot, so no separate frontend server is required.

## Payment

The default configuration remains `app.payment.provider=mock` for local testing. Switch to Razorpay and add the required keys in your environment/config before production payments.

## Important

The browser UI has a safe demo fallback when the database/API is not available, so the design can still be reviewed without a running MySQL instance. Once the backend is running, it automatically uses the real API for supported flows.


## RideHub v3 additions
- Booking overlap protection includes paid/pending-owner-approval bookings.
- Customer booking cancellation and owner approve/reject/complete workflow.
- Verified driving license required before booking; admin license verification endpoints.
- Customer notifications for payment, approval/rejection, cancellation and completion.
- Customer profile and password update APIs.
- Admin reports endpoint and expanded management APIs.
- Customer support ticket conversation with customer/support messages.
- Uploaded files are served from `/uploads/**`, including vehicle images.

### Admin login
The `ADMIN` role is intentionally not exposed through public registration. Seed an admin account directly in the database or add one through a controlled seed/migration before production.
