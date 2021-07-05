package dev.efnilite.witp.util.particle;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Does particle stuff
 *
 * @author Efnilite
 */
public class Particles {

    /**
     * Draws particles
     * <p>
     * Uses default org.bukkit.World methods to spawn particles using {@link ParticleData}
     * </p>
     *
     * @param   at
     *          The location of the particles
     *
     * @param   data
     *          The particle data
     */
    public static <T> void draw(Location at, @NotNull ParticleData<T> data) {
        World world = at.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#draw)");
        }

        world.spawnParticle(data.getType(), at, data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
    }

    public static <T> void draw(Location at, @NotNull ParticleData<T> data, Player player) {
        World world = at.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#draw)");
        }

        player.spawnParticle(data.getType(), at, data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
    }

    /*/**
     * Draws a thunder-like effect between 2 locations
     * <p>
     * First gets the locations between the 2 locations using param distanceBetween, then at random locations generates circles.
     * A random location from that circle is picked to be the location where one of the branches goes to.
     * This process is repeated until it reaches the second location.
     * </p>
     *
     * @param   shoot
     *          The location from where the tower shoots (always shoot variable)
     *
     * @param   entity
     *          The location of the entity
     *
     * @param   data
     *          The particle data
     *
     * @param   maxBranchTimes
     *          The max amount of branch times
     *
     * @param   radius
     *          The max radius of the lightning
     */
    /*public static <T> void thunder(Location shoot, Location entity, ParticleData<T> data, double distanceBetween, int maxBranchTimes, int branchAmount, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double distance = shoot.distance(entity);
        double amount = distance / distanceBetween;
        World world = shoot.getWorld() == null ? entity.getWorld() : shoot.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#thunder)");
        }

        Location max = Util.max(shoot, entity); // 10, 10, 10
        Location min = Util.min(shoot, entity); // 0, 0, 0

        double intervalX = (max.getX() - min.getX()) / amount;
        double intervalY = (max.getY() - min.getY()) / amount;
        double intervalZ = (max.getZ() - min.getZ()) / amount;


        // per
        for (int i = 0; i < branchAmount; i++) {

        }
    }

    private int thunder(Location location, int max, int branchAmount, double radius) {
        double increment = (2 * Math.PI) / 100; // calc degree per amount, 2 x pi x r = circumference
        double y = location.getY();

        for (int i = 0; i < 100; i++) {
            double angle = i * increment;
            double x = location.getX() + (radius * Math.cos(angle));
            double z = location.getZ() + (radius * Math.sin(angle));

        }
        return max--;
    }*/

    /**
     * Draws a particle line between 2 points
     * <p>
     * First it finds the max and min locations, then it calculates the amount of particles based on param distanceBetween
     * After that it gets the 3D distances (x, y and z) between the max and min points (delta X / amount of particles).
     *
     * Then it loops the amount of times particles will spawn and in every loop removes delta X,
     * delta Y and delta Z from the max location.
     * </p><p>
     * Example: max is (10, 10, 10), min is (0, 0, 0), distance between particles = 1 block.
     * Gets distance between max and min (~ 14) and divides that by distance between (14 / 1 = 14 particles).
     * Then the difference between max and min gets calculated (10 - 0 = 10) and that gets divided by amount (10 / 14 = ~ 0.7).
     * This means that on the x, y and z axis every time the loop fires 0.7 blocks get removed. Looping 14 times and removing
     * 0.7 every time, means it goes from (10, 10, 10) to (0, 0, 0) and gets all the locations inbetween those 2 locations, where
     * particles need to be spawned.
     * </p>
     *
     * @param   one
     *          The location from where the tower shoots (always shoot variable)
     *
     * @param   two
     *          The location of the entity
     *
     * @param   data
     *          The particle data
     *
     * @param   distanceBetween
     *          The distance between particles in blocks
     */
    public static <T> void line(Location one, Location two, ParticleData<T> data, double distanceBetween) {
        World world = one.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#draw)");
        }
        double dist = one.distance(two);
        Vector p1 = one.toVector();
        Vector p2 = two.toVector();
        Vector vec = p2.clone().subtract(p1).normalize().multiply(distanceBetween);
        world.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(), data.getSize(),
                data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
        world.spawnParticle(data.getType(), p2.getX(), p2.getY(), p2.getZ(), data.getSize(),
                data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
        double length = 0;
        for (; length < dist; p1.add(vec)) {
            world.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(), data.getSize(),
                    data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
            length += distanceBetween;
        }
    }

