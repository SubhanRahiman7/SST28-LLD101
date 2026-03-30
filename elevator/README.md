# Elevator System (LLD Design + Implementation)

## Functional Requirements (Elevator LLD)
- Multiple elevator cars per building.
- Outside buttons (up/down) exist on each floor and the same set controls all elevator cars.
- Inside each elevator car: individual buttons for floor selection.
- Optimize elevator selection (FCFS / shortest seek time) and allow adding new selection algorithms later.
- Elevator states: `MOVING_UP`, `MOVING_DOWN`, `IDLE`, `MAINTENANCE`, `EMERGENCY`.
- Weight limit per elevator (default 700 kg, configurable).
  - If weight exceeds limit: play a sound and keep doors open (do not move).
- Emergency alarm functionality and power outage handling (system enters emergency mode).
- Track current floor for each elevator.
- Door open/close button support inside elevator.
- Add floors and add elevator cars after deployment.
- Handle elevators under maintenance (non-operational; not selected for calls).
- Concurrency: safe handling of multiple simultaneous button presses.
- Energy-efficient assignment: only one elevator responds to an outside call request.
- Handle simultaneous `UP` and `DOWN` presses by creating two independent call requests.
- Express elevator support (optional): prioritize express elevators for express requests/floors.
- Disable specific buttons/floors optionally.
- Multilingual announcements/interfaces (lightweight stub).

## Software APIs (What the code demonstrates)
- `pressOutsideButton(floor, direction, priority, expressPreferred)`
- `pressInsideButton(elevatorId, floor)`
- `requestDoorOpen(elevatorId)` / `requestDoorClose(elevatorId)`
- Admin-style ops:
  - `addElevatorCar(elevatorId, weightLimitKg, expressEnabled)`
  - `setSelectionPolicy(SelectionPolicy)`
  - `setMaintenance(elevatorId, boolean)`
  - `triggerEmergency()`
  - `setLoadKg(elevatorId, loadKg)`
  - `disableFloor(floor, direction)`
  - `addFloors(newMaxFloor)`

## Mermaid Flowchart (LLD Design)
```mermaid
flowchart TD
  subgraph User["User / Button Operations"]
    U1[Outside button press on floor + direction]
    U2[Inside button press inside elevator car]
    U3[Door Open/Close buttons]
  end

  subgraph Admin["Admin / System Ops"]
    A1[Add floors / add elevator car]
    A2[Set maintenance / clear maintenance]
    A3[Trigger emergency (power outage)]
    A4[Configure weight limit & selection policy]
    A5[Disable floors or buttons]
  end

  subgraph System["ElevatorSystem (Facade + APIs)"]
    E1[Create CallRequest]
    E2[Concurrent-safe assignment lock]
    E3[SelectionPolicy.chooseElevator()]
    E4[Enqueue stop into chosen ElevatorCar]
    E5[ElevatorCar worker loop]
  end

  subgraph Car["ElevatorCar (State Machine)"]
    C1[States: IDLE / MOVING / MAINTENANCE / EMERGENCY]
    C2[Stop set (upStops / downStops)]
    C3[One-floor step movement + stop checks]
    C4[Door open/close]
    C5[Weight check: if exceeded => play sound + keep door open]
  end

  U1 --> E1 --> E2 --> E3 --> E4 --> E5
  U2 --> E5
  U3 --> C4
  Admin --> E5

  E5 --> C1 --> C3 --> C4
  C3 -->|weight exceeded| C5
```

## How to Compile & Run
```bash
cd "elevator/answer"
javac com/example/elevator/*.java
java com.example.elevator.App
```

## Demo Notes
- `App` presses multiple outside buttons concurrently to show thread-safe call assignment.
- It also triggers weight-limit behavior and an emergency example.

