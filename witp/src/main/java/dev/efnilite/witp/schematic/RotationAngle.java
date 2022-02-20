package dev.efnilite.witp.schematic;

public enum RotationAngle {
    ANGLE_0(0, 180),
    ANGLE_90(90, 270),
    ANGLE_180(180, 0),
    ANGLE_270(270, 90);

    private final int angle;
    private final int opposite;

    RotationAngle(int angle, int opposite) {
        this.angle = angle;
        this.opposite = opposite;
    }

    public static RotationAngle getFromInteger(int angle) {
        return valueOf("ANGLE_" + angle);
    }

    public int getAngle() {
        return angle;
    }

    public int getOpposite() {
        return opposite;
    }
}