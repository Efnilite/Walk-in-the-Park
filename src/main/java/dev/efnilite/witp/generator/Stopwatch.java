package dev.efnilite.witp.generator;

import org.apache.commons.lang.time.DurationFormatUtils;

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

    @Override
    public String toString() {
        if (start == 0) {
            return "0ms";
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
        if (secs > 0) {
            updated += secs + "s ";
        }
        int ms = Integer.parseInt(split[3]);
        StringBuilder string = new StringBuilder(String.valueOf(ms));
        if (string.length() == 0) {
            string.insert(0, "000");
        } else if (string.length() < 3) {
            for (int i = 0; i < (3 - string.length()); i++) {
                string.insert(0, "0");
            }
        }
        updated += string.toString() + "ms";
        return updated;
    }
}
