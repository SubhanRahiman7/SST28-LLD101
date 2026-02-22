/**
 * Abstraction for room base price. New room types = new implementation, no switch edit.
 */
public interface RoomPricing {
    Money baseMonthly(int roomType);
}
