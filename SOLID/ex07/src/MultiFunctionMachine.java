public class MultiFunctionMachine implements Printable, Scannable, Faxable {

    @Override
    public void print(Document doc) {
        System.out.println("[MFD] Printing: " + doc.title);
    }

    @Override
    public Document scan() {
        System.out.println("[MFD] Scanning document...");
        return new Document("ScannedDoc", "scanned content");
    }

    @Override
    public void fax(Document doc, String phoneNumber) {
        System.out.println("[MFD] Faxing '" + doc.title + "' to " + phoneNumber);
    }
}

