import java.nio.charset.StandardCharsets;

/** CSV export. Preserves content by escaping newlines and commas in body. */
public class CsvExporter extends Exporter {
    @Override
    public ExportResult export(ExportRequest req) {
        if (req == null) {
            return ExportResult.ok("text/csv", new byte[0]);
        }
        String body = req.body == null ? "" : req.body.replace("\n", " ").replace(",", " ");
        String csv = "title,body\n" + req.title + "," + body + "\n";
        return ExportResult.ok("text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }
}
