/** Scanner: only attendance. No power/print. */
public class AttendanceScanner implements AttendanceScanning {
    @Override
    public int scanAttendance() {
        return 3;
    }
}
