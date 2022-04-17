package dev.efnilite.ip.util;

import dev.efnilite.vilib.util.Logging;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.ip.IP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

public class UpdateChecker {

    public void check() {
        new Task()
                .async()
                .execute(() -> {
                    String latest;
                    try {
                        latest = getLatestVersion();
                    } catch (IOException ex) {
                        Logging.stack("Error while trying to fetch latest version!",
                                "Please report this error to the developer!", ex);
                        return;
                    }
                    if (!IP.getInstance().getDescription().getVersion().equals(latest)) {
                        Logging.info("A new version of WITP is available to download!");
                        Logging.info("Newest version: " + latest);
                        IP.OUTDATED = true;
                    } else {
                        Logging.info("WITP is currently up-to-date!");
                    }
                })
                .run();
    }

    private String getLatestVersion() throws IOException {
        InputStream stream;
        Logging.info("Checking for updates...");
        try {
            stream = new URL("https://raw.githubusercontent.com/Efnilite/Walk-in-the-Park/master/witp/src/main/resources/plugin.yml").openStream();
        } catch (IOException e) {
            Logging.info("Unable to check for updates!");
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