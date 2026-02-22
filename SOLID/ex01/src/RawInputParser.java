import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single job: parse a raw input line like "name=Riya;email=riya@sst.edu;..."
 * into a key-value map. No validation, no printing.
 */
public class RawInputParser {

    public Map<String, String> parse(String raw) {
        Map<String, String> kv = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return kv;
        }
        String[] parts = raw.split(";");
        for (String p : parts) {
            String[] pair = p.split("=", 2);
            if (pair.length == 2) {
                kv.put(pair[0].trim(), pair[1].trim());
            }
        }
        return kv;
    }
}
