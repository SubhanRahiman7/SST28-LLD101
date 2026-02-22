/** Operations for event lead: create event and get count. */
public interface EventOperations {
    void createEvent(String name, double budget);
    int getEventsCount();
}
