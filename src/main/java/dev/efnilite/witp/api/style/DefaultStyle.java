package dev.efnilite.witp.api.style;

import org.bukkit.Material;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultStyle extends Style {

    public DefaultStyle(List<Material> possible) {
        super(possible);
    }

    @Override
    public Material get() {
        return possible.get(ThreadLocalRandom.current().nextInt(possible.size() - 1));
    }
}
