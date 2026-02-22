/**
 * Base contract: export(request) returns a result and never throws.
 * Subtypes must accept any non-null ExportRequest and return success or error result.
 */
public abstract class Exporter {
    public abstract ExportResult export(ExportRequest req);
}
