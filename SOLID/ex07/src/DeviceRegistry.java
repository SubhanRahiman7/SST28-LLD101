/**
 * Registry that stores devices by type name and returns the right capability interface.
 */
public class DeviceRegistry {
    private Projector projector;
    private LightsPanel lightsPanel;
    private AirConditioner airConditioner;
    private AttendanceScanner attendanceScanner;

    public void add(Projector p) {
        this.projector = p;
    }

    public void add(LightsPanel p) {
        this.lightsPanel = p;
    }

    public void add(AirConditioner a) {
        this.airConditioner = a;
    }

    public void add(AttendanceScanner s) {
        this.attendanceScanner = s;
    }

    public Projector getProjector() {
        if (projector == null) throw new IllegalStateException("Missing: Projector");
        return projector;
    }

    public LightsPanel getLightsPanel() {
        if (lightsPanel == null) throw new IllegalStateException("Missing: LightsPanel");
        return lightsPanel;
    }

    public AirConditioner getAirConditioner() {
        if (airConditioner == null) throw new IllegalStateException("Missing: AirConditioner");
        return airConditioner;
    }

    public AttendanceScanner getAttendanceScanner() {
        if (attendanceScanner == null) throw new IllegalStateException("Missing: AttendanceScanner");
        return attendanceScanner;
    }
}
