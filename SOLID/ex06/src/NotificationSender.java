/**
 * Base contract: send(notification) delivers the message and returns a result.
 * Does not throw. Subtypes must not tighten preconditions (e.g. require specific phone format).
 */
public abstract class NotificationSender {
    protected final AuditLog audit;

    protected NotificationSender(AuditLog audit) {
        this.audit = audit;
    }

    public abstract SendResult send(Notification n);
}
