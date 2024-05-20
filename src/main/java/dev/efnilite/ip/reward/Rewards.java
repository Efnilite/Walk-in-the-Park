package dev.efnilite.ip.reward;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class that reads the rewards-v2.yml file and puts them in the variables listed below.
 */
public class Rewards {

    public static boolean REWARDS_ENABLED;

    /**
     * A map with all Score-type score rewards.
     * The key is the score, and the value are the commands that will be executed once this score is reached.
     */
    public static Map<Integer, Set<Reward>> SCORE_REWARDS = new HashMap<>();

    /**
     * A map with all Interval-type score rewards.
     * The key is the score, and the value are the commands that will be executed once this score is reached.
     */
    public static Map<Integer, Set<Reward>> INTERVAL_REWARDS = new HashMap<>();

    /**
     * A map with all One time-type score rewards.
     * The key is the score, and the value are the commands that will be executed once this score is reached.
     */
    public static Map<Integer, Set<Reward>> ONE_TIME_REWARDS = new HashMap<>();

    /**
     * Reads the rewards from the rewards-v2.yml file
     */
    public static void init() {
        // init options
        REWARDS_ENABLED = Config.REWARDS.getBoolean("enabled");

        if (!REWARDS_ENABLED) {
            return;
        }

        SCORE_REWARDS = parseScores("score");
        INTERVAL_REWARDS = parseScores("interval");
        ONE_TIME_REWARDS = parseScores("one-time");
    }

    private static Map<Integer, Set<Reward>> parseScores(String path) {
        Map<Integer, Set<Reward>> rewardMap = new HashMap<>();

        for (String score : Config.REWARDS.getChildren(path)) {

            // read commands for this score
            Set<Reward> rewardStrings = Config.REWARDS.getStringList("%s.%s".formatted(path, score)).stream()
                    .map(Reward::new)
                    .collect(Collectors.toSet());

            try {
                int value = Integer.parseInt(score);

                if (value < 1) {
                    IP.logging().error("Invalid score %s in rewards".formatted(score));
                    continue;
                }

                rewardMap.put(value, rewardStrings);
            } catch (NumberFormatException ex) {
                IP.logging().error("Invalid score %s in rewards".formatted(score));
            }
        }

        return rewardMap;
    }
}
