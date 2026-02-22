/** Abstraction for allocating a driver. */
public interface DriverAllocator {
    String allocate(String studentId);
}
