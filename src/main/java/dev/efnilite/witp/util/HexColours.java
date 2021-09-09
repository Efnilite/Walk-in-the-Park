package dev.efnilite.witp.util;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to convert hex colours
 * Source: https://github.com/sb2bg/CustomDeathMessages/blob/master/CustomDeathMessages/src/me/element/customdeathmessages/other/HexChat.java
 */
public class HexColours {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final char COLOR_CHAR = '\u00A7';

    public static String translate(String message) {
        if (!Version.isHigherOrEqual(Version.V1_16)) {
            return message;
        }
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }
}