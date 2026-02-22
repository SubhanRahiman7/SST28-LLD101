/**
 * Abstraction for saving invoice text. Keeps CafeteriaSystem from depending on FileStore.
 */
public interface InvoiceStore {
    void save(String invoiceId, String content);
    int countLines(String invoiceId);
}
