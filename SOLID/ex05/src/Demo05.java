/**
 * Demo for Ex5 — Export (LSP). All exporters honor the same contract; no throwing.
 * Run: javac *.java && java Demo05
 */
public class Demo05 {
    public static void main(String[] args) {
        System.out.println("=== Export Demo ===");

        ExportRequest req = new ExportRequest("Weekly Report", SampleData.longBody());
        Exporter pdf = new PdfExporter();
        Exporter csv = new CsvExporter();
        Exporter json = new JsonExporter();

        System.out.println("PDF: " + formatResult(pdf, req));
        System.out.println("CSV: " + formatResult(csv, req));
        System.out.println("JSON: " + formatResult(json, req));
    }

    private static String formatResult(Exporter e, ExportRequest r) {
        ExportResult out = e.export(r);
        if (out.success) {
            return "OK bytes=" + out.bytes.length;
        }
        return "ERROR: " + out.errorMessage;
    }
}
