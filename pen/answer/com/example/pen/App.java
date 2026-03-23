package com.example.pen;

public final class App {
    public static void main(String[] args) {
        InkReservoir ink = new InkReservoir(10, 5); // capacity 10, start with 5
        Paper paper = new Paper();
        Pen pen = new Pen(ink, paper);

        pen.start();
        pen.write("Hi");
        pen.close();

        try {
            pen.write(" there"); // should fail (pen is closed)
        } catch (RuntimeException ex) {
            System.out.println("Expected error: " + ex.getMessage());
        }

        pen.refill(10);
        pen.start();
        pen.write(" there");

        System.out.println("Paper contents: " + paper.getContents());
    }
}

