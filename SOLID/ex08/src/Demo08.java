public class Demo08 {
    public static void main(String[] args) {
        System.out.println("=== ISP Demo (Ex8) ===");

        InMemoryUserStore store = new InMemoryUserStore();
        UserService svc = new UserService(store, store);

        svc.register("U1001", "Riya");
        svc.register("U1001", "Riya Again");
        svc.show("U1001");
        svc.show("U9999");
    }
}

