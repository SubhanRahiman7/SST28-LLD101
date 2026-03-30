# BookMyShow (LLD Design + Implementation)

## Functional Requirements (Core Use Cases)
- User can:
  - View movies/theaters/shows for a city
  - View seat map for a show
  - Select seats and lock them temporarily
  - Book tickets (with payment)
  - Cancel booking and get refund
- Admin can:
  - Add/update movies
  - Add/update theaters
  - Add screens in a theater
  - Add shows for a screen
  - Configure pricing rules (show/time/demand + per-category base price)

## Core System Requirements (Summary)
- Multiple cities, theaters, screens, shows.
- Each show has a fixed seat layout and maintains seat availability.
- Seat locking prevents double booking; seat states stay consistent.
- Booking is atomic: all requested seats are booked together or none are booked.

## Mermaid Flow Chart (LLD Design)
```mermaid
flowchart TD
  subgraph U["User Operations"]
    U1[Select City]
    U2[Select Movie or Theater]
    U3[Select Show]
    U4[View Seat Map (availability)]
    U5[Select Seats]
    U6[Lock Seats (5 min)]
    U7[Proceed to Payment]
    U8{Payment SUCCESS?}
    U9[Confirm Booking]
    U10[Release Seats + Booking FAILED]
    U11[Cancel Booking (before showtime)]
    U12[Refund Trigger]
  end

  subgraph A["Admin Operations"]
    A1[Add/Update Movie]
    A2[Add/Update Theater]
    A3[Add Screen]
    A4[Add Show]
    A5[Configure Pricing Rules]
  end

  subgraph S["System Components"]
    C1[Catalog Service
      (City/Theater/Movie/Screen)]
    SH1[Show Service
      (seat map + availability)]
    SL1[Seat Lock Manager
      (pessimistic locking)]
    P1[Pricing Engine
      (base + show/time/demand)]
    PG1[Payment Gateway
      INITIATED/SUCCESS/FAILED]
    B1[Booking Service
      atomic seat update]
    R1[Refund Service]
  end

  %% User flow
  U1 --> C1
  U2 --> C1
  U3 --> SH1
  SH1 --> U4
  U4 --> U5
  U5 --> SL1
  SL1 --> U6
  U6 --> P1
  P1 --> U7
  U7 --> PG1
  PG1 --> U8
  U8 -->|Yes| U9
  U8 -->|No| U10
  U9 --> B1
  B1 --> SL1
  U10 --> SL1
  U11 --> B1
  B1 --> R1
  R1 --> U12

  %% Admin flow
  A1 --> C1
  A2 --> C1
  A3 --> C1
  A4 --> SH1
  A5 --> P1

  %% Concurrency hint
  SL1 -. "reservation token + expiry" .-> B1
```

## How to Compile & Run (Answer Code)
```bash
cd "answer"
javac com/example/bookmyshow/*.java
java com.example.bookmyshow.App
```

## Notes
- Seat locking uses a 5-minute expiry window by default.
- The in-memory implementation demonstrates the LLD approach (thread-safe, no double booking).
- Payment and refund are mocked but preserve the required payment states.

