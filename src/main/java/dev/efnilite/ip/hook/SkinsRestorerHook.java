package dev.efnilite.ip.hook;

import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

public class SkinsRestorerHook {
    private static SkinsRestorer skinsRestorer;
    private static boolean isInit;

    public static void initialize() {
        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("SkinsRestorer") != null) {
                skinsRestorer = SkinsRestorerProvider.get();
                isInit = skinsRestorer != null;
                if (isInit) {
                    Bukkit.getLogger().info("Registered SkinsRestorer hook");
                } else {
                    Bukkit.getLogger().warning("Failed to initialize SkinsRestorer hook");
                }
            } else {
                isInit = false;
                Bukkit.getLogger().warning("SkinsRestorer plugin is not enabled or not present");
            }
        } catch (NoClassDefFoundError e) {
            isInit = false;
            Bukkit.getLogger().warning("SkinsRestorer classes not found: " + e.getMessage());
        }
    }

    public static PlayerProfile getPlayerHead(UUID uuid, String name) {
        if (isInit) {
            try {
                PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
                Optional<SkinProperty> skinProperty = playerStorage.getSkinForPlayer(uuid, name);
                if (skinProperty.isPresent()) {
                    SkinProperty property = skinProperty.get();
                    String url = PropertyUtils.getSkinTextureUrl(property);
                    PlayerTextures textures = Bukkit.createPlayerProfile("RandomName").getTextures();
                    textures.setSkin(new URL(url));
                    PlayerProfile profile = Bukkit.createPlayerProfile("Random");
                    profile.setTextures(textures);
                    return profile;
                }
                return null;
            } catch (MalformedURLException | DataRequestException e) {
                throw new RuntimeException(e);
            }
        }
        else return null;
    }

    public static boolean isInitialized() {
        return isInit;
    }
}
