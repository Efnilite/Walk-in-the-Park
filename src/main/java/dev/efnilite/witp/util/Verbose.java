package dev.efnilite.witp.util;

import dev.efnilite.witp.WITP;

import java.util.logging.Logger;

/**
 * Adds useful data for later (e.g. game testing)
 */
public class Verbose {

    private static Logger logger;

    public static void init() {
        logger = WITP.getInstance().getLogger();
    }

    public static void info(String info) {
        logger.info(info);
    }

    public static void warn(String warn) {
        logger.warning(warn);
    }

    public static void error(String error) {
        logger.severe(error);
    }

    public static void verbose(String msg) {
        if (Option.VERBOSE) {
            logger.info("(Verbose) " + msg);
        }
    }
}
