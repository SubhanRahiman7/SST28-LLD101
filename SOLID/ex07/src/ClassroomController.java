/**
 * Uses only the capability interfaces it needs for each device. No fat interface.
 */
public class ClassroomController {
    private final DeviceRegistry reg;

    public ClassroomController(DeviceRegistry reg) {
        this.reg = reg;
    }

    public void startClass() {
        Projector pj = reg.getProjector();
        pj.powerOn();
        pj.connectInput("HDMI-1");

        LightsPanel lights = reg.getLightsPanel();
        lights.setBrightness(60);

        AirConditioner ac = reg.getAirConditioner();
        ac.setTemperatureC(24);

        AttendanceScanner scan = reg.getAttendanceScanner();
        System.out.println("Attendance scanned: present=" + scan.scanAttendance());
    }

    public void endClass() {
        System.out.println("Shutdown sequence:");
        reg.getProjector().powerOff();
        reg.getLightsPanel().powerOff();
        reg.getAirConditioner().powerOff();
    }
}
