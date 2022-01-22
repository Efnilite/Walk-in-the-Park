package dev.efnilite.witp.util;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.config.Option;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * Adds useful data for later (e.g. game testing)
 */
public class Logging {

    private static Logger logger;

    public static void init(Plugin plugin) {
        logger = plugin.getLogger();
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

    public static void stack(String error, String fix, @Nullable Throwable throwable) {
        error("##");
        error("## Walk in the Park has encountered a critical error!");
        error("## " + error);

        if (throwable != null) {
            error("##");
            error("## Stack trace:");
            error("## " + throwable);
            StackTraceElement[] stack = throwable.getStackTrace();
            for (StackTraceElement stackTraceElement : stack) {
                error("## \t" + stackTraceElement.toString());
            }
        }

        error("##");
        error("## This is an internal error which you may be able to fix.");
        error("## How to fix: " + fix);
        error("## Unable to solve this problem using the fix? Visit the Discord or GitHub.");
        error("##");
        error("## Version information:");
        error("##\tBuild Version: " + WITP.getInstance().getDescription().getVersion() + " " + (WITP.OUTDATED ? "(outdated)" : "(latest)"));
        error("##\tMinecraft: " + Util.getVersion().replaceAll("_", "."));
        error("##");
    }

    public static void verbose(String msg) {
        if (Option.VERBOSE.get()) {
            logger.info("(Verbose) " + msg);
        }
    }
}
