package dev.efnilite.ip.generator.profile;

/**
 * This class is used for easily collecting the real value of a provided value
 *
 * @param   value
 *          The provided String value
 */
public record ProfileValue(String value) {

    public boolean asBoolean() {
        return value.equals("true"); // save parsing
    }

    public double asDouble() {
        return Double.parseDouble(value);
    }

    public int asInt() {
        return Integer.parseInt(value);
    }
}