import java.util.HashMap;
import java.util.Map;

public class InMemoryUserStore implements UserReader, UserWriter {
    private final Map<String, User> data = new HashMap<>();

    @Override
    public User findById(String id) {
        return data.get(id);
    }

    @Override
    public void save(User user) {
        data.put(user.id, user);
    }
}

