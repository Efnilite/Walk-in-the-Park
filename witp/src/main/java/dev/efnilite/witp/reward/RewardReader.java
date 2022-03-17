package dev.efnilite.witp.reward;

import dev.efnilite.fycore.util.Logging;
import dev.efnilite.witp.WITP;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that reads the rewards.yml file and puts them in the variables listed below.
 *
 * @author Efnilite
 */
public class RewardReader {

    private static FileConfiguration rewards;

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
     * Reads the rewards from the rewards.yml file
     */
    public static void readRewards() {
        rewards = WITP.getConfiguration().getFile("rewards");

        // loop over interval rewards
        for (String interval : getNodes("interval-rewards")) {
            // read commands for this interval
            List<String> commands = rewards.getStringList("interval-rewards." + interval);

            List<RewardString> strings = new ArrayList<>();
            // turn each command into a RewardString
            for (String command : commands) {
                strings.add(new RewardString(command));
            }

            try {
                INTERVAL_REWARDS.put(Integer.parseInt(interval), strings);
            } catch (NumberFormatException ex) {
                Logging.stack(interval + " is not a valid score", "Check your rewards.yml file for incorrect numbers");
            }
        }

        for (String interval : getNodes("score-rewards")) {
            List<String> commands = rewards.getStringList("score-rewards." + interval);

            List<RewardString> strings = new ArrayList<>();
            for (String command : commands) {
                strings.add(new RewardString(command));
            }

            try {
                SCORE_REWARDS.put(Integer.parseInt(interval), strings);
            } catch (NumberFormatException ex) {
                Logging.stack(interval + " is not a valid score", "Check your rewards.yml file for incorrect numbers");
            }
        }

        for (String interval : getNodes("one-time-rewards")) {
            List<String> commands = rewards.getStringList("one-time-rewards." + interval);

            List<RewardString> strings = new ArrayList<>();
            for (String command : commands) {
                strings.add(new RewardString(command));
            }

            try {
                ONE_TIME_REWARDS.put(Integer.parseInt(interval), strings);
            } catch (NumberFormatException ex) {
                Logging.stack(interval + " is not a valid score", "Check your rewards.yml file for incorrect numbers");
            }
        }
    }

    private static @NotNull List<String> getNodes(@NotNull String path) {
        ConfigurationSection section = rewards.getConfigurationSection(path);
        if (section == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(section.getKeys(false));
    }
}
