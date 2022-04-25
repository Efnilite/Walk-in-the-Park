package dev.efnilite.ip.util;

import dev.efnilite.ip.IP;
import dev.efnilite.vilib.util.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

public class UpdateChecker {

    public void check() {
        Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    String latest;
                    try {
                        latest = getLatestVersion();
                    } catch (IOException ex) {
                        IP.logging().stack("Error while trying to fetch latest version!", ex);
                        return;
                    }
                    if (!IP.getPlugin().getDescription().getVersion().equals(latest)) {
                        IP.logging().info("A new version of WITP is available to download!");
                        IP.logging().info("Newest version: " + latest);
                        IP.OUTDATED = true;
                    } else {
                        IP.logging().info("WITP is currently up-to-date!");
                    }
                })
                .run();
    }

    private String getLatestVersion() throws IOException {
        InputStream stream;
        IP.logging().info("Checking for updates...");
        try {
            stream = new URL("https://raw.githubusercontent.com/Efnilite/Walk-in-the-Park/master/witp/src/main/resources/plugin.yml").openStream();
        } catch (IOException e) {
            IP.logging().warn("Unable to check for updates!");
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines()
                    .filter(s -> s.contains("version: ") && !s.contains("api"))
                    .collect(Collectors.toList())
                    .get(0)
                    .replace("version: ", "");
        }
    }
}