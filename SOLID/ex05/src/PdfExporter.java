import java.nio.charset.StandardCharsets;

/**
 * PDF export with a content length limit. Honors base contract: returns error result instead of throwing.
 */
public class PdfExporter extends Exporter {
    private static final int MAX_BODY_LENGTH = 20;

    @Override
    public ExportResult export(ExportRequest req) {
        if (req == null) {
            return ExportResult.error("request is null");
        }
        if (req.body != null && req.body.length() > MAX_BODY_LENGTH) {
            return ExportResult.error("PDF cannot handle content > 20 chars");
        }
        String body = req.body == null ? "" : req.body;
        String fakePdf = "PDF(" + req.title + "):" + body;
        return ExportResult.ok("application/pdf", fakePdf.getBytes(StandardCharsets.UTF_8));
    }
}
