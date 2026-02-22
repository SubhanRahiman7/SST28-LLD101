import java.nio.charset.StandardCharsets;

/** JSON export. Treats null request as empty result to match base contract. */
public class JsonExporter extends Exporter {
    @Override
    public ExportResult export(ExportRequest req) {
        if (req == null) {
            return ExportResult.ok("application/json", new byte[0]);
        }
        String json = "{\"title\":\"" + escape(req.title) + "\",\"body\":\"" + escape(req.body) + "\"}";
        return ExportResult.ok("application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
