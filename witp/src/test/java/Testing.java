import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.Util;

public class Testing {

    public static void main(String[] args) {
        System.out.println(Util.isLatest(IP.REQUIRED_VILIB_VERSION, "1.0.7"));
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
