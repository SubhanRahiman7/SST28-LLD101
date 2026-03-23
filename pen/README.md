# Pen (LLD Design + Implementation)

## Requirements
- `start()` enables the pen for writing (requires ink).
- `write(String text)` writes on paper while the pen is started and has enough ink.
- `close()` disables the pen from writing.
- `refill(...)` adds ink back to the pen.

## Mermaid Flow Chart (LLD Design)
```mermaid
flowchart TD
  subgraph PenContext["Pen (Context)"]
    P[Pen]
    S[PenState]
    I[InkReservoir]
    R[Paper]
  end

  subgraph States["State Objects"]
    C[ClosedState]
    W[StartedState]
  end

  P --> I
  P --> R
  P --> S
  S --> C
  S --> W

  %% Interactions
  P -- start() --> S
  S -- delegate start() --> C
  S -- delegate start() --> W

  P -- write(text) --> W
  W -- draw ink units --> I
  W -- write text --> R

  P -- close() --> C

  %% Refill can happen in any state
  P -- refill(amount) --> I
```

## Mermaid Class Diagram
```mermaid
classDiagram
  class Pen {
    +start()
    +write(String)
    +close()
    +refill(int)
    +refillFull()
  }

  class PenState {
    <<interface>>
    +start(Pen)
    +write(Pen, String)
    +close(Pen)
  }

  class ClosedState
  class StartedState
  class InkReservoir {
    +isEmpty()
    +draw(int)
    +refill(int)
    +refillFull()
  }
  class Paper {
    +write(String)
    +getContents()
  }

  Pen --> InkReservoir : owns
  Pen --> Paper : writes to
  Pen --> PenState : delegates behavior to
  PenState <|.. ClosedState
  PenState <|.. StartedState
```

## How to Compile & Run
```bash
cd pen/answer
javac com/example/pen/*.java
java com.example.pen.App
```

## Notes
- Each character consumes **1 ink unit**.
- If you try to `write(...)` while closed (or without enough ink), the implementation throws an `IllegalStateException` / `IllegalArgumentException`.

