package com.example.reports;

/**
 * TODO (student):
 * Extract expensive loading logic from ReportFile into this RealSubject.
 */
public class RealReport implements Report {

    private final String reportId;
    private final String title;
    private final String classification;
    private String cachedContent;

    public RealReport(String reportId, String title, String classification) {
        this.reportId = reportId;
        this.title = title;
        this.classification = classification;
    }

    @Override
    public void display(User user) {
        String content = loadFromDiskOnce();
        System.out.println("REPORT -> id=" + reportId
                + " title=" + title
                + " classification=" + classification
                + " openedBy=" + user.getName());
        System.out.println("CONTENT: " + content);
    }

    public String getClassification() {
        return classification;
    }

    private String loadFromDiskOnce() {
        if (cachedContent == null) {
            System.out.println("[disk] loading report " + reportId + " ...");
            try { Thread.sleep(120); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            cachedContent = "Internal report body for " + title;
        }
        return cachedContent;
    }
}
