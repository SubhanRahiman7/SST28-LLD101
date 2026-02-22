/** SMS channel: uses phone and body (subject not used by SMS, but we don't change meaning). */
public class SmsSender extends NotificationSender {
    public SmsSender(AuditLog audit) {
        super(audit);
    }

    @Override
    public SendResult send(Notification n) {
        System.out.println("SMS -> to=" + n.phone + " body=" + n.body);
        audit.add("sms sent");
        return SendResult.ok();
    }
}
