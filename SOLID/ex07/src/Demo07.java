public class Demo07 {
    public static void main(String[] args) {
        System.out.println("=== ISP Demo (Ex7) ===");

        Document doc = new Document("Offer Letter", "Welcome to SST!");

        Printable printer = new BasicPrinter();
        printer.print(doc);

        MultiFunctionMachine mfd = new MultiFunctionMachine();
        mfd.print(doc);
        Document scanned = mfd.scan();
        mfd.fax(scanned, "+919876543210");
    }
}

