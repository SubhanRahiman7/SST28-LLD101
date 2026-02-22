/**
 * Result of an export. Either success with bytes, or failure with an error message.
 * Base contract: exporters do not throw; they return a result.
 */
public class ExportResult {
    public final String contentType;
    public final byte[] bytes;
    public final boolean success;
    public final String errorMessage;

    public static ExportResult ok(String contentType, byte[] bytes) {
        return new ExportResult(contentType, bytes, true, null);
    }

    public static ExportResult error(String message) {
        return new ExportResult(null, new byte[0], false, message);
    }

    private ExportResult(String contentType, byte[] bytes, boolean success, String errorMessage) {
        this.contentType = contentType;
        this.bytes = bytes;
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