    /**
     * Creates a box of particles
     * <p>
     * Calculates the min point and then adds all the dimensions to each other point to get the locations of all the points
     * </p>
     * @param   box
     *          The box which the particles will go around
     *
     * @param   world
     *          The world
     *
     * @param   data
     *          The particle data
     *
     * @param   distanceBetween
     *          The distance between particles
     */
    public static <T> void box(BoundingBox box, @NotNull World world, ParticleData<T> data, Player player, double distanceBetween) {
        Location point1 = box.getMin().toLocation(world);
        Location point2, point3, point4, point5, point6, point7, point8;
        if (box.getWidthX() == 1 && box.getWidthZ() == 1) {
            point2 = point1.clone().add(box.getWidthX(), 0, 0);
            point3 = point2.clone().add(0, 0, box.getWidthZ());
            point4 = point1.clone().add(0, 0, box.getWidthZ());
            point5 = point1.clone().add(0, box.getHeight(), 0);
            point6 = point2.clone().add(0, box.getHeight(), 0);
            point7 = point3.clone().add(0, box.getHeight(), 0);
            point8 = point4.clone().add(0, box.getHeight(), 0);
        } else {
            point2 = point1.clone().add(box.getWidthX() + 1, 0, 0);
            point3 = point2.clone().add(0, 0, box.getWidthZ() + 1);
            point4 = point1.clone().add(0, 0, box.getWidthZ() + 1);
            point5 = point1.clone().add(0, box.getHeight() + 1, 0);
            point6 = point2.clone().add(0, box.getHeight() + 1, 0);
            point7 = point3.clone().add(0, box.getHeight() + 1, 0);
            point8 = point4.clone().add(0, box.getHeight() + 1, 0);
        }

        line(point1, point2, data, player, distanceBetween);
        line(point2, point3, data, player, distanceBetween);
        line(point3, point4, data, player, distanceBetween);
        line(point4, point1, data, player, distanceBetween);

        line(point5, point6, data, player, distanceBetween);
        line(point6, point7, data, player, distanceBetween);
        line(point7, point8, data, player, distanceBetween);
        line(point5, point8, data, player, distanceBetween);

        line(point1, point5, data, player, distanceBetween);
        line(point2, point6, data, player, distanceBetween);
        line(point3, point7, data, player, distanceBetween);
        line(point4, point8, data, player, distanceBetween);
    }

    /**
     * {@link #line(Location, Location, ParticleData, double)} but for players
     */
    public static <T> void line(Location one, Location two, ParticleData<T> data, Player player, double distanceBetween) {
        World world = one.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#draw)");
        }
        double dist = one.distance(two);
        Vector p1 = one.toVector();
        Vector p2 = two.toVector();
        Vector vec = p2.clone().subtract(p1).normalize().multiply(distanceBetween);
        player.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(), data.getSize(),
                data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
        player.spawnParticle(data.getType(), p2.getX(), p2.getY(), p2.getZ(), data.getSize(),
                data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
        double length = 0;
        for (; length < dist; p1.add(vec)) {
            player.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(), data.getSize(),
                    data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
            length += distanceBetween;
        }
    }

//    /**
//     * {@link #box(BoundingBox, World, ParticleData, double)} but for players
//     */
//    public static <T> void box(BoundingBox box, @NotNull World world, ParticleData<T> data, Player player, double distanceBetween) {
//        Location point1 = box.getMin().toLocation(world);
//        Location point2, point3, point4;
//        if (box.getWidthX() == 1 && box.getWidthZ() == 1) {
//            point2 = point1.clone().add(box.getWidthX(), 0, 0);
//            point3 = point2.clone().add(0, 0, box.getWidthZ());
//            point4 = point1.clone().add(0, 0, box.getWidthZ());
//        } else {
//            point2 = point1.clone().add(box.getWidthX() + 1, 0, 0);
//            point3 = point2.clone().add(0, 0, box.getWidthZ() + 1);
//            point4 = point1.clone().add(0, 0, box.getWidthZ() + 1);
//        }
//
//        line(point1, point2, data, player, distanceBetween);
//        line(point2, point3, data, player, distanceBetween);
//        line(point3, point4, data, player, distanceBetween);
//        line(point4, point1, data, player, distanceBetween);
//    }

    /**
     * Creates a circle
     *
     * @param   location
     *          The center
     *
     * @param   data
     *          The particle data
     *
     * @param   radius
     *          The radius of the circle
     *
     * @param   amount
     *          The amount of particles
     */
    public static <T> void circle(Location location, ParticleData<T> data, int radius, int amount) {
        World world = location.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#circle)");
        }

        double increment = (2 * Math.PI) / amount; // calc degree per amount, 2 x pi x r = circumference
        double y = location.getY();

        for (int i = 0; i < amount; i++) {
            double angle = i * increment;
            double x = location.getX() + (radius * Math.cos(angle));
            double z = location.getZ() + (radius * Math.sin(angle));
            world.spawnParticle(data.getType(), x, y, z, data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
        }
    }

    public static <T> void circle(Location location, ParticleData<T> data, @NotNull Player player, int radius, int amount) {
        double increment = (2 * Math.PI) / amount; // calc degree per amount, 2 x pi x r = circumference
        double y = location.getY();

        for (int i = 0; i < amount; i++) {
            double angle = i * increment;
            double x = location.getX() + (radius * Math.cos(angle));
            double z = location.getZ() + (radius * Math.sin(angle));
            player.spawnParticle(data.getType(), x, y, z, data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
        }
    }
}