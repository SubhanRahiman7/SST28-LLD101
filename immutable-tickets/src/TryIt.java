import com.example.tickets.IncidentTicket;
import com.example.tickets.TicketService;

import java.util.List;

/**
 * Starter demo that shows why mutability is risky.
 *
 * After refactor:
 * - direct mutation should not compile (no setters)
 * - external modifications to tags should not affect the ticket
 * - service "updates" should return a NEW ticket instance
 */
public class TryIt {

    public static void main(String[] args) {
        TicketService service = new TicketService();

        IncidentTicket created = service.createTicket(
                "TCK-1001",
                "reporter@example.com",
                "Payment failing on checkout"
        );
        System.out.println("Created: " + created);

        // Service "updates" now return NEW ticket instances
        IncidentTicket assigned = service.assign(created, "agent@example.com");
        IncidentTicket escalated = service.escalateToCritical(assigned);

        System.out.println("\nAfter service 'updates' (new instance): " + escalated);
        System.out.println("Original remains unchanged            : " + created);

        // Demonstrate that tags cannot be mutated from outside
        List<String> tagsView = escalated.getTags();
        try {
            tagsView.add("HACKED_FROM_OUTSIDE");
        } catch (UnsupportedOperationException ex) {
            System.out.println("\nExternal tag mutation blocked: " + ex.getClass().getSimpleName());
        }
        System.out.println("\nAfter external tag attempt: " + escalated);
    }
}
