package dev.efnilite.witp.generator;

import org.bukkit.util.Vector;

public class SubareaPair {

    public int x;
    public int z;

    public SubareaPair(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public Vector getEstimatedCenter(double borderSize) {
        int size = (int) borderSize;
        return new Vector(x * size, 150, z * size);
    }

    public SubareaPair zero() {
        this.x = 0;
        this.z = 0;
        return this;
    }

    public boolean equals(SubareaPair pair) {
        return pair.x == x && pair.z == z;
    }
}
