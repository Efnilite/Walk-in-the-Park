package dev.efnilite.witp.hook;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
import dev.efnilite.witp.WITP;
import org.bukkit.entity.Player;

public class ProtocolHook {

    /**
     * Sets the lighting to another dimension
     * @author Shevchik (https://github.com/Shevchik/FakeDimension)
     *
     * @param   player
     *          The player
     *
     * @param   dimension
     *          The dimension
     */
    public void setLighting(Player player, Dimension dimension) {
        Class<?> dimensionManagerClass = MinecraftReflection.getMinecraftClass("DimensionManager");
        StructureModifier<Object> dimensionManagerStructureModifier = new StructureModifier<>(dimensionManagerClass, Object.class, false, true);
        Class<?> minecraftKeyClass = MinecraftReflection.getMinecraftKeyClass();

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(PacketAdapter.params(WITP.getInstance(), PacketType.Play.Server.KEEP_ALIVE)) {
            @Override
            public void onPacketSending(PacketEvent event) {
                dimensionManagerStructureModifier
                        .withTarget(event.getPacket().getSpecificModifier(dimensionManagerClass)
                        .read(0))
                        .withType(minecraftKeyClass, MinecraftKey.getConverter())
                        .write(1, dimension.key);
            }
        });
    }

    /**
     * The types of dimension
     */
    public enum Dimension {
        OVERWORLD("overworld"),
        NETHER("the_nether"),
        END("the_end");

        MinecraftKey key;

        Dimension(String key) {
            this.key = new MinecraftKey(key);
        }
    }

}
