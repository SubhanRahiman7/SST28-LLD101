public class BasicPrinter implements Printable {

    @Override
    public void print(Document doc) {
        System.out.println("PRINTING: " + doc.title);
        System.out.println(doc.content);
    }
}

