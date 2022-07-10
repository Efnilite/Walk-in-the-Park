import dev.efnilite.ip.IP;
import dev.efnilite.vilib.util.elevator.VersionComparator;
import dev.efnilite.vilib.vector.Vector3D;

public class Testing {

    public static void main(String[] args) {
        System.out.println(VersionComparator.FROM_SEMANTIC.isLatest(IP.REQUIRED_VILIB_VERSION, "1.0.9"));

        String id = "1";
        Vector3D rel = new Vector3D(10, 10, 10);
        System.out.println(id + "(" + (int) rel.x + "," + (int) rel.y + "," + (int) rel.z + ")");
    }

    public static void main0(String[] args) {
        for (int dy = -2; dy <= 1; dy++) {
            for (int range = 1; range <= 4; range++) {
                int gap = range;
                if (dy > 0 && gap < 2) {
                    gap = 2;
                }

                // the adjusted dy, used to get the updated max range
                int ady = dy;

                // change coefficient of line if dy is below 0
                if (dy < 0) {
                    ady = (int) Math.ceil(0.5 * dy); // use ceil since ceiling of negative is closer to 0
                }

                // the max range, adjusted to the difference in height
                // +1 to allow 4 block jumps
                int adjustedRange = gap - ady;

                // delta sideways
                int ds = 0; // make sure df is always 1 by making sure adjustedRange > ds

                // delta forwards
                int df = adjustedRange - Math.abs(ds);

                System.out.println("Range " + range + " with dy " + dy);
                System.out.println("\tSideways: " + ds);
                System.out.println("\tForwards: " + df);
            }
        }
    }

}
