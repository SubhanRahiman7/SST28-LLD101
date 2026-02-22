/**
 * Demo for Ex7 — Smart Classroom (ISP). Devices implement only their capabilities.
 * Run: javac *.java && java Demo07
 */
public class Demo07 {
    public static void main(String[] args) {
        System.out.println("=== Smart Classroom ===");
        DeviceRegistry reg = new DeviceRegistry();
        reg.add(new Projector());
        reg.add(new LightsPanel());
        reg.add(new AirConditioner());
        reg.add(new AttendanceScanner());

        ClassroomController c = new ClassroomController(reg);
        c.startClass();
        c.endClass();
    }
}
