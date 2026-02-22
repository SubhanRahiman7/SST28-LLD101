/**
 * Demo for Ex6 — Notification senders (LSP). No throwing; WA returns failure result.
 * Run: javac *.java && java Demo06
 */
public class Demo06 {
    public static void main(String[] args) {
        System.out.println("=== Notification Demo ===");
        AuditLog audit = new AuditLog();

        Notification n = new Notification("Welcome", "Hello and welcome to SST!", "riya@sst.edu", "9876543210");

        NotificationSender email = new EmailSender(audit);
        NotificationSender sms = new SmsSender(audit);
        NotificationSender wa = new WhatsAppSender(audit);

        email.send(n);
        sms.send(n);

        SendResult waResult = wa.send(n);
        if (!waResult.success) {
            System.out.println("WA ERROR: " + waResult.errorMessage);
            audit.add("WA failed");
        }

        System.out.println("AUDIT entries=" + audit.size());
    }
}
