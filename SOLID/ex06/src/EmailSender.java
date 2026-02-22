/**
 * Sends full body (no truncation) so behavior is consistent with base contract.
 */
public class EmailSender extends NotificationSender {
    public EmailSender(AuditLog audit) {
        super(audit);
    }

    @Override
    public SendResult send(Notification n) {
        System.out.println("EMAIL -> to=" + n.email + " subject=" + n.subject + " body=" + n.body);
        audit.add("email sent");
        return SendResult.ok();
    }
}
