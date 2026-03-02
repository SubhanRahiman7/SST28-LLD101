public class UserService {
    private final UserReader reader;
    private final UserWriter writer;

    public UserService(UserReader reader, UserWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    public void register(String id, String name) {
        if (reader.findById(id) != null) {
            System.out.println("ERROR: user already exists: " + id);
            return;
        }
        writer.save(new User(id, name));
        System.out.println("OK: registered " + name + \" (\" + id + \")\");
    }

    public void show(String id) {
        User u = reader.findById(id);
        if (u == null) {
            System.out.println("NOT FOUND: " + id);
        } else {
            System.out.println("USER: " + u.id + \" - \" + u.name);
        }
    }
}

