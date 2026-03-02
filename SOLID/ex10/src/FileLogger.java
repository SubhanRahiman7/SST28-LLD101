public class FileLogger implements Logger {
    private final StringBuilder buffer = new StringBuilder();

    @Override
    public void info(String msg) {
        buffer.append("INFO: ").append(msg).append("\n");
    }

    @Override
    public void error(String msg) {
        buffer.append("ERROR: ").append(msg).append("\n");
    }

    public String dump() {
        return buffer.toString();
    }
}

