package dev.efnilite.ip.util;

import dev.efnilite.vilib.util.Time;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;

/**
 * A stopwatch that... counts
 *
 * @author Efnilite
 */
public class Stopwatch {

    private long start;

    public boolean hasStarted() {
        return start != 0;
    }

    public void start() {
        this.start = System.currentTimeMillis();
    }

    public void stop() {
        start = 0;
    }

    /**
     * Parses a time to a duration in ms
     *
     * @param   string
     *          The provided time
     *
     * @return the provided time, measured in ms
     */
    public static long toMillis(@NotNull String string) {
        long total = 0; // total duration in ms

        for (String part : string.trim().split(" ")) {
            if (part.contains("h")) { // measure hours
                int h = Integer.parseInt(part.replace("h", ""));

                total += Time.toMillis((long) h * Time.SECONDS_PER_HOUR);
            } else if (part.contains("m")) {
                int m = Integer.parseInt(part.replace("m", ""));

                total += Time.toMillis((long) m * Time.SECONDS_PER_MINUTE);
            } else if (part.contains("s")) {
                double s = Double.parseDouble(part.replace("s", ""));

                total += s * 1000;
            }
        }

        return total;
    }

    @Override
    public String toString() {
        if (start == 0) {
            return "0.0s";
        }
        long delta = System.currentTimeMillis() - start;
        String format = DurationFormatUtils.formatDuration(delta, "HH:mm:ss:SSS", true);
        String[] split = format.split(":");
        String updated = "";
        int hours = Integer.parseInt(split[0]);
        if (hours > 0) {
            updated += hours + "h ";
        }
        int mins = Integer.parseInt(split[1]);
        if (mins > 0) {
            updated += mins + "m ";
        }
        int secs = Integer.parseInt(split[2]);
        updated += secs;
        int ms = Integer.parseInt(split[3]);
        String parsed = Integer.toString(ms);
        updated += "." + parsed.charAt(0) + "s";
        return updated;
    }
}
