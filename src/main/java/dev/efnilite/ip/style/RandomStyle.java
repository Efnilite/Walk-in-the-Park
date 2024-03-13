package dev.efnilite.ip.style;

import org.bukkit.Material;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomStyle implements Style {

    private final String name;
    private final List<Material> materials;

    public RandomStyle(String name, List<Material> materials) {
        this.name = name;
        this.materials = materials;
    }

    @Override
    public Material getNext() {
        return materials.get(ThreadLocalRandom.current().nextInt(materials.size()));
    }

    @Override
    public String getName() {
        return name;
    }

}
