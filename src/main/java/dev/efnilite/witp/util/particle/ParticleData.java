package dev.efnilite.witp.util.particle;

import org.bukkit.Particle;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for data used in {@link Particles}
 *
 * @author Efnilite
 */
public class ParticleData<D> {

    private int size;
    private double speed;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private @Nullable D data;
    private Particle type;

    public ParticleData(Particle type, @Nullable D data, int size, double speed, double offsetX, double offsetY, double offsetZ) {
        this.data = data;
        this.type = type;
        this.size = size;
        this.speed = speed;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public ParticleData(Particle type, @Nullable D data, int size, double offsetX, double offsetY, double offsetZ) {
        this(type, data, size, 0, offsetX, offsetY, offsetZ);
    }

    public ParticleData(Particle type, @Nullable D data, int size, double speed) {
        this(type, data, size, speed, 0, 0, 0);
    }

    /**
     * Constructor
     *
     * @param   type
     *          The type of particle
     *
     * @param   data
     *          The particle data
     *
     * @param   size
     *          The size of the particle
     */
    public ParticleData(Particle type, @Nullable D data, int size) {
        this(type, data, size, 0, 0, 0, 0);
    }

    public ParticleData setSize(int size) {
        this.size = size;
        return this;
    }

    public ParticleData setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public ParticleData setOffsetX(double offsetX) {
        this.offsetX = offsetX;
        return this;
    }

    public ParticleData setOffsetY(double offsetY) {
        this.offsetY = offsetY;
        return this;
    }

    public ParticleData setOffsetZ(double offsetZ) {
        this.offsetZ = offsetZ;
        return this;
    }

    public ParticleData setData(@Nullable D data) {
        this.data = data;
        return this;
    }

    public ParticleData setType(Particle type) {
        this.type = type;
        return this;
    }

    public int getSize() {
        return size;
    }

    public double getSpeed() {
        return speed;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    @Nullable
    public D getData() {
        return data;
    }

    public Particle getType() {
        return type;
    }
}