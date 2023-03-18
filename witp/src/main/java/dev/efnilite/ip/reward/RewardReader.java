package dev.efnilite.ip.reward;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.Colls;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that reads the rewards-v2.yml file and puts them in the variables listed below.
 *
 * @author Efnilite
 */
public class RewardReader {

    private static FileConfiguration rewards;
    
    public static boolean REWARDS_ENABLED;

    /**
     * A map with all Score-type score rewards.
     * The key is the score, and the value are the commands that will be executed once this score is reached.
     */
    public static Map<Integer, List<RewardString>> SCORE_REWARDS = new HashMap<>();

    /**
     * A map with all Interval-type score rewards.
     * The key is the score, and the value are the commands that will be executed once this score is reached.
     */
    public static Map<Integer, List<RewardString>> INTERVAL_REWARDS = new HashMap<>();

    /**
     * A map with all One time-type score rewards.
     * The key is the score, and the value are the commands that will be executed once this score is reached.
     */
    public static Map<Integer, List<RewardString>> ONE_TIME_REWARDS = new HashMap<>();

    /**
     * Reads the rewards from the rewards-v2.yml file
     */
    public static void readRewards(FileConfiguration config) {
        rewards = config;

        // init options
        REWARDS_ENABLED = rewards.getBoolean("enabled");

        if (!REWARDS_ENABLED) {
            return;
        }

        SCORE_REWARDS = parseScores("score-rewards");
        INTERVAL_REWARDS = parseScores("interval-rewards");
        ONE_TIME_REWARDS = parseScores("one-time-rewards");
    }

    private static Map<Integer, List<RewardString>> parseScores(String path) {

        Map<Integer, List<RewardString>> rewardMap = new HashMap<>();

        for (String score : getNodes(path)) {

            // read commands for this score
            List<String> commands = rewards.getStringList(path + "." + score);

            try {
                int value = Integer.parseInt(score);

                if (value <= 0) {
                    IP.logging().stack(score + " is not a valid score (should be above 1)", "check the rewards file for incorrect numbers");
                    continue;
                }

                rewardMap.put(value, Colls.map(RewardString::new, commands));
            } catch (NumberFormatException ex) {
                IP.logging().stack(score + " is not a valid score", "check the rewards file for incorrect numbers", ex);
            }
        }

        return rewardMap;
    }

    private static @NotNull List<String> getNodes(@NotNull String path) {
        ConfigurationSection section = rewards.getConfigurationSection(path);
        if (section == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(section.getKeys(false));
    }
}
