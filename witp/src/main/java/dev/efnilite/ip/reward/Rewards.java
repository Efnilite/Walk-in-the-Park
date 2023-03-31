package dev.efnilite.ip.reward;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that reads the rewards-v2.yml file and puts them in the variables listed below.
 */
public class Rewards {

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
    public static void init() {
        // init options
        REWARDS_ENABLED = Config.REWARDS.getBoolean("enabled");

        if (!REWARDS_ENABLED) {
            return;
        }

        SCORE_REWARDS = parseScores("score-rewards");
        INTERVAL_REWARDS = parseScores("interval-rewards");
        ONE_TIME_REWARDS = parseScores("one-time-rewards");
    }

    private static Map<Integer, List<RewardString>> parseScores(String path) {
        Map<Integer, List<RewardString>> rewardMap = new HashMap<>();

        for (String score : Config.REWARDS.getChildren(path)) {

            // read commands for this score
            List<String> commands = Config.REWARDS.getStringList("%s.%s".formatted(path, score));

            try {
                int value = Integer.parseInt(score);

                if (value <= 0) {
                    IP.logging().stack("%s is not a valid score (should be above 1 and an integer)".formatted(score), "check the rewards file for incorrect numbers");
                    continue;
                }

                rewardMap.put(value, commands.stream().map(RewardString::new).toList());
            } catch (NumberFormatException ex) {
                IP.logging().stack("%s is not a valid score".formatted(score), "check the rewards file for incorrect numbers", ex);
            }
        }

        return rewardMap;
    }
}
