package dev.efnilite.ip.generator;

public enum Direction {

    NORTH,
    EAST,
    SOUTH,
    WEST;

    public Direction turnRight() {
        switch (this) {
            case NORTH:
                return EAST;
            case EAST:
                return SOUTH;
            case SOUTH:
                return WEST;
            default:
                return NORTH;
        }
    }

    public Direction turnLeft() {
        switch (this) {
            case NORTH:
                return WEST;
            case WEST:
                return SOUTH;
            case SOUTH:
                return EAST;
            default:
                return NORTH;
        }
    }
}
