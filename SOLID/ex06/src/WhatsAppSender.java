/**
 * WhatsApp requires E.164 phone. Instead of throwing, we return failure result so contract is upheld.
 */
public class WhatsAppSender extends NotificationSender {
    public WhatsAppSender(AuditLog audit) {
        super(audit);
    }

    @Override
    public SendResult send(Notification n) {
        if (n.phone == null || !n.phone.startsWith("+")) {
            return SendResult.fail("phone must start with + and country code");
        }
        System.out.println("WA -> to=" + n.phone + " body=" + n.body);
        audit.add("wa sent");
        return SendResult.ok();
    }
}
